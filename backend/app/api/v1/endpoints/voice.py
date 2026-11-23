from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.schemas.voice import VoiceSettingsResponse, VoiceSettingsUpdateRequest
from app.services.voice_settings_service import VoiceSettingsService

router = APIRouter(prefix="/voice", tags=["Voice Preferences"])


@router.get("/settings", response_model=VoiceSettingsResponse)
def get_voice_settings(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return VoiceSettingsService(db).get_voice_settings(user.id)


@router.put("/settings", response_model=VoiceSettingsResponse)
def update_voice_settings(
    payload: VoiceSettingsUpdateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return VoiceSettingsService(db).update_voice_settings(user.id, payload)
