from typing import Optional
from pydantic import BaseModel, Field


class VoiceSettingsResponse(BaseModel):
    language: str
    voice_gender: str
    speech_speed: float
    response_style: str


class VoiceSettingsUpdateRequest(BaseModel):
    language: Optional[str] = Field(default=None, pattern="^(Hinglish|Hindi|English)$")
    voice_gender: Optional[str] = Field(default=None, pattern="^(Male|Female)$")
    speech_speed: Optional[float] = Field(default=None, ge=0.6, le=1.6)
    response_style: Optional[str] = Field(
        default=None, pattern="^(Playful Boss Mode|Concise|Detailed)$"
    )


class AssistantSettingsResponse(BaseModel):
    greeting_message: str
    keep_conversation_history: bool
    privacy_redact_logs: bool
    wake_word_enabled: bool
    wake_phrase: str


class AssistantSettingsUpdateRequest(BaseModel):
    greeting_message: Optional[str] = Field(default=None, min_length=2, max_length=255)
    keep_conversation_history: Optional[bool] = None
    privacy_redact_logs: Optional[bool] = None


class WakeWordSettingsResponse(BaseModel):
    wake_word_enabled: bool
    wake_phrase: str
    processing_mode: str = "on_device_local_only"
    requires_unlock_for_private_actions: bool = True


class WakeWordSettingsUpdateRequest(BaseModel):
    wake_word_enabled: bool
