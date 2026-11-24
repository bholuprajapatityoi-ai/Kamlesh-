from datetime import datetime, timedelta, timezone
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.middleware.rate_limit import ai_rate_limiter
from app.models.daily_life import CalendarEvent, Reminder, TaskItem
from app.models.golu_v2 import CodingHistory, CreativeAsset, VoiceRecording
from app.schemas.common import MessageResponse
from app.schemas.golu_v2 import (
    CodingAssistRequest,
    CodingAssistResponse,
    CreativeAssetResponse,
    CreativeRequest,
    PlannedItemProposal,
    PlannerApproveRequest,
    PlannerSuggestRequest,
    PlannerSuggestResponse,
    RecordingCreateRequest,
    RecordingResponse,
    RecordingUpdateRequest,
)
from app.security.sanitizer import ensure_owner, sanitize_text
from app.services.golu_v2_ai import (
    call_openai_image,
    call_openai_text,
    scrub_generated_code_secrets,
)

router = APIRouter(tags=["Phase 18 — Productivity & Creative Intelligence (v2)"])


# Feature 9: Life Planner AI (reuses TaskItem, Reminder, CalendarEvent)
@router.post("/planner/suggest", response_model=PlannerSuggestResponse)
async def suggest_daily_plan(
    payload: PlannerSuggestRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    pending_tasks = (
        db.query(TaskItem)
        .filter(TaskItem.user_id == user.id, TaskItem.is_completed == False)  # noqa: E712
        .order_by(TaskItem.created_at.desc())
        .limit(10)
        .all()
    )
    active_reminders = (
        db.query(Reminder)
        .filter(Reminder.user_id == user.id, Reminder.is_completed == False)  # noqa: E712
        .limit(10)
        .all()
    )

    proposals: List[PlannedItemProposal] = [
        PlannedItemProposal(
            title=f"Deep Focus Block: {sanitize_text(payload.goal_focus, 100)}",
            item_type="CALENDAR_EVENT",
            priority="HIGH",
            suggested_slot="09:00 - 10:30",
            reason="High-energy morning deep work session aligned with your focus goal.",
        ),
        PlannedItemProposal(
            title="Review pending tasks & prioritize top 2 deliverables",
            item_type="TASK",
            priority="HIGH",
            suggested_slot="11:00 - 12:00",
            reason=f"You currently have {len(pending_tasks)} pending task(s) and {len(active_reminders)} reminder(s).",
        ),
        PlannedItemProposal(
            title="Evening revision, hydration & next-day prep",
            item_type="REMINDER",
            priority="MEDIUM",
            suggested_slot="20:00",
            reason="Keeps daily routine balanced without overloading.",
        ),
    ]

    summary = f"Proposed a {payload.available_hours}h plan focused on '{payload.goal_focus}'."
    if get_settings().openai_configured:
        summary = await call_openai_text(
            system_prompt=f"Act as GOLU Life Planner AI in {payload.language}. Give a crisp 3-line plan overview.",
            user_prompt=(
                f"Focus: {payload.goal_focus}, Hours: {payload.available_hours}, "
                f"Pending Tasks: {[t.title for t in pending_tasks]}, "
                f"Reminders: {[r.title for r in active_reminders]}"
            ),
            max_tokens=260,
        )

    return PlannerSuggestResponse(summary=summary, proposals=proposals, requires_user_approval=True)


@router.post("/planner/approve", response_model=MessageResponse)
def approve_planned_items(
    payload: PlannerApproveRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    now = datetime.now(timezone.utc)
    created = 0
    for idx, item in enumerate(payload.items):
        clean_title = sanitize_text(item.title, 180)
        if not clean_title:
            continue
        kind = item.item_type.upper()
        if kind == "REMINDER":
            db.add(Reminder(user_id=user.id, title=clean_title, notes=sanitize_text(item.reason, 200), remind_at=now + timedelta(hours=idx + 1)))
        elif kind == "CALENDAR_EVENT":
            start = now + timedelta(hours=idx + 1)
            db.add(CalendarEvent(user_id=user.id, title=clean_title, description=sanitize_text(item.reason, 200), start_at=start, end_at=start + timedelta(hours=1)))
        else:
            db.add(TaskItem(user_id=user.id, title=clean_title, priority=item.priority if item.priority in ("LOW", "MEDIUM", "HIGH") else "MEDIUM"))
        created += 1
    db.commit()
    return MessageResponse(message=f"Saved {created} approved item(s) to your Daily Life Planner.")


# Feature 10: Coding Assistant
@router.post("/coding/assist", response_model=CodingAssistResponse, status_code=status.HTTP_201_CREATED)
async def assist_coding(
    payload: CodingAssistRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    ai_rate_limiter.check(f"v2_coding:{user.id}", limit=25, window_seconds=60)
    prompt = sanitize_text(payload.prompt, 6000)

    raw_response = await call_openai_text(
        system_prompt=(
            f"You are GOLU Senior Coding Assistant for {payload.language} ({payload.task_type}). "
            "Never hardcode API keys, passwords, or secrets in code—use environment variables/BuildConfig. "
            "Provide clean code and a concise step-by-step explanation."
        ),
        user_prompt=prompt,
        max_tokens=900,
    )
    safe_code, security_note = scrub_generated_code_secrets(raw_response)

    rec = CodingHistory(
        user_id=user.id,
        language=sanitize_text(payload.language, 40),
        task_type=payload.task_type,
        prompt=prompt[:2000],
        code_output=safe_code,
        explanation=security_note,
    )
    db.add(rec)
    db.commit()
    db.refresh(rec)
    return CodingAssistResponse(
        id=rec.id,
        language=rec.language,
        task_type=rec.task_type,
        prompt=rec.prompt,
        code_output=rec.code_output,
        explanation=rec.explanation,
        security_note=security_note,
        ai_backed=True,
        created_at=rec.created_at,
    )


@router.get("/coding/history", response_model=List[CodingAssistResponse])
def list_coding_history(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rows = (
        db.query(CodingHistory)
        .filter(CodingHistory.user_id == user.id)
        .order_by(CodingHistory.created_at.desc())
        .limit(30)
        .all()
    )
    return [
        CodingAssistResponse(
            id=r.id,
            language=r.language,
            task_type=r.task_type,
            prompt=r.prompt,
            code_output=r.code_output,
            explanation=r.explanation,
            security_note="Reviewed statically. Never execute untrusted snippets automatically.",
            ai_backed=True,
            created_at=r.created_at,
        )
        for r in rows
    ]


# Feature 11: Creative Studio
@router.post("/creative/generate", response_model=CreativeAssetResponse, status_code=status.HTTP_201_CREATED)
async def create_creative_asset(
    payload: CreativeRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    ai_rate_limiter.check(f"v2_creative:{user.id}", limit=10, window_seconds=60)
    full_prompt = sanitize_text(f"{payload.prompt} (Style: {payload.style})", 1500)

    if payload.asset_type == "IMAGE":
        try:
            image_url, revised = await call_openai_image(full_prompt)
            rec = CreativeAsset(
                user_id=user.id,
                asset_type=payload.asset_type,
                prompt=full_prompt,
                revised_prompt=revised,
                image_url=image_url,
                design_notes="Generated via OpenAI Images API.",
                status="COMPLETED",
                error_detail="",
            )
            db.add(rec)
            db.commit()
            db.refresh(rec)
        except HTTPException as exc:
            failed = CreativeAsset(
                user_id=user.id,
                asset_type=payload.asset_type,
                prompt=full_prompt,
                status="FAILED",
                error_detail=str(exc.detail)[:250],
            )
            db.add(failed)
            db.commit()
            raise
    else:
        notes = await call_openai_text(
            system_prompt="Provide structured poster layout, color palette, typography, and tagline ideas.",
            user_prompt=full_prompt,
            max_tokens=550,
        )
        rec = CreativeAsset(
            user_id=user.id,
            asset_type=payload.asset_type,
            prompt=full_prompt,
            design_notes=notes,
            status="COMPLETED",
        )
        db.add(rec)
        db.commit()
        db.refresh(rec)

    return CreativeAssetResponse(
        id=rec.id,
        asset_type=rec.asset_type,
        prompt=rec.prompt,
        revised_prompt=rec.revised_prompt,
        image_url=rec.image_url,
        design_notes=rec.design_notes,
        status=rec.status,
        error_detail=rec.error_detail,
        created_at=rec.created_at,
    )


@router.get("/creative/history", response_model=List[CreativeAssetResponse])
def list_creative_assets(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rows = (
        db.query(CreativeAsset)
        .filter(CreativeAsset.user_id == user.id)
        .order_by(CreativeAsset.created_at.desc())
        .limit(30)
        .all()
    )
    return [
        CreativeAssetResponse(
            id=r.id,
            asset_type=r.asset_type,
            prompt=r.prompt,
            revised_prompt=r.revised_prompt,
            image_url=r.image_url,
            design_notes=r.design_notes,
            status=r.status,
            error_detail=r.error_detail,
            created_at=r.created_at,
        )
        for r in rows
    ]


@router.delete("/creative/{asset_id}", response_model=MessageResponse)
def delete_creative_asset(
    asset_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    row = db.query(CreativeAsset).filter(CreativeAsset.id == asset_id).first()
    if not row:
        raise HTTPException(status_code=404, detail="Creative asset not found.")
    ensure_owner(row.user_id, user.id, "Creative asset not found.")
    db.delete(row)
    db.commit()
    return MessageResponse(message="Creative asset deleted.")


# Feature 12: Voice Recorder Intelligence
@router.post("/recorder", response_model=RecordingResponse, status_code=status.HTTP_201_CREATED)
async def create_recording_transcript(
    payload: RecordingCreateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    transcript = sanitize_text(payload.transcript, 50000)
    summary = ""
    lecture_notes = ""
    if payload.generate_summary and transcript:
        if get_settings().openai_configured:
            summary = await call_openai_text(
                system_prompt="Summarize this recording transcript concisely and list 3 lecture takeaways.",
                user_prompt=transcript[:10000],
                max_tokens=360,
            )
            lecture_notes = summary
        else:
            summary = transcript[:220] + ("..." if len(transcript) > 220 else "")
            lecture_notes = f"Local note excerpt ({len(transcript)} chars)."

    rec = VoiceRecording(
        user_id=user.id,
        title=sanitize_text(payload.title, 160),
        duration_seconds=payload.duration_seconds,
        transcript=transcript,
        summary=summary,
        lecture_notes=lecture_notes,
        local_file_hint=sanitize_text(payload.local_file_hint, 255),
    )
    db.add(rec)
    db.commit()
    db.refresh(rec)
    return RecordingResponse(
        id=rec.id,
        title=rec.title,
        duration_seconds=rec.duration_seconds,
        transcript=rec.transcript,
        summary=rec.summary,
        lecture_notes=rec.lecture_notes,
        local_file_hint=rec.local_file_hint,
        created_at=rec.created_at,
        updated_at=rec.updated_at,
    )


@router.get("/recorder", response_model=List[RecordingResponse])
def list_recordings(
    q: Optional[str] = Query(default=None, max_length=120),
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rows = (
        db.query(VoiceRecording)
        .filter(VoiceRecording.user_id == user.id)
        .order_by(VoiceRecording.created_at.desc())
        .all()
    )
    if q and q.strip():
        needle = q.strip().lower()
        rows = [r for r in rows if needle in r.title.lower() or needle in r.transcript.lower()]
    return [
        RecordingResponse(
            id=r.id,
            title=r.title,
            duration_seconds=r.duration_seconds,
            transcript=r.transcript,
            summary=r.summary,
            lecture_notes=r.lecture_notes,
            local_file_hint=r.local_file_hint,
            created_at=r.created_at,
            updated_at=r.updated_at,
        )
        for r in rows
    ]


@router.put("/recorder/{rec_id}", response_model=RecordingResponse)
async def update_recording(
    rec_id: str,
    payload: RecordingUpdateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rec = db.query(VoiceRecording).filter(VoiceRecording.id == rec_id).first()
    if not rec:
        raise HTTPException(status_code=404, detail="Recording not found.")
    ensure_owner(rec.user_id, user.id, "Recording not found.")
    if payload.title is not None:
        rec.title = sanitize_text(payload.title, 160)
    if payload.transcript is not None:
        rec.transcript = sanitize_text(payload.transcript, 50000)
    if payload.regenerate_summary and rec.transcript and get_settings().openai_configured:
        rec.summary = await call_openai_text(
            system_prompt="Summarize the edited transcript clearly in bullet points.",
            user_prompt=rec.transcript[:10000],
            max_tokens=360,
        )
        rec.lecture_notes = rec.summary
    db.commit()
    db.refresh(rec)
    return RecordingResponse(
        id=rec.id,
        title=rec.title,
        duration_seconds=rec.duration_seconds,
        transcript=rec.transcript,
        summary=rec.summary,
        lecture_notes=rec.lecture_notes,
        local_file_hint=rec.local_file_hint,
        created_at=rec.created_at,
        updated_at=rec.updated_at,
    )


@router.delete("/recorder/{rec_id}", response_model=MessageResponse)
def delete_recording(
    rec_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rec = db.query(VoiceRecording).filter(VoiceRecording.id == rec_id).first()
    if not row_exists(rec):
        raise HTTPException(status_code=404, detail="Recording not found.")
    ensure_owner(rec.user_id, user.id, "Recording not found.")
    db.delete(rec)
    db.commit()
    return MessageResponse(message="Recording deleted.")


def row_exists(row) -> bool:
    return row is not None
