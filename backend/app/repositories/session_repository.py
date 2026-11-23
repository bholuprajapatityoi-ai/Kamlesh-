from datetime import datetime
from typing import Optional
from sqlalchemy.orm import Session

from app.models.user_session import UserSession


class SessionRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def create(self, user_id: str, refresh_token_hash: str, expires_at: datetime) -> UserSession:
        session = UserSession(
            user_id=user_id,
            refresh_token_hash=refresh_token_hash,
            expires_at=expires_at,
        )
        self.db.add(session)
        self.db.commit()
        self.db.refresh(session)
        return session

    def get_by_id(self, session_id: str) -> Optional[UserSession]:
        return self.db.query(UserSession).filter(UserSession.id == session_id).first()

    def get_by_refresh_hash(self, token_hash: str) -> Optional[UserSession]:
        return (
            self.db.query(UserSession)
            .filter(UserSession.refresh_token_hash == token_hash)
            .first()
        )

    def revoke(self, session: UserSession) -> None:
        session.revoked = True
        self.db.commit()

    def rotate_refresh_token(
        self, session: UserSession, new_refresh_hash: str, new_expires_at: datetime
    ) -> UserSession:
        session.refresh_token_hash = new_refresh_hash
        session.expires_at = new_expires_at
        self.db.commit()
        self.db.refresh(session)
        return session
