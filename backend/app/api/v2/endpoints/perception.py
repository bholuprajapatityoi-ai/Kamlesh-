import json
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.middleware.rate_limit import ai_rate_limiter
from app.models.daily_life import Reminder
from app.models.golu_v2 import KnowledgeDocument, NotificationInsight
from app.schemas.common import MessageResponse
from app.schemas.golu_v2 import (
    CameraAnalyzeRequest,
    CameraAnalyzeResponse,
    DocumentQueryRequest,
    DocumentQueryResponse,
    DocumentResponse,
    DocumentUploadRequest,
    NotificationIngestRequest,
    NotificationInsightResponse,
    NotificationSummaryResponse,
    ScreenUnderstandRequest,
    ScreenUnderstandResponse,
)
from app.security.sanitizer import ensure_owner, sanitize_text
from app.services.golu_v2_ai import (
    call_openai_text,
    call_openai_vision,
    extract_text_from_file_bytes,
)

router = APIRouter(tags=["Phase 16 — Vision & Perception Intelligence (v2)"])


def _extract_highlights(text: str, limit: int = 5) -> List[str]:
    lines = [ln.strip("•- \t") for ln in (text or "").splitlines() if len(ln.strip()) > 12]
    if not lines:
        sentences = [s.strip() for s in (text or "").split(".") if len(s.strip()) > 15]
        return [s[:180] for s in sentences[:limit]]
    return [ln[:180] for ln in lines[:limit]]


@router.post("/vision/analyze", response_model=ScreenUnderstandResponse)
async def analyze_screen_content(
    payload: ScreenUnderstandRequest,
    auth=Depends(get_current_user_and_session),
):
    user, _ = auth
    ai_rate_limiter.check(f"v2_vision:{user.id}", limit=25, window_seconds=60)
    if not payload.consent_granted:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Explicit user consent is required before analyzing screen content.",
        )

    clean_text = sanitize_text(payload.screen_text, 12000)
    highlights = _extract_highlights(clean_text, 5)
    settings = get_settings()

    if settings.openai_configured:
        prompt = (
            f"Action: {payload.action}\nApp Context: {payload.app_context or 'Unknown'}\n"
            f"Question: {payload.question or 'Explain and summarize the key points clearly.'}\n\n"
            f"Visible Screen Text:\n{clean_text}"
        )
        answer = await call_openai_text(
            system_prompt=(
                f"You are GOLU Smart Screen Assistant for Boss. Respond in {payload.language}. "
                "Explain or summarize only the user-provided visible screen text."
            ),
            user_prompt=prompt,
            max_tokens=550,
        )
        return ScreenUnderstandResponse(
            action=payload.action,
            answer=answer,
            extracted_highlights=highlights,
            provider=settings.AI_PROVIDER,
            ai_backed=True,
        )

    fallback_summary = (
        f"Local screen extraction ({payload.action}) — Cloud AI key not configured on server. "
        f"Captured {len(clean_text)} characters across {len(highlights)} key line(s)."
    )
    return ScreenUnderstandResponse(
        action=payload.action,
        answer=fallback_summary,
        extracted_highlights=highlights,
        provider="local-extractor",
        ai_backed=False,
    )


@router.post("/camera/analyze", response_model=CameraAnalyzeResponse)
async def analyze_camera_frame(
    payload: CameraAnalyzeRequest,
    auth=Depends(get_current_user_and_session),
):
    user, _ = auth
    ai_rate_limiter.check(f"v2_camera:{user.id}", limit=20, window_seconds=60)
    if not payload.consent_granted:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Explicit camera capture consent is required.",
        )

    prompt = (
        f"Mode: {payload.mode}. User question: {payload.question}. "
        "Provide: 1) Scene/Object description, 2) Any clearly legible text, 3) Practical next help."
    )
    analysis = await call_openai_vision(payload.image_base64, prompt, payload.language)
    detected_lines = _extract_highlights(analysis, 4)
    return CameraAnalyzeResponse(
        mode=payload.mode,
        description=analysis,
        detected_text=detected_lines[0] if payload.mode == "OCR" and detected_lines else "",
        objects=detected_lines[:3],
        provider=get_settings().OPENAI_VISION_MODEL,
        ai_backed=True,
    )


def _to_doc_response(doc: KnowledgeDocument) -> DocumentResponse:
    try:
        points = json.loads(doc.key_points_json or "[]")
        if not isinstance(points, list):
            points = []
    except Exception:
        points = []
    return DocumentResponse(
        id=doc.id,
        title=doc.title,
        mime_type=doc.mime_type,
        file_size_bytes=doc.file_size_bytes,
        summary=doc.summary,
        key_points=[str(p) for p in points],
        preview_text=doc.extracted_text[:600],
        created_at=doc.created_at,
    )


@router.post("/documents", response_model=DocumentResponse, status_code=status.HTTP_201_CREATED)
async def upload_and_analyze_document(
    payload: DocumentUploadRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    ai_rate_limiter.check(f"v2_docs:{user.id}", limit=20, window_seconds=60)
    if not payload.consent_granted:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Document upload blocked: explicit user consent is required.",
        )

    extracted = sanitize_text(payload.text_content, 40000)
    size_bytes = len(extracted.encode("utf-8"))
    if payload.file_base64.strip():
        parsed_text, parsed_size = extract_text_from_file_bytes(payload.file_base64, payload.mime_type)
        if parsed_text:
            extracted = parsed_text
        size_bytes = max(size_bytes, parsed_size)

    if not extracted:
        raise HTTPException(status_code=400, detail="No readable text could be extracted from the document.")

    key_points = _extract_highlights(extracted, 5)
    summary = f"Document '{sanitize_text(payload.title, 180)}' ({len(extracted)} chars) indexed."
    if get_settings().openai_configured:
        summary = await call_openai_text(
            system_prompt="Summarize the document concisely in 3-4 sentences for Boss without inventing facts.",
            user_prompt=extracted[:9000],
            max_tokens=320,
        )

    doc = KnowledgeDocument(
        user_id=user.id,
        title=sanitize_text(payload.title, 200),
        mime_type=sanitize_text(payload.mime_type, 80) or "text/plain",
        file_size_bytes=size_bytes,
        extracted_text=extracted,
        summary=summary,
        key_points_json=json.dumps(key_points),
        consent_granted=True,
    )
    db.add(doc)
    db.commit()
    db.refresh(doc)
    return _to_doc_response(doc)


@router.get("/documents", response_model=List[DocumentResponse])
def list_documents(
    q: Optional[str] = Query(default=None, max_length=120),
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rows = (
        db.query(KnowledgeDocument)
        .filter(KnowledgeDocument.user_id == user.id)
        .order_by(KnowledgeDocument.created_at.desc())
        .all()
    )
    if q and q.strip():
        needle = q.strip().lower()
        rows = [
            r for r in rows
            if needle in r.title.lower() or needle in r.summary.lower() or needle in r.extracted_text.lower()
        ]
    return [_to_doc_response(r) for r in rows]


@router.post("/documents/{doc_id}/ask", response_model=DocumentQueryResponse)
async def ask_document(
    doc_id: str,
    payload: DocumentQueryRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    doc = db.query(KnowledgeDocument).filter(KnowledgeDocument.id == doc_id).first()
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found.")
    ensure_owner(doc.user_id, user.id, "Document not found.")

    keywords = [w.lower() for w in payload.question.split() if len(w) > 3]
    lines = [ln.strip() for ln in doc.extracted_text.splitlines() if ln.strip()]
    matched = [ln[:240] for ln in lines if any(k in ln.lower() for k in keywords)][:4]
    if not matched:
        matched = lines[:3]

    if get_settings().openai_configured:
        answer = await call_openai_text(
            system_prompt=(
                f"Answer the question strictly from the provided document in {payload.language}. "
                "If the document does not mention the answer, say so honestly."
            ),
            user_prompt=f"Question: {payload.question}\n\nDocument ({doc.title}):\n{doc.extracted_text[:10000]}",
            max_tokens=450,
        )
        return DocumentQueryResponse(document_id=doc.id, answer=answer, matched_excerpts=matched, ai_backed=True)

    excerpt_text = " | ".join(matched) if matched else doc.summary
    return DocumentQueryResponse(
        document_id=doc.id,
        answer=f"Keyword match from '{doc.title}' (Cloud AI unconfigured): {excerpt_text}",
        matched_excerpts=matched,
        ai_backed=False,
    )


@router.delete("/documents/{doc_id}", response_model=MessageResponse)
def delete_document(
    doc_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    doc = db.query(KnowledgeDocument).filter(KnowledgeDocument.id == doc_id).first()
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found.")
    ensure_owner(doc.user_id, user.id, "Document not found.")
    db.delete(doc)
    db.commit()
    return MessageResponse(message="Document deleted securely.")


def _classify_notification(title: str, body: str) -> tuple[str, int, bool]:
    text = f"{title} {body}".lower()
    if any(k in text for k in ("otp", "bank", "debited", "credited", "upi", "payment", "transaction")):
        return "FINANCE_ALERT", 5, True
    if any(k in text for k in ("urgent", "alert", "security", "emergency", "missed call", "deadline")):
        return "URGENT", 5, True
    if any(k in text for k in ("meeting", "calendar", "zoom", "interview", "class", "flight", "train")):
        return "SCHEDULE", 4, True
    if any(k in text for k in ("delivered", "order", "shipped", "parcel", "package")):
        return "DELIVERY", 3, False
    if any(k in text for k in ("whatsapp", "message", "replied", "mentioned")):
        return "COMMUNICATION", 3, False
    return "GENERAL", 1, False


@router.post("/notifications/analyze", response_model=NotificationInsightResponse, status_code=status.HTTP_201_CREATED)
def ingest_notification_insight(
    payload: NotificationIngestRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    if not payload.consent_granted:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Explicit notification permission & user consent are required.",
        )

    title = sanitize_text(payload.title, 200)
    body = sanitize_text(payload.body_preview, 2000)
    category, score, important = _classify_notification(title, body)
    summary = f"[{category}] {payload.source_app}: {title}"
    reminder_id = None
    if payload.create_reminder_if_important and important:
        rem = Reminder(user_id=user.id, title=f"Follow up: {title[:160]}", notes=body[:400])
        db.add(rem)
        db.flush()
        reminder_id = rem.id

    rec = NotificationInsight(
        user_id=user.id,
        source_app=sanitize_text(payload.source_app, 120) or "Android",
        title=title,
        body_preview=body[:400],
        category=category,
        priority_score=score,
        is_important=important,
        ai_summary=summary,
        converted_reminder_id=reminder_id,
    )
    db.add(rec)
    db.commit()
    db.refresh(rec)
    return NotificationInsightResponse(
        id=rec.id,
        source_app=rec.source_app,
        title=rec.title,
        body_preview=rec.body_preview,
        category=rec.category,
        priority_score=rec.priority_score,
        is_important=rec.is_important,
        ai_summary=rec.ai_summary,
        converted_reminder_id=rec.converted_reminder_id,
        created_at=rec.created_at,
    )


@router.get("/notifications/summary", response_model=NotificationSummaryResponse)
def get_notification_summary(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rows = (
        db.query(NotificationInsight)
        .filter(NotificationInsight.user_id == user.id)
        .order_by(NotificationInsight.created_at.desc())
        .limit(50)
        .all()
    )
    important = [r for r in rows if r.is_important]
    summary = (
        f"You have {len(rows)} analyzed notification(s), {len(important)} marked important."
        if rows
        else "No notification insights analyzed yet."
    )
    return NotificationSummaryResponse(
        total_count=len(rows),
        important_count=len(important),
        summary=summary,
        items=[
            NotificationInsightResponse(
                id=r.id,
                source_app=r.source_app,
                title=r.title,
                body_preview=r.body_preview,
                category=r.category,
                priority_score=r.priority_score,
                is_important=r.is_important,
                ai_summary=r.ai_summary,
                converted_reminder_id=r.converted_reminder_id,
                created_at=r.created_at,
            )
            for r in rows
        ],
    )


@router.delete("/notifications/{insight_id}", response_model=MessageResponse)
def delete_notification_insight(
    insight_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    row = db.query(NotificationInsight).filter(NotificationInsight.id == insight_id).first()
    if not row:
        raise HTTPException(status_code=404, detail="Notification insight not found.")
    ensure_owner(row.user_id, user.id, "Notification insight not found.")
    db.delete(row)
    db.commit()
    return MessageResponse(message="Notification insight removed.")
