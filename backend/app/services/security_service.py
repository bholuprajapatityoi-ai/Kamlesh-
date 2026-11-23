from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.models.conversation import ConversationSession
from app.models.memory import MemoryRecord
from app.repositories.security_repository import SecurityRepository
from app.schemas.security import (
    ActiveSessionResponse,
    SecurityDashboardResponse,
    SecurityEventResponse,
    UserSecuritySettingsResponse,
    UserSecuritySettingsUpdate,
)


class SecurityService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.repo = SecurityRepository(db)

    def get_dashboard(self, user_id: str, current_session_id: str) -> SecurityDashboardResponse:
        s = self.repo.get_or_create_settings(user_id)
        sessions = self.repo.list_user_sessions(user_id)
        events = self.repo.list_recent_events(user_id)
        return SecurityDashboardResponse(
            settings=UserSecuritySettingsResponse(
                pin_lock_enabled=s.pin_lock_enabled,
                confirm_sensitive_actions=s.confirm_sensitive_actions,
                microphone_allowed=s.microphone_allowed,
                wake_word_monitoring_allowed=s.wake_word_monitoring_allowed,
                updated_at=s.updated_at.isoformat(),
            ),
            active_sessions=[
                ActiveSessionResponse(
                    id=sess.id,
                    is_current=(sess.id == current_session_id),
                    revoked=sess.revoked,
                    expires_at=sess.expires_at.isoformat(),
                    created_at=sess.created_at.isoformat(),
                )
                for sess in sessions
            ],
            recent_events=[
                SecurityEventResponse(
                    id=e.id,
                    event_type=e.event_type,
                    severity=e.severity,
                    detail=e.detail,
                    ip_address=e.ip_address,
                    created_at=e.created_at.isoformat(),
                )
                for e in events
            ],
        )

    def update_settings(
        self, user_id: str, req: UserSecuritySettingsUpdate
    ) -> UserSecuritySettingsResponse:
        s = self.repo.get_or_create_settings(user_id)
        if req.pin_lock_enabled is not None:
            s.pin_lock_enabled = req.pin_lock_enabled
        if req.confirm_sensitive_actions is not None:
            s.confirm_sensitive_actions = req.confirm_sensitive_actions
        if req.microphone_allowed is not None:
            s.microphone_allowed = req.microphone_allowed
        if req.wake_word_monitoring_allowed is not None:
            s.wake_word_monitoring_allowed = req.wake_word_monitoring_allowed
        s = self.repo.save_settings(s)
        self.repo.log_security_event(
            user_id=user_id,
            event_type="SECURITY_SETTINGS_UPDATED",
            severity="INFO",
            detail="Privacy/security preferences updated",
        )
        return UserSecuritySettingsResponse(
            pin_lock_enabled=s.pin_lock_enabled,
            confirm_sensitive_actions=s.confirm_sensitive_actions,
            microphone_allowed=s.microphone_allowed,
            wake_word_monitoring_allowed=s.wake_word_monitoring_allowed,
            updated_at=s.updated_at.isoformat(),
        )

    def revoke_session(self, user_id: str, target_session_id: str) -> None:
        ok = self.repo.revoke_session_for_user(user_id, target_session_id, reason="DASHBOARD_REVOKED")
        if not ok:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found.")

    def delete_personal_data(self, user_id: str) -> dict:
        deleted_memories = (
            self.db.query(MemoryRecord).filter(MemoryRecord.user_id == user_id).delete()
        )
        deleted_conversations = (
            self.db.query(ConversationSession)
            .filter(ConversationSession.user_id == user_id)
            .delete()
        )
        self.repo.log_security_event(
            user_id=user_id,
            event_type="PERSONAL_DATA_WIPED",
            severity="WARN",
            detail=f"Cleared {deleted_memories} memories and {deleted_conversations} conversation sessions",
        )
        self.db.commit()
        return {
            "memories_deleted": deleted_memories,
            "conversations_deleted": deleted_conversations,
        }
