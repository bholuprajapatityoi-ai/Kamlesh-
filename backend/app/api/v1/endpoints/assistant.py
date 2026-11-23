from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.schemas.voice import (
    AssistantSettingsResponse,
    AssistantSettingsUpdateRequest,
    WakeWordSettingsResponse,
    WakeWordSettingsUpdateRequest,
)
from app.services.voice_settings_service import VoiceSettingsService

router = APIRouter(prefix="/assistant", tags=["Assistant & Wake-Word"])


@router.get("/settings", response_model=AssistantSettingsResponse)
def get_assistant_settings(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return VoiceSettingsService(db).get_assistant_settings(user.id)


@router.put("/settings", response_model=AssistantSettingsResponse)
def update_assistant_settings(
    payload: AssistantSettingsUpdateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return VoiceSettingsService(db).update_assistant_settings(user.id, payload)


@router.get("/wake-word/settings", response_model=WakeWordSettingsResponse)
def get_wake_word_settings(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return VoiceSettingsService(db).get_wake_word_settings(user.id)


@router.put("/wake-word/settings", response_model=WakeWordSettingsResponse)
def update_wake_word_settings(
    payload: WakeWordSettingsUpdateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return VoiceSettingsService(db).update_wake_word_settings(user.id, payload)
