import uuid
from datetime import datetime, timezone
from sqlalchemy import Boolean, DateTime, ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.base import Base


def _utcnow() -> datetime:
    return datetime.now(timezone.utc)


class SecurityEvent(Base):
    __tablename__ = "security_events"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[str | None] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=True, index=True
    )
    event_type: Mapped[str] = mapped_column(String(60), nullable=False, index=True)
    severity: Mapped[str] = mapped_column(String(20), default="INFO", nullable=False)
    detail: Mapped[str] = mapped_column(String(255), default="", nullable=False)
    ip_address: Mapped[str] = mapped_column(String(64), default="", nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, nullable=False
    )


class LoginAttempt(Base):
    __tablename__ = "login_attempts"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[str | None] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="SET NULL"), nullable=True, index=True
    )
    masked_email: Mapped[str] = mapped_column(String(120), default="", nullable=False)
    success: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    ip_address: Mapped[str] = mapped_column(String(64), default="", nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, nullable=False
    )


class UserSecuritySetting(Base):
    __tablename__ = "user_security_settings"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False, index=True
    )
    pin_lock_enabled: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    confirm_sensitive_actions: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    microphone_allowed: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    wake_word_monitoring_allowed: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, onupdate=_utcnow, nullable=False
    )


class SessionRevocation(Base):
    __tablename__ = "session_revocations"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    session_id: Mapped[str] = mapped_column(String(36), nullable=False, index=True)
    reason: Mapped[str] = mapped_column(String(120), default="USER_REVOKED", nullable=False)
    revoked_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, nullable=False
    )


class AutomationRule(Base):
    __tablename__ = "automation_rules"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    name: Mapped[str] = mapped_column(String(120), nullable=False)
    trigger_type: Mapped[str] = mapped_column(String(40), nullable=False)
    trigger_value: Mapped[str] = mapped_column(String(160), nullable=False)
    is_enabled: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    requires_confirmation: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, onupdate=_utcnow, nullable=False
    )


class AutomationCondition(Base):
    __tablename__ = "automation_conditions"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    rule_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("automation_rules.id", ondelete="CASCADE"), nullable=False, index=True
    )
    condition_type: Mapped[str] = mapped_column(String(40), nullable=False)
    condition_value: Mapped[str] = mapped_column(String(160), nullable=False)


class AutomationAction(Base):
    __tablename__ = "automation_actions"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    rule_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("automation_rules.id", ondelete="CASCADE"), nullable=False, index=True
    )
    action_type: Mapped[str] = mapped_column(String(40), nullable=False)
    action_payload: Mapped[str] = mapped_column(Text, default="", nullable=False)


class AutomationExecutionLog(Base):
    __tablename__ = "automation_execution_logs"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    rule_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("automation_rules.id", ondelete="CASCADE"), nullable=False, index=True
    )
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    status: Mapped[str] = mapped_column(String(30), default="EXECUTED", nullable=False)
    summary: Mapped[str] = mapped_column(String(255), default="", nullable=False)
    executed_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, nullable=False
    )
