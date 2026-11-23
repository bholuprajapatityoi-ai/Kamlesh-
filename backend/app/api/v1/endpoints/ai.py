from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.middleware.rate_limit import rate_limiter
from app.schemas.ai import (
    AiChatRequest,
    AiChatResponse,
    AiIntentRequest,
    AiIntentResponse,
    AiStatusResponse,
    ClearContextRequest,
    ConversationSessionDto,
)
from app.schemas.common import MessageResponse
from app.services.ai_service import ai_service
from app.services.conversation_service import conversation_service
from app.services.intent_classifier import intent_classifier

router = APIRouter(prefix="/ai", tags=["Hybrid AI & Conversations"])
conversations_router = APIRouter(prefix="/conversations", tags=["Conversations"])


@router.post("/chat", response_model=AiChatResponse)
async def ai_chat(
    payload: AiChatRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rate_limiter.check(f"ai:{user.id}", max_requests=get_settings().AI_RATE_LIMIT_PER_MINUTE)
    return await ai_service.process_chat(db=db, user_id=user.id, req=payload)


@router.post("/intent", response_model=AiIntentResponse)
def ai_intent(payload: AiIntentRequest, auth=Depends(get_current_user_and_session)):
    intent, route, confidence = intent_classifier.classify(payload.text)
    return AiIntentResponse(intent=intent, recommended_route=route, confidence=confidence)


@router.get("/status", response_model=AiStatusResponse)
def ai_status():
    settings = get_settings()
    return AiStatusResponse(
        online_ai_configured=settings.openai_configured,
        provider=settings.AI_PROVIDER,
        model=settings.OPENAI_MODEL,
        offline_engine_ready=True,
    )


@router.post("/context/clear", response_model=MessageResponse)
def clear_context(payload: ClearContextRequest, auth=Depends(get_current_user_and_session)):
    user, _ = auth
    session_id = payload.session_id or "default"
    conversation_service.clear_context(f"{user.id}:{session_id}")
    return MessageResponse(message="Temporary AI conversation context cleared.")


@router.get("/conversations", response_model=List[ConversationSessionDto])
def list_conversations(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return conversation_service.list_user_sessions(db=db, user_id=user.id)


@router.get("/conversations/{session_id}", response_model=ConversationSessionDto)
def get_conversation(
    session_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    session = conversation_service.get_user_session_detail(
        db=db, user_id=user.id, session_id=session_id
    )
    if session is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Conversation session not found.",
        )
    return session


@router.delete("/conversations/{session_id}", response_model=MessageResponse)
def delete_conversation(
    session_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    deleted = conversation_service.delete_user_session(
        db=db, user_id=user.id, session_id=session_id
    )
    if not deleted:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Conversation session not found.",
        )
    return MessageResponse(message="Conversation session deleted.")


conversations_router.add_api_route("", list_conversations, methods=["GET"], response_model=List[ConversationSessionDto])
conversations_router.add_api_route("/{session_id}", get_conversation, methods=["GET"], response_model=ConversationSessionDto)
conversations_router.add_api_route("/{session_id}", delete_conversation, methods=["DELETE"], response_model=MessageResponse)
