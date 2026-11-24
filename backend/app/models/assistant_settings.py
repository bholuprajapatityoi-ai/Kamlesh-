import uuid
from datetime import datetime
from sqlalchemy import Boolean, DateTime, Float, ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base
from app.models.user import utcnow


class AssistantSettings(Base):
    __tablename__ = "assistant_settings"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), unique=True, index=True, nullable=False
    )
    # Phase 4 Voice Preferences
    language: Mapped[str] = mapped_column(String(30), default="Hinglish", nullable=False)
    voice_gender: Mapped[str] = mapped_column(String(20), default="Male", nullable=False)
    speech_speed: Mapped[float] = mapped_column(Float, default=1.0, nullable=False)
    response_style: Mapped[str] = mapped_column(String(40), default="Playful Boss Mode", nullable=False)

    # Phase 2/5 Assistant & Wake-Word Preferences (disabled by default for privacy)
    greeting_message: Mapped[str] = mapped_column(
        String(255), default="Hello! Boss, GOLU ready hai!", nullable=False
    )
    wake_word_enabled: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    wake_phrase: Mapped[str] = mapped_column(String(50), default="Hey Golu", nullable=False)

    # Phase 6 Conversation Retention & Privacy
    keep_conversation_history: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    privacy_redact_logs: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    # Phase 19 Feature 15: Custom AI Personality
    humor_level: Mapped[int] = mapped_column(default=70, nullable=False)
    response_length: Mapped[str] = mapped_column(String(20), default="MEDIUM", nullable=False)
    avatar_style: Mapped[str] = mapped_column(String(40), default="CYBER_BOT", nullable=False)
    custom_system_notes: Mapped[str] = mapped_column(String(255), default="", nullable=False)

    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=utcnow, onupdate=utcnow, nullable=False
    )

    user = relationship("User", back_populates="assistant_settings")
