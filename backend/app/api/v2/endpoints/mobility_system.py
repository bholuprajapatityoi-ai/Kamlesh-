import json
from datetime import datetime, timezone
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.redis_client import redis_client
from app.database.connection import check_database_health
from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.models.device_notification import DeviceRegistration
from app.models.golu_v2 import (
    DiagnosticEvent,
    ExpenseRecord,
    KnowledgeDocument,
    StudySession,
    SyncCheckpoint,
    TravelPlan,
)
from app.models.memory import MemoryRecord
from app.schemas.common import MessageResponse
from app.schemas.golu_v2 import (
    DiagnosticCheckInput,
    DiagnosticReportResponse,
    MultiDeviceOverviewResponse,
    StudyAskRequest,
    StudyProgressUpdateRequest,
    StudySessionResponse,
    SyncCheckpointResponse,
    SyncPushRequest,
    TravelPlaceItem,
    TravelPlanRequest,
    TravelPlanResponse,
)
from app.security.sanitizer import ensure_owner, sanitize_text
from app.services.golu_v2_ai import call_openai_text, search_nearby_places_osm

router = APIRouter(tags=["Phase 20 — Learning, Mobility & System Intelligence (v2)"])


# Feature 17: Study Tutor Mode
@router.post("/study/explain", response_model=StudySessionResponse, status_code=status.HTTP_201_CREATED)
async def explain_study_topic(
    payload: StudyAskRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    subject = sanitize_text(payload.subject, 100)
    topic = sanitize_text(payload.topic, 180)
    explanation = await call_openai_text(
        system_prompt=(
            f"You are GOLU Study Tutor teaching Boss in {payload.language} ({payload.difficulty} level). "
            "Teach step-by-step for deep conceptual understanding, not rote memorization. "
            "Include: 1) Core concept, 2) Step-by-step intuition, 3) Quick revision recap."
        ),
        user_prompt=f"Subject: {subject}\nTopic: {topic}",
        max_tokens=700,
    )
    questions = [
        f"Explain the core intuition behind {topic} in your own words.",
        f"Where is {topic} applied practically in {subject}?",
        f"What common mistake should you avoid when solving {topic} problems?",
    ]
    rec = StudySession(
        user_id=user.id,
        subject=subject,
        topic=topic,
        language=sanitize_text(payload.language, 30),
        difficulty=sanitize_text(payload.difficulty, 30),
        explanation=explanation,
        practice_questions_json=json.dumps(questions),
        mastery_pct=20,
    )
    db.add(rec)
    db.commit()
    db.refresh(rec)
    return StudySessionResponse(
        id=rec.id,
        subject=rec.subject,
        topic=rec.topic,
        language=rec.language,
        difficulty=rec.difficulty,
        explanation=rec.explanation,
        practice_questions=questions,
        mastery_pct=rec.mastery_pct,
        created_at=rec.created_at,
    )


@router.get("/study/sessions", response_model=List[StudySessionResponse])
def list_study_sessions(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rows = (
        db.query(StudySession)
        .filter(StudySession.user_id == user.id)
        .order_by(StudySession.updated_at.desc())
        .limit(30)
        .all()
    )
    out: List[StudySessionResponse] = []
    for r in rows:
        try:
            qs = json.loads(r.practice_questions_json or "[]")
        except Exception:
            qs = []
        out.append(
            StudySessionResponse(
                id=r.id,
                subject=r.subject,
                topic=r.topic,
                language=r.language,
                difficulty=r.difficulty,
                explanation=r.explanation,
                practice_questions=[str(q) for q in qs],
                mastery_pct=r.mastery_pct,
                created_at=r.created_at,
            )
        )
    return out


@router.put("/study/sessions/{session_id}/progress", response_model=StudySessionResponse)
def update_study_progress(
    session_id: str,
    payload: StudyProgressUpdateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rec = db.query(StudySession).filter(StudySession.id == session_id).first()
    if not rec:
        raise HTTPException(status_code=404, detail="Study session not found.")
    ensure_owner(rec.user_id, user.id, "Study session not found.")
    rec.mastery_pct = payload.mastery_pct
    db.commit()
    db.refresh(rec)
    try:
        qs = json.loads(rec.practice_questions_json or "[]")
    except Exception:
        qs = []
    return StudySessionResponse(
        id=rec.id,
        subject=rec.subject,
        topic=rec.topic,
        language=rec.language,
        difficulty=rec.difficulty,
        explanation=rec.explanation,
        practice_questions=[str(q) for q in qs],
        mastery_pct=rec.mastery_pct,
        created_at=rec.created_at,
    )


# Feature 18: Travel Intelligence
@router.post("/travel/plan", response_model=TravelPlanResponse, status_code=status.HTTP_201_CREATED)
async def create_travel_plan(
    payload: TravelPlanRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    origin = sanitize_text(payload.origin, 160)
    destination = sanitize_text(payload.destination, 160)
    raw_places, notice = await search_nearby_places_osm(destination, payload.latitude, payload.longitude)
    places = [TravelPlaceItem(**p) for p in raw_places]

    route_summary = (
        f"Route guidance from {origin or 'your current area'} to {destination}: "
        f"Found {len(places)} OpenStreetMap reference match(es)."
    )
    itinerary = f"Interests: {payload.interests}. Verify local transit & timings before departure."
    if get_settings().openai_configured:
        itinerary = await call_openai_text(
            system_prompt=(
                "Create a concise practical travel checklist and day plan. "
                "Do NOT invent live flight/train prices, live traffic durations, or opening hours."
            ),
            user_prompt=(
                f"Origin: {origin or 'Current location'}\nDestination: {destination}\n"
                f"Interests: {payload.interests}\nVerified map matches: {[p.name for p in places]}"
            ),
            max_tokens=420,
        )

    plan = TravelPlan(
        user_id=user.id,
        origin=origin,
        destination=destination,
        route_summary=route_summary,
        nearby_places_json=json.dumps([p.model_dump() for p in places]),
        itinerary_notes=itinerary,
    )
    db.add(plan)
    db.commit()
    db.refresh(plan)
    return TravelPlanResponse(
        id=plan.id,
        origin=plan.origin,
        destination=plan.destination,
        route_summary=plan.route_summary,
        nearby_places=places,
        itinerary_notes=plan.itinerary_notes,
        live_data_notice=notice,
        created_at=plan.created_at,
    )


@router.get("/travel/history", response_model=List[TravelPlanResponse])
def list_travel_plans(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rows = (
        db.query(TravelPlan)
        .filter(TravelPlan.user_id == user.id)
        .order_by(TravelPlan.created_at.desc())
        .limit(20)
        .all()
    )
    out: List[TravelPlanResponse] = []
    for r in rows:
        try:
            places = [TravelPlaceItem(**p) for p in json.loads(r.nearby_places_json or "[]")]
        except Exception:
            places = []
        out.append(
            TravelPlanResponse(
                id=r.id,
                origin=r.origin,
                destination=r.destination,
                route_summary=r.route_summary,
                nearby_places=places,
                itinerary_notes=r.itinerary_notes,
                live_data_notice="Saved itinerary. Live traffic/booking status requires external live APIs.",
                created_at=r.created_at,
            )
        )
    return out


# Feature 19: Multi-Device Sync (reuses DeviceRegistration)
@router.get("/devices/sync", response_model=MultiDeviceOverviewResponse)
def get_multi_device_sync_overview(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    devices = (
        db.query(DeviceRegistration)
        .filter(DeviceRegistration.user_id == user.id)
        .order_by(DeviceRegistration.last_seen_at.desc())
        .all()
    )
    checkpoints = (
        db.query(SyncCheckpoint)
        .filter(SyncCheckpoint.user_id == user.id)
        .order_by(SyncCheckpoint.synced_at.desc())
        .limit(20)
        .all()
    )
    notes_count = db.query(KnowledgeDocument).filter(KnowledgeDocument.user_id == user.id).count()
    memories_count = db.query(MemoryRecord).filter(MemoryRecord.user_id == user.id).count()
    expenses_count = db.query(ExpenseRecord).filter(ExpenseRecord.user_id == user.id).count()

    return MultiDeviceOverviewResponse(
        devices=[
            {
                "id": d.id,
                "device_identifier": d.device_identifier,
                "device_name": d.device_name,
                "platform": d.platform,
                "app_version": d.app_version,
                "is_active": d.is_active,
                "last_seen_at": d.last_seen_at.isoformat(),
            }
            for d in devices
        ],
        checkpoints=[
            SyncCheckpointResponse(
                id=c.id,
                device_identifier=c.device_identifier,
                scope=c.scope,
                conflict_policy=c.conflict_policy,
                synced_items_count=c.synced_items_count,
                synced_at=c.synced_at,
            )
            for c in checkpoints
        ],
        notes_count=notes_count,
        memories_count=memories_count,
        expenses_count=expenses_count,
    )


@router.post("/devices/sync", response_model=SyncCheckpointResponse, status_code=status.HTTP_201_CREATED)
def sync_device_state(
    payload: SyncPushRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    device_id = sanitize_text(payload.device_identifier, 120)
    dev = (
        db.query(DeviceRegistration)
        .filter(DeviceRegistration.user_id == user.id, DeviceRegistration.device_identifier == device_id)
        .first()
    )
    if not dev:
        dev = DeviceRegistration(
            user_id=user.id,
            device_identifier=device_id,
            device_name=f"Linked Device ({device_id[:16]})",
            platform="android",
        )
        db.add(dev)
    else:
        dev.last_seen_at = datetime.now(timezone.utc)

    total_items = (
        db.query(MemoryRecord).filter(MemoryRecord.user_id == user.id).count()
        + db.query(KnowledgeDocument).filter(KnowledgeDocument.user_id == user.id).count()
        + db.query(ExpenseRecord).filter(ExpenseRecord.user_id == user.id).count()
    )
    chk = SyncCheckpoint(
        user_id=user.id,
        device_identifier=device_id,
        scope=sanitize_text(payload.scope, 40),
        conflict_policy=payload.conflict_policy,
        synced_items_count=total_items,
    )
    db.add(chk)
    db.commit()
    db.refresh(chk)
    return SyncCheckpointResponse(
        id=chk.id,
        device_identifier=chk.device_identifier,
        scope=chk.scope,
        conflict_policy=chk.conflict_policy,
        synced_items_count=chk.synced_items_count,
        synced_at=chk.synced_at,
    )


# Feature 20: Self-Diagnostic System
@router.post("/diagnostics/run", response_model=DiagnosticReportResponse)
def run_self_diagnostics(
    payload: DiagnosticCheckInput,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    settings = get_settings()
    db_ok, db_err = check_database_health(db)
    redis_ok = redis_client.ping() if settings.redis_configured else False

    suggestions: List[str] = []
    if not db_ok:
        suggestions.append(f"PostgreSQL unreachable ({db_err}). Verify DATABASE_URL and Alembic migrations.")
    if not settings.openai_configured:
        suggestions.append("Configure OPENAI_API_KEY on backend for Cloud AI, Vision, Translator & Image Studio.")
    if not payload.mic_permission:
        suggestions.append("Grant RECORD_AUDIO permission for voice commands, wake-word, and recorder.")
    if not payload.camera_permission:
        suggestions.append("Grant CAMERA permission to activate Live Camera AI.")
    if not payload.device_online:
        suggestions.append("Device offline: local offline brain commands remain available.")

    overall = "HEALTHY" if (db_ok and settings.openai_configured) else ("DEGRADED" if db_ok else "CRITICAL")
    event = DiagnosticEvent(
        user_id=user.id,
        component="FULL_SYSTEM_CHECK",
        status=overall,
        detail=f"db={db_ok}, redis={redis_ok}, openai={settings.openai_configured}",
    )
    db.add(event)
    db.commit()

    recent = (
        db.query(DiagnosticEvent)
        .filter(DiagnosticEvent.user_id == user.id)
        .order_by(DiagnosticEvent.created_at.desc())
        .limit(15)
        .all()
    )
    return DiagnosticReportResponse(
        backend_ok=True,
        database_ok=db_ok,
        redis_ok=redis_ok,
        openai_configured=settings.openai_configured,
        search_api_configured=bool(settings.TAVILY_API_KEY.strip()),
        overall_status=overall,
        troubleshooting_suggestions=suggestions,
        recent_events=[
            {
                "id": e.id,
                "component": e.component,
                "status": e.status,
                "detail": e.detail,
                "created_at": e.created_at.isoformat(),
            }
            for e in recent
        ],
    )
