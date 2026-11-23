"""Add assistant_settings, conversation_sessions, and conversation_messages tables.

Revision ID: 0002_assistant_and_conversations
Revises: 0001_initial_schema
Create Date: 2026-11-23
"""

from alembic import op
import sqlalchemy as sa

revision = "0002_assistant_and_conversations"
down_revision = "0001_initial_schema"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column(
        "user_profiles",
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("CURRENT_TIMESTAMP"),
        ),
    )

    op.create_table(
        "assistant_settings",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column(
            "user_id",
            sa.String(length=36),
            sa.ForeignKey("users.id", ondelete="CASCADE"),
            nullable=False,
            unique=True,
        ),
        sa.Column("language", sa.String(length=30), nullable=False, server_default="Hinglish"),
        sa.Column("voice_gender", sa.String(length=20), nullable=False, server_default="Male"),
        sa.Column("speech_speed", sa.Float(), nullable=False, server_default="1.0"),
        sa.Column(
            "response_style",
            sa.String(length=40),
            nullable=False,
            server_default="Playful Boss Mode",
        ),
        sa.Column(
            "greeting_message",
            sa.String(length=255),
            nullable=False,
            server_default="Hello! Boss, GOLU ready hai!",
        ),
        sa.Column(
            "wake_word_enabled", sa.Boolean(), nullable=False, server_default=sa.text("false")
        ),
        sa.Column("wake_phrase", sa.String(length=50), nullable=False, server_default="Hey Golu"),
        sa.Column(
            "keep_conversation_history",
            sa.Boolean(),
            nullable=False,
            server_default=sa.text("true"),
        ),
        sa.Column(
            "privacy_redact_logs", sa.Boolean(), nullable=False, server_default=sa.text("true")
        ),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_assistant_settings_user_id", "assistant_settings", ["user_id"], unique=True)

    op.create_table(
        "conversation_sessions",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column(
            "user_id",
            sa.String(length=36),
            sa.ForeignKey("users.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("title", sa.String(length=160), nullable=False, server_default="GOLU Chat Session"),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_conversation_sessions_user_id", "conversation_sessions", ["user_id"])

    op.create_table(
        "conversation_messages",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column(
            "session_id",
            sa.String(length=36),
            sa.ForeignKey("conversation_sessions.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column(
            "user_id",
            sa.String(length=36),
            sa.ForeignKey("users.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("role", sa.String(length=20), nullable=False),
        sa.Column("content", sa.Text(), nullable=False),
        sa.Column("intent", sa.String(length=60), nullable=False, server_default="GENERAL_CHAT"),
        sa.Column(
            "route", sa.String(length=40), nullable=False, server_default="ONLINE_AI_BACKEND"
        ),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_conversation_messages_session_id", "conversation_messages", ["session_id"])
    op.create_index("ix_conversation_messages_user_id", "conversation_messages", ["user_id"])


def downgrade() -> None:
    op.drop_table("conversation_messages")
    op.drop_table("conversation_sessions")
    op.drop_table("assistant_settings")
    op.drop_column("user_profiles", "updated_at")
