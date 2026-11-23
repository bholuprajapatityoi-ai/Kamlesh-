"""Add device registrations, notification records, and email verification tables.

Revision ID: 0004_devices_notifications_and_verification
Revises: 0003_phases_7_to_13
Create Date: 2026-11-23
"""

from alembic import op
import sqlalchemy as sa

revision = "0004_devices_notifications_and_verification"
down_revision = "0003_phases_7_to_13"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("users", sa.Column("email_verified_at", sa.DateTime(timezone=True), nullable=True))

    op.create_table(
        "email_verification_tokens",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("token_hash", sa.String(length=128), nullable=False),
        sa.Column("used", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_email_verification_tokens_user_id", "email_verification_tokens", ["user_id"])
    op.create_index("ix_email_verification_tokens_token_hash", "email_verification_tokens", ["token_hash"], unique=True)

    op.create_table(
        "device_registrations",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("device_identifier", sa.String(length=120), nullable=False),
        sa.Column("device_name", sa.String(length=120), nullable=False, server_default="Android Device"),
        sa.Column("platform", sa.String(length=30), nullable=False, server_default="android"),
        sa.Column("os_version", sa.String(length=40), nullable=False, server_default=""),
        sa.Column("app_version", sa.String(length=40), nullable=False, server_default="1.1.0"),
        sa.Column("push_token_masked", sa.String(length=64), nullable=False, server_default=""),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("last_seen_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_device_registrations_user_id", "device_registrations", ["user_id"])
    op.create_index(
        "ix_device_registrations_user_identifier",
        "device_registrations",
        ["user_id", "device_identifier"],
        unique=True,
    )

    op.create_table(
        "notification_records",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=200), nullable=False),
        sa.Column("body", sa.Text(), nullable=False, server_default=""),
        sa.Column("category", sa.String(length=40), nullable=False, server_default="REMINDER"),
        sa.Column("channel", sa.String(length=40), nullable=False, server_default="IN_APP"),
        sa.Column("status", sa.String(length=24), nullable=False, server_default="PENDING"),
        sa.Column("retry_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("last_error", sa.String(length=255), nullable=False, server_default=""),
        sa.Column("scheduled_for", sa.DateTime(timezone=True), nullable=True),
        sa.Column("delivered_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_notification_records_user_id", "notification_records", ["user_id"])
    op.create_index("ix_notification_records_status", "notification_records", ["status"])


def downgrade() -> None:
    op.drop_table("notification_records")
    op.drop_table("device_registrations")
    op.drop_table("email_verification_tokens")
    op.drop_column("users", "email_verified_at")
