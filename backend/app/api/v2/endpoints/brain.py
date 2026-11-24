import json
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.middleware.rate_limit import ai_rate_limiter
from app.models.golu_v2 import ResearchReport, TranslationHistory
from app.models.memory import MemoryAuditEvent, MemoryRecord
from app.schemas.common import MessageResponse
from app.schemas.golu_v2 import (
    CitationItem,
    KnowledgeNoteRequest,
    KnowledgeNoteResponse,
    OfflineManifestResponse,
    ResearchReportResponse,
    ResearchRequest,
    TranslateRequest,
    TranslationResponse,
)
from app.security.sanitizer import ensure_owner, sanitize_text
from app.services.golu_v2_ai import call_openai_text, perform_legitimate_web_research

router = APIRouter(tags=["Phase 17 — Advanced Brain & Knowledge (v2)"])


def _tags_to_csv(tags: List[str]) -> str:
    cleaned = [sanitize_text(t, 30).replace(",", "") for t in tags if t and t.strip()]
    return ",".join(cleaned[:10])


def _csv_to_tags(csv: str) -> List[str]:
    return [t.strip() for t in (csv or "").split(",") if t.strip()]


def _to_note_response(rec: MemoryRecord) -> KnowledgeNoteResponse:
    title = rec.title.strip() if rec.title else rec.content[:60]
    return KnowledgeNoteResponse(
        id=rec.id,
        title=title,
        category=rec.category,
        content=rec.content,
        tags=_csv_to_tags(rec.tags_csv),
        family_shared=rec.family_shared,
        created_at=rec.created_at,
        updated_at=rec.updated_at,
    )


# Feature 5: Personal Knowledge Brain — unified directly with MemoryRecord
@router.get("/knowledge", response_model=List[KnowledgeNoteResponse])
def search_knowledge_notes(
    q: Optional[str] = Query(default=None, max_length=120),
    category: Optional[str] = Query(default=None, max_length=60),
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    query = db.query(MemoryRecord).filter(MemoryRecord.user_id == user.id)
    if category and category.strip():
        query = query.filter(MemoryRecord.category == category.strip())
    rows = query.order_by(MemoryRecord.updated_at.desc()).all()
    if q and q.strip():
        needle = q.strip().lower()
        rows = [
            r for r in rows
            if needle in (r.title or "").lower()
            or needle in r.content.lower()
            or needle in (r.tags_csv or "").lower()
            or needle in r.category.lower()
        ]
    return [_to_note_response(r) for r in rows]


@router.post("/knowledge", response_model=KnowledgeNoteResponse, status_code=status.HTTP_201_CREATED)
def create_knowledge_note(
    payload: KnowledgeNoteRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rec = MemoryRecord(
        user_id=user.id,
        title=sanitize_text(payload.title, 160),
        category=sanitize_text(payload.category, 60) or "Custom Notes",
        content=sanitize_text(payload.content, 6000),
        tags_csv=_tags_to_csv(payload.tags),
        family_shared=payload.family_shared,
    )
    db.add(rec)
    db.flush()
    db.add(
        MemoryAuditEvent(
            user_id=user.id,
            memory_id=rec.id,
            action="KNOWLEDGE_CREATE",
            category=rec.category,
            detail=f"Added note '{rec.title[:80]}'",
        )
    )
    db.commit()
    db.refresh(rec)
    return _to_note_response(rec)


@router.put("/knowledge/{note_id}", response_model=KnowledgeNoteResponse)
def update_knowledge_note(
    note_id: str,
    payload: KnowledgeNoteRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rec = db.query(MemoryRecord).filter(MemoryRecord.id == note_id).first()
    if not rec:
        raise HTTPException(status_code=404, detail="Knowledge note not found.")
    ensure_owner(rec.user_id, user.id, "Knowledge note not found.")
    rec.title = sanitize_text(payload.title, 160)
    rec.category = sanitize_text(payload.category, 60) or rec.category
    rec.content = sanitize_text(payload.content, 6000)
    rec.tags_csv = _tags_to_csv(payload.tags)
    rec.family_shared = payload.family_shared
    db.add(
        MemoryAuditEvent(
            user_id=user.id,
            memory_id=rec.id,
            action="KNOWLEDGE_UPDATE",
            category=rec.category,
            detail=f"Updated note '{rec.title[:80]}'",
        )
    )
    db.commit()
    db.refresh(rec)
    return _to_note_response(rec)


@router.delete("/knowledge/{note_id}", response_model=MessageResponse)
def delete_knowledge_note(
    note_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rec = db.query(MemoryRecord).filter(MemoryRecord.id == note_id).first()
    if not rec:
        raise HTTPException(status_code=404, detail="Knowledge note not found.")
    ensure_owner(rec.user_id, user.id, "Knowledge note not found.")
    db.add(
        MemoryAuditEvent(
            user_id=user.id,
            memory_id=rec.id,
            action="KNOWLEDGE_DELETE",
            category=rec.category,
            detail=f"Deleted note '{(rec.title or rec.content)[:80]}'",
        )
    )
    db.delete(rec)
    db.commit()
    return MessageResponse(message="Knowledge note deleted.")


# Feature 6: Live Language Translator
@router.post("/translator/translate", response_model=TranslationResponse, status_code=status.HTTP_201_CREATED)
async def translate_text(
    payload: TranslateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    ai_rate_limiter.check(f"v2_translate:{user.id}", limit=40, window_seconds=60)
    source = sanitize_text(payload.text, 4000)

    translated = await call_openai_text(
        system_prompt=(
            f"Translate accurately from {payload.source_language} to {payload.target_language}. "
            "Output ONLY the natural translated text, preserving tone and meaning."
        ),
        user_prompt=source,
        max_tokens=500,
    )

    row = TranslationHistory(
        user_id=user.id,
        source_language=sanitize_text(payload.source_language, 40),
        target_language=sanitize_text(payload.target_language, 40),
        source_text=source,
        translated_text=translated,
        mode=payload.mode,
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return TranslationResponse(
        id=row.id,
        source_language=row.source_language,
        target_language=row.target_language,
        source_text=row.source_text,
        translated_text=row.translated_text,
        mode=row.mode,
        ai_backed=True,
        created_at=row.created_at,
    )


@router.get("/translator/history", response_model=List[TranslationResponse])
def list_translations(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rows = (
        db.query(TranslationHistory)
        .filter(TranslationHistory.user_id == user.id)
        .order_by(TranslationHistory.created_at.desc())
        .limit(40)
        .all()
    )
    return [
        TranslationResponse(
            id=r.id,
            source_language=r.source_language,
            target_language=r.target_language,
            source_text=r.source_text,
            translated_text=r.translated_text,
            mode=r.mode,
            ai_backed=True,
            created_at=r.created_at,
        )
        for r in rows
    ]


@router.delete("/translator/history/{item_id}", response_model=MessageResponse)
def delete_translation(
    item_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    row = db.query(TranslationHistory).filter(TranslationHistory.id == item_id).first()
    if not row:
        raise HTTPException(status_code=404, detail="Translation entry not found.")
    ensure_owner(row.user_id, user.id, "Translation entry not found.")
    db.delete(row)
    db.commit()
    return MessageResponse(message="Translation entry deleted.")


# Feature 7: Deep Research Mode
@router.post("/research/query", response_model=ResearchReportResponse, status_code=status.HTTP_201_CREATED)
async def run_deep_research(
    payload: ResearchRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    ai_rate_limiter.check(f"v2_research:{user.id}", limit=15, window_seconds=60)
    query = sanitize_text(payload.query, 300)
    provider, raw_citations, err = await perform_legitimate_web_research(query)
    citations = [CitationItem(**c) for c in raw_citations]

    if not citations:
        summary = err or "No live external sources returned results for this query."
        verified = "No verified external web sources retrieved."
        uncertain = "Live search connectivity or API key is required for verified citations."
    else:
        source_digest = "\n".join(f"- {c.title} ({c.url}): {c.snippet}" for c in citations)
        if get_settings().openai_configured:
            summary = await call_openai_text(
                system_prompt=(
                    f"Write a structured research summary in {payload.language} based ONLY on the retrieved sources below. "
                    "Never invent citations or facts not supported by those sources."
                ),
                user_prompt=f"Topic: {query}\n\nSources:\n{source_digest}",
                max_tokens=550,
            )
        else:
            summary = f"Retrieved {len(citations)} source(s) for '{query}'. Review citations below."
        verified = "\n".join(f"• {c.title}: {c.snippet}" for c in citations[:3])
        uncertain = "Any claim outside the linked sources has not been independently verified."

    report = ResearchReport(
        user_id=user.id,
        query=query,
        summary=summary,
        verified_findings=verified,
        uncertain_points=uncertain,
        citations_json=json.dumps([c.model_dump() for c in citations]),
        provider_used=provider,
    )
    db.add(report)
    db.commit()
    db.refresh(report)
    return ResearchReportResponse(
        id=report.id,
        query=report.query,
        summary=report.summary,
        verified_findings=report.verified_findings,
        uncertain_points=report.uncertain_points,
        citations=citations,
        provider_used=report.provider_used,
        created_at=report.created_at,
    )


@router.get("/research/history", response_model=List[ResearchReportResponse])
def list_research_history(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rows = (
        db.query(ResearchReport)
        .filter(ResearchReport.user_id == user.id)
        .order_by(ResearchReport.created_at.desc())
        .limit(25)
        .all()
    )
    out: List[ResearchReportResponse] = []
    for r in rows:
        try:
            cits = [CitationItem(**item) for item in json.loads(r.citations_json or "[]")]
        except Exception:
            cits = []
        out.append(
            ResearchReportResponse(
                id=r.id,
                query=r.query,
                summary=r.summary,
                verified_findings=r.verified_findings,
                uncertain_points=r.uncertain_points,
                citations=cits,
                provider_used=r.provider_used,
                created_at=r.created_at,
            )
        )
    return out


# Feature 8: Advanced Offline Brain Manifest
@router.get("/offline/manifest", response_model=OfflineManifestResponse)
def get_offline_manifest():
    return OfflineManifestResponse(
        offline_capable_features=[
            "Local greetings, time, date & battery telemetry",
            "On-device app launcher & smart app shortcuts",
            "Alarms, timers, WorkManager reminders & calendar intents",
            "Volume, media playback & Android settings navigation",
            "Local math/unit calculations & Hindi/English phrasebook",
            "Offline PDF page preview & local audio recording",
        ],
        online_required_features=[
            "Cloud LLM chat, coding assistant & study tutor generation",
            "Camera AI vision & OpenAI image generation",
            "Live web deep research with external citations",
            "Multi-device PostgreSQL cloud synchronization",
        ],
        local_command_examples=[
            "Time kya hua?",
            "Battery kitni hai?",
            "WhatsApp kholo",
            "Alarm lagao 07:00",
            "Calculate 125 * 8",
            "Reminder set karo meeting",
        ],
        cloud_ai_note="Cloud AI never runs offline. GOLU switches automatically between local execution and FastAPI AI.",
    )
