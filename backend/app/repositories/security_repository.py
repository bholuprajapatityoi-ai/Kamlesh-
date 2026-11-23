from typing import List, Optional
from sqlalchemy.orm import Session

from app.models.security_automation import (
    LoginAttempt,
    SecurityEvent,
    SessionRevocation,
    UserSecuritySetting,
)
from app.models.user_session import UserSession


def _mask_email(email: str) -> str:
    parts = email.strip().lower().split("@")
    if len(parts) != 2 or not parts[0]:
        return "unknown"
    name = parts[0]
    prefix = name[:2] if len(name) > 2 else name[:1]
    return f"{prefix}***@{parts[1]}"


class SecurityRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_or_create_settings(self, user_id: str) -> UserSecuritySetting:
        rec = (
            self.db.query(UserSecuritySetting)
            .filter(UserSecuritySetting.user_id == user_id)
            .first()
        )
        if rec is None:
            rec = UserSecuritySetting(user_id=user_id)
            self.db.add(rec)
            self.db.commit()
            self.db.refresh(rec)
        return rec

    def save_settings(self, rec: UserSecuritySetting) -> UserSecuritySetting:
        self.db.commit()
        self.db.refresh(rec)
        return rec

    def log_security_event(
        self,
        user_id: Optional[str],
        event_type: str,
        severity: str = "INFO",
        detail: str = "",
        ip_address: str = "",
    ) -> SecurityEvent:
        ev = SecurityEvent(
            user_id=user_id,
            event_type=event_type,
            severity=severity,
            detail=detail[:255],
            ip_address=ip_address[:64],
        )
        self.db.add(ev)
        self.db.commit()
        return ev

    def log_login_attempt(
        self, email: str, user_id: Optional[str], success: bool, ip_address: str = ""
    ) -> None:
        attempt = LoginAttempt(
            user_id=user_id,
            masked_email=_mask_email(email),
            success=success,
            ip_address=ip_address[:64],
        )
        self.db.add(attempt)
        self.db.commit()

    def list_user_sessions(self, user_id: str) -> List[UserSession]:
        return (
            self.db.query(UserSession)
            .filter(UserSession.user_id == user_id)
            .order_by(UserSession.created_at.desc())
            .all()
        )

    def revoke_session_for_user(
        self, user_id: str, session_id: str, reason: str = "USER_REVOKED"
    ) -> bool:
        session = (
            self.db.query(UserSession)
            .filter(UserSession.id == session_id, UserSession.user_id == user_id)
            .first()
        )
        if not session:
            return False
        session.revoked = True
        self.db.add(SessionRevocation(user_id=user_id, session_id=session_id, reason=reason))
        self.db.add(
            SecurityEvent(
                user_id=user_id,
                event_type="SESSION_REVOKED",
                severity="INFO",
                detail=f"Session revoked ({reason})",
            )
        )
        self.db.commit()
        return True

    def list_recent_events(self, user_id: str, limit: int = 30) -> List[SecurityEvent]:
        return (
            self.db.query(SecurityEvent)
            .filter(SecurityEvent.user_id == user_id)
            .order_by(SecurityEvent.created_at.desc())
            .limit(limit)
            .all()
        )
