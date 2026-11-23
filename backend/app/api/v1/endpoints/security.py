from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.schemas.common import MessageResponse
from app.schemas.security import (
    SecurityDashboardResponse,
    UserSecuritySettingsResponse,
    UserSecuritySettingsUpdate,
)
from app.services.security_service import SecurityService

router = APIRouter(prefix="/security", tags=["Security & Privacy Dashboard (Phase 12)"])


@router.get("/dashboard", response_model=SecurityDashboardResponse)
def get_security_dashboard(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, session = auth
    return SecurityService(db).get_dashboard(user.id, session.id)


@router.put("/settings", response_model=UserSecuritySettingsResponse)
def update_security_settings(
    payload: UserSecuritySettingsUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return SecurityService(db).update_settings(user.id, payload)


@router.post("/sessions/{session_id}/revoke", response_model=MessageResponse)
def revoke_session(
    session_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    SecurityService(db).revoke_session(user.id, session_id)
    return MessageResponse(message="Session revoked successfully.")


@router.delete("/personal-data", response_model=MessageResponse)
def delete_personal_data(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    counts = SecurityService(db).delete_personal_data(user.id)
    return MessageResponse(
        message=(
            f"Deleted {counts['memories_deleted']} memory record(s) and "
            f"{counts['conversations_deleted']} conversation session(s)."
        )
    )
