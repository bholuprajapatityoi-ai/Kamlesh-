import uuid
from datetime import datetime, timezone
from sqlalchemy import Boolean, DateTime, ForeignKey, Index, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.base import Base


def _utcnow() -> datetime:
    return datetime.now(timezone.utc)


class FamilyGroup(Base):
    __tablename__ = "family_groups"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    name: Mapped[str] = mapped_column(String(120), nullable=False)
    owner_user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, nullable=False
    )


class FamilyMember(Base):
    __tablename__ = "family_members"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    group_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("family_groups.id", ondelete="CASCADE"), nullable=False, index=True
    )
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    role: Mapped[str] = mapped_column(String(30), default="MEMBER", nullable=False)
    display_label: Mapped[str] = mapped_column(String(80), default="Family Member", nullable=False)
    father_friendly_mode: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    joined_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, nullable=False
    )

    __table_args__ = (
        Index("ix_family_members_group_user", "group_id", "user_id", unique=True),
    )


class FamilyInvitation(Base):
    __tablename__ = "family_invitations"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    group_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("family_groups.id", ondelete="CASCADE"), nullable=False, index=True
    )
    invited_by_user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    invitee_email: Mapped[str] = mapped_column(String(255), nullable=False, index=True)
    invite_code_hash: Mapped[str] = mapped_column(String(128), nullable=False, index=True)
    role: Mapped[str] = mapped_column(String(30), default="FATHER", nullable=False)
    display_label: Mapped[str] = mapped_column(String(80), default="Papa", nullable=False)
    father_friendly_mode: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    status: Mapped[str] = mapped_column(String(24), default="PENDING", nullable=False)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, nullable=False
    )


class SharedReminder(Base):
    __tablename__ = "shared_reminders"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    group_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("family_groups.id", ondelete="CASCADE"), nullable=False, index=True
    )
    created_by_user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    notes: Mapped[str] = mapped_column(Text, default="", nullable=False)
    remind_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    is_completed: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, onupdate=_utcnow, nullable=False
    )


class FamilyPermission(Base):
    __tablename__ = "family_permissions"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    group_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("family_groups.id", ondelete="CASCADE"), nullable=False, index=True
    )
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    share_shared_reminders: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    share_family_routines: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    # Private data stays isolated regardless; explicit opt-in flags only
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, onupdate=_utcnow, nullable=False
    )

    __table_args__ = (
        Index("ix_family_permissions_group_user", "group_id", "user_id", unique=True),
    )
