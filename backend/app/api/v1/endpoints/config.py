from fastapi import APIRouter

from app.core.config import get_settings
from app.schemas.common import SystemConfigResponse

router = APIRouter(tags=["System"])


@router.get("/config", response_model=SystemConfigResponse)
def get_config():
    settings = get_settings()
    return SystemConfigResponse(
        api_version="v1",
        supported_languages=["Hinglish", "Hindi", "English"],
        wake_phrase="Hey Golu",
        email_delivery_configured=settings.email_delivery_configured,
        online_ai_available=settings.openai_configured,
    )
