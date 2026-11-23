from sqlalchemy.orm import Session

from app.repositories.user_repository import UserRepository
from app.schemas.voice import (
    AssistantSettingsResponse,
    AssistantSettingsUpdateRequest,
    VoiceSettingsResponse,
    VoiceSettingsUpdateRequest,
    WakeWordSettingsResponse,
    WakeWordSettingsUpdateRequest,
)


class VoiceSettingsService:
    def __init__(self, db: Session) -> None:
        self.repo = UserRepository(db)

    def get_voice_settings(self, user_id: str) -> VoiceSettingsResponse:
        rec = self.repo.get_or_create_assistant_settings(user_id)
        return VoiceSettingsResponse(
            language=rec.language,
            voice_gender=rec.voice_gender,
            speech_speed=rec.speech_speed,
            response_style=rec.response_style,
        )

    def update_voice_settings(
        self, user_id: str, payload: VoiceSettingsUpdateRequest
    ) -> VoiceSettingsResponse:
        rec = self.repo.get_or_create_assistant_settings(user_id)
        if payload.language is not None:
            rec.language = payload.language
        if payload.voice_gender is not None:
            rec.voice_gender = payload.voice_gender
        if payload.speech_speed is not None:
            rec.speech_speed = payload.speech_speed
        if payload.response_style is not None:
            rec.response_style = payload.response_style
        saved = self.repo.save_assistant_settings(rec)
        return VoiceSettingsResponse(
            language=saved.language,
            voice_gender=saved.voice_gender,
            speech_speed=saved.speech_speed,
            response_style=saved.response_style,
        )

    def get_assistant_settings(self, user_id: str) -> AssistantSettingsResponse:
        rec = self.repo.get_or_create_assistant_settings(user_id)
        return AssistantSettingsResponse(
            greeting_message=rec.greeting_message,
            keep_conversation_history=rec.keep_conversation_history,
            privacy_redact_logs=rec.privacy_redact_logs,
            wake_word_enabled=rec.wake_word_enabled,
            wake_phrase=rec.wake_phrase,
        )

    def update_assistant_settings(
        self, user_id: str, payload: AssistantSettingsUpdateRequest
    ) -> AssistantSettingsResponse:
        rec = self.repo.get_or_create_assistant_settings(user_id)
        if payload.greeting_message is not None:
            rec.greeting_message = payload.greeting_message.strip()
        if payload.keep_conversation_history is not None:
            rec.keep_conversation_history = payload.keep_conversation_history
        if payload.privacy_redact_logs is not None:
            rec.privacy_redact_logs = payload.privacy_redact_logs
        saved = self.repo.save_assistant_settings(rec)
        return AssistantSettingsResponse(
            greeting_message=saved.greeting_message,
            keep_conversation_history=saved.keep_conversation_history,
            privacy_redact_logs=saved.privacy_redact_logs,
            wake_word_enabled=saved.wake_word_enabled,
            wake_phrase=saved.wake_phrase,
        )

    def get_wake_word_settings(self, user_id: str) -> WakeWordSettingsResponse:
        rec = self.repo.get_or_create_assistant_settings(user_id)
        return WakeWordSettingsResponse(
            wake_word_enabled=rec.wake_word_enabled,
            wake_phrase=rec.wake_phrase,
        )

    def update_wake_word_settings(
        self, user_id: str, payload: WakeWordSettingsUpdateRequest
    ) -> WakeWordSettingsResponse:
        rec = self.repo.get_or_create_assistant_settings(user_id)
        rec.wake_word_enabled = payload.wake_word_enabled
        saved = self.repo.save_assistant_settings(rec)
        return WakeWordSettingsResponse(
            wake_word_enabled=saved.wake_word_enabled,
            wake_phrase=saved.wake_phrase,
        )
