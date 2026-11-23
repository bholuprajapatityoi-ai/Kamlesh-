from typing import List, Optional
from sqlalchemy.orm import Session

from app.repositories.conversation_repository import ConversationRepository
from app.repositories.user_repository import UserRepository
from app.schemas.ai import ConversationMessageDto, ConversationSessionDto
from app.services.context_manager import context_manager
from app.services.online_ai_service import OnlineAiResult, online_ai_service


class ConversationService:
    async def handle_online_turn_with_usage(
        self,
        context_key: str,
        message: str,
        language: str,
        response_style: str,
    ) -> OnlineAiResult:
        history = context_manager.get_history(context_key)
        result = await online_ai_service.generate_reply_with_usage(
            message=message,
            history=history,
            language=language,
            response_style=response_style,
        )
        context_manager.append_turn(context_key, message, result.reply)
        return result

    async def handle_online_turn(
        self,
        context_key: str,
        message: str,
        language: str,
        response_style: str,
    ) -> str:
        res = await self.handle_online_turn_with_usage(
            context_key=context_key,
            message=message,
            language=language,
            response_style=response_style,
        )
        return res.reply

    def persist_turn_if_permitted(
        self,
        db: Session,
        user_id: str,
        session_id: str,
        user_message: str,
        assistant_reply: str,
        intent: str,
        route: str,
    ) -> None:
        settings_rec = UserRepository(db).get_or_create_assistant_settings(user_id)
        if not settings_rec.keep_conversation_history:
            return
        conv_repo = ConversationRepository(db)
        session = conv_repo.get_or_create_session(
            user_id=user_id,
            session_id=session_id,
            title=user_message[:60] or "GOLU Chat",
        )
        conv_repo.add_message(
            session_id=session.id,
            user_id=user_id,
            role="user",
            content=user_message,
            intent=intent,
            route=route,
        )
        conv_repo.add_message(
            session_id=session.id,
            user_id=user_id,
            role="assistant",
            content=assistant_reply,
            intent=intent,
            route=route,
        )

    def list_user_sessions(self, db: Session, user_id: str) -> List[ConversationSessionDto]:
        sessions = ConversationRepository(db).list_sessions(user_id=user_id)
        return [
            ConversationSessionDto(
                id=s.id,
                title=s.title,
                created_at=s.created_at.isoformat(),
                updated_at=s.updated_at.isoformat(),
                messages=[],
            )
            for s in sessions
        ]

    def get_user_session_detail(
        self, db: Session, user_id: str, session_id: str
    ) -> Optional[ConversationSessionDto]:
        session = ConversationRepository(db).get_owned_session(user_id=user_id, session_id=session_id)
        if session is None:
            return None
        return ConversationSessionDto(
            id=session.id,
            title=session.title,
            created_at=session.created_at.isoformat(),
            updated_at=session.updated_at.isoformat(),
            messages=[
                ConversationMessageDto(
                    id=m.id,
                    role=m.role,
                    content=m.content,
                    intent=m.intent,
                    route=m.route,
                    created_at=m.created_at.isoformat(),
                )
                for m in session.messages
            ],
        )

    def delete_user_session(self, db: Session, user_id: str, session_id: str) -> bool:
        context_manager.clear(f"{user_id}:{session_id}")
        return ConversationRepository(db).delete_owned_session(user_id=user_id, session_id=session_id)

    def clear_context(self, context_key: str) -> None:
        context_manager.clear(context_key)


conversation_service = ConversationService()
