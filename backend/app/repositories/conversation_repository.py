from typing import List, Optional
from sqlalchemy.orm import Session

from app.models.conversation import ConversationSession
from app.models.conversation_message import ConversationMessage


class ConversationRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_or_create_session(
        self, user_id: str, session_id: Optional[str] = None, title: str = "GOLU Chat"
    ) -> ConversationSession:
        if session_id:
            existing = (
                self.db.query(ConversationSession)
                .filter(
                    ConversationSession.id == session_id,
                    ConversationSession.user_id == user_id,
                )
                .first()
            )
            if existing is not None:
                return existing

        rec = ConversationSession(
            id=session_id if session_id else None,
            user_id=user_id,
            title=title[:160],
        )
        self.db.add(rec)
        self.db.commit()
        self.db.refresh(rec)
        return rec

    def add_message(
        self,
        session_id: str,
        user_id: str,
        role: str,
        content: str,
        intent: str,
        route: str,
    ) -> ConversationMessage:
        msg = ConversationMessage(
            session_id=session_id,
            user_id=user_id,
            role=role,
            content=content,
            intent=intent,
            route=route,
        )
        self.db.add(msg)
        self.db.commit()
        self.db.refresh(msg)
        return msg

    def list_sessions(self, user_id: str, limit: int = 20) -> List[ConversationSession]:
        return (
            self.db.query(ConversationSession)
            .filter(ConversationSession.user_id == user_id)
            .order_by(ConversationSession.updated_at.desc())
            .limit(limit)
            .all()
        )

    def get_owned_session(self, user_id: str, session_id: str) -> Optional[ConversationSession]:
        return (
            self.db.query(ConversationSession)
            .filter(
                ConversationSession.id == session_id,
                ConversationSession.user_id == user_id,
            )
            .first()
        )

    def delete_owned_session(self, user_id: str, session_id: str) -> bool:
        session = self.get_owned_session(user_id, session_id)
        if session is None:
            return False
        self.db.delete(session)
        self.db.commit()
        return True
