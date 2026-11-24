"""Add GOLU 2.0 Phases 16-20 tables and column extensions safely.

Revision ID: 0005_golu_2_0_phases_16_to_20
Revises: 0004_devices_notifications_and_verification
Create Date: 2026-11-24
"""

from alembic import op
import sqlalchemy as sa

revision = "0005_golu_2_0_phases_16_to_20"
down_revision = "0004_devices_notifications_and_verification"
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Safe additive extensions to existing tables (Phases 4, 7, 8 -> Features 5, 15, 16)
    op.add_column("assistant_settings", sa.Column("humor_level", sa.Integer(), nullable=False, server_default="70"))
    op.add_column("assistant_settings", sa.Column("response_length", sa.String(length=20), nullable=False, server_default="MEDIUM"))
    op.add_column("assistant_settings", sa.Column("avatar_style", sa.String(length=40), nullable=False, server_default="CYBER_BOT"))
    op.add_column("assistant_settings", sa.Column("custom_system_notes", sa.String(length=255), nullable=False, server_default=""))

    op.add_column("memory_records", sa.Column("title", sa.String(length=160), nullable=False, server_default=""))
    op.add_column("memory_records", sa.Column("tags_csv", sa.String(length=255), nullable=False, server_default=""))

    op.add_column("app_preferences", sa.Column("launch_count", sa.Integer(), nullable=False, server_default="0"))
    op.add_column("app_preferences", sa.Column("shortcut_command", sa.String(length=120), nullable=False, server_default=""))
    op.add_column("app_preferences", sa.Column("last_launched_at", sa.DateTime(timezone=True), nullable=True))

    op.create_table(
        "knowledge_documents",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=200), nullable=False),
        sa.Column("mime_type", sa.String(length=80), nullable=False, server_default="text/plain"),
        sa.Column("file_size_bytes", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("extracted_text", sa.Text(), nullable=False, server_default=""),
        sa.Column("summary", sa.Text(), nullable=False, server_default=""),
        sa.Column("key_points_json", sa.Text(), nullable=False, server_default="[]"),
        sa.Column("consent_granted", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_knowledge_documents_user_id", "knowledge_documents", ["user_id"])

    op.create_table(
        "notification_insights",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("source_app", sa.String(length=120), nullable=False, server_default="Android"),
        sa.Column("title", sa.String(length=200), nullable=False),
        sa.Column("body_preview", sa.Text(), nullable=False, server_default=""),
        sa.Column("category", sa.String(length=40), nullable=False, server_default="GENERAL"),
        sa.Column("priority_score", sa.Integer(), nullable=False, server_default="1"),
        sa.Column("is_important", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("ai_summary", sa.Text(), nullable=False, server_default=""),
        sa.Column("converted_reminder_id", sa.String(length=36), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_notification_insights_user_id", "notification_insights", ["user_id"])

    op.create_table(
        "translation_history",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("source_language", sa.String(length=40), nullable=False, server_default="auto"),
        sa.Column("target_language", sa.String(length=40), nullable=False, server_default="Hindi"),
        sa.Column("source_text", sa.Text(), nullable=False),
        sa.Column("translated_text", sa.Text(), nullable=False),
        sa.Column("mode", sa.String(length=30), nullable=False, server_default="TEXT"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_translation_history_user_id", "translation_history", ["user_id"])

    op.create_table(
        "research_reports",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("query", sa.String(length=300), nullable=False),
        sa.Column("summary", sa.Text(), nullable=False, server_default=""),
        sa.Column("verified_findings", sa.Text(), nullable=False, server_default=""),
        sa.Column("uncertain_points", sa.Text(), nullable=False, server_default=""),
        sa.Column("citations_json", sa.Text(), nullable=False, server_default="[]"),
        sa.Column("provider_used", sa.String(length=60), nullable=False, server_default="public-search"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_research_reports_user_id", "research_reports", ["user_id"])

    op.create_table(
        "coding_history",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("language", sa.String(length=40), nullable=False, server_default="kotlin"),
        sa.Column("task_type", sa.String(length=40), nullable=False, server_default="EXPLAIN"),
        sa.Column("prompt", sa.Text(), nullable=False),
        sa.Column("code_output", sa.Text(), nullable=False, server_default=""),
        sa.Column("explanation", sa.Text(), nullable=False, server_default=""),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_coding_history_user_id", "coding_history", ["user_id"])

    op.create_table(
        "creative_assets",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("asset_type", sa.String(length=30), nullable=False, server_default="IMAGE"),
        sa.Column("prompt", sa.Text(), nullable=False),
        sa.Column("revised_prompt", sa.Text(), nullable=False, server_default=""),
        sa.Column("image_url", sa.Text(), nullable=False, server_default=""),
        sa.Column("design_notes", sa.Text(), nullable=False, server_default=""),
        sa.Column("status", sa.String(length=30), nullable=False, server_default="FAILED"),
        sa.Column("error_detail", sa.String(length=255), nullable=False, server_default=""),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_creative_assets_user_id", "creative_assets", ["user_id"])

    op.create_table(
        "voice_recordings",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=160), nullable=False),
        sa.Column("duration_seconds", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("transcript", sa.Text(), nullable=False, server_default=""),
        sa.Column("summary", sa.Text(), nullable=False, server_default=""),
        sa.Column("lecture_notes", sa.Text(), nullable=False, server_default=""),
        sa.Column("local_file_hint", sa.String(length=255), nullable=False, server_default=""),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_voice_recordings_user_id", "voice_recordings", ["user_id"])

    op.create_table(
        "smart_home_configs",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False, unique=True),
        sa.Column("provider_type", sa.String(length=40), nullable=False, server_default="HOME_ASSISTANT"),
        sa.Column("hub_url", sa.String(length=255), nullable=False, server_default=""),
        sa.Column("token_hint", sa.String(length=64), nullable=False, server_default=""),
        sa.Column("is_authorized", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_smart_home_configs_user_id", "smart_home_configs", ["user_id"], unique=True)

    op.create_table(
        "smart_home_devices",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("external_entity_id", sa.String(length=120), nullable=False),
        sa.Column("name", sa.String(length=120), nullable=False),
        sa.Column("room", sa.String(length=80), nullable=False, server_default="Living Room"),
        sa.Column("device_type", sa.String(length=40), nullable=False, server_default="LIGHT"),
        sa.Column("is_on", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("brightness_pct", sa.Integer(), nullable=False, server_default="100"),
        sa.Column("status", sa.String(length=40), nullable=False, server_default="UNVERIFIED"),
        sa.Column("last_synced_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_smart_home_devices_user_id", "smart_home_devices", ["user_id"])

    op.create_table(
        "expense_budgets",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("month_key", sa.String(length=7), nullable=False),
        sa.Column("limit_amount", sa.Float(), nullable=False, server_default="0"),
        sa.Column("currency", sa.String(length=10), nullable=False, server_default="INR"),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_expense_budgets_user_id", "expense_budgets", ["user_id"])
    op.create_index("ix_expense_budgets_user_month", "expense_budgets", ["user_id", "month_key"], unique=True)

    op.create_table(
        "expense_records",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=160), nullable=False),
        sa.Column("category", sa.String(length=50), nullable=False, server_default="General"),
        sa.Column("amount", sa.Float(), nullable=False),
        sa.Column("currency", sa.String(length=10), nullable=False, server_default="INR"),
        sa.Column("notes", sa.String(length=255), nullable=False, server_default=""),
        sa.Column("spent_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_expense_records_user_id", "expense_records", ["user_id"])

    op.create_table(
        "study_sessions",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("subject", sa.String(length=100), nullable=False),
        sa.Column("topic", sa.String(length=180), nullable=False),
        sa.Column("language", sa.String(length=30), nullable=False, server_default="English"),
        sa.Column("difficulty", sa.String(length=30), nullable=False, server_default="Intermediate"),
        sa.Column("explanation", sa.Text(), nullable=False, server_default=""),
        sa.Column("practice_questions_json", sa.Text(), nullable=False, server_default="[]"),
        sa.Column("mastery_pct", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_study_sessions_user_id", "study_sessions", ["user_id"])

    op.create_table(
        "travel_plans",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("origin", sa.String(length=160), nullable=False, server_default=""),
        sa.Column("destination", sa.String(length=160), nullable=False),
        sa.Column("route_summary", sa.Text(), nullable=False, server_default=""),
        sa.Column("nearby_places_json", sa.Text(), nullable=False, server_default="[]"),
        sa.Column("itinerary_notes", sa.Text(), nullable=False, server_default=""),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_travel_plans_user_id", "travel_plans", ["user_id"])

    op.create_table(
        "sync_checkpoints",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("device_identifier", sa.String(length=120), nullable=False),
        sa.Column("scope", sa.String(length=40), nullable=False, server_default="ALL"),
        sa.Column("conflict_policy", sa.String(length=30), nullable=False, server_default="SERVER_WINS"),
        sa.Column("synced_items_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("synced_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_sync_checkpoints_user_id", "sync_checkpoints", ["user_id"])

    op.create_table(
        "diagnostic_events",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=True),
        sa.Column("component", sa.String(length=60), nullable=False),
        sa.Column("status", sa.String(length=30), nullable=False),
        sa.Column("detail", sa.String(length=255), nullable=False, server_default=""),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_diagnostic_events_user_id", "diagnostic_events", ["user_id"])


def downgrade() -> None:
    for table in [
        "diagnostic_events",
        "sync_checkpoints",
        "travel_plans",
        "study_sessions",
        "expense_records",
        "expense_budgets",
        "smart_home_devices",
        "smart_home_configs",
        "voice_recordings",
        "creative_assets",
        "coding_history",
        "research_reports",
        "translation_history",
        "notification_insights",
        "knowledge_documents",
    ]:
        op.drop_table(table)

    op.drop_column("app_preferences", "last_launched_at")
    op.drop_column("app_preferences", "shortcut_command")
    op.drop_column("app_preferences", "launch_count")
    op.drop_column("memory_records", "tags_csv")
    op.drop_column("memory_records", "title")
    op.drop_column("assistant_settings", "custom_system_notes")
    op.drop_column("assistant_settings", "avatar_style")
    op.drop_column("assistant_settings", "response_length")
    op.drop_column("assistant_settings", "humor_level")
