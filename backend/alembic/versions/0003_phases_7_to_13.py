"""Add Phase 7 to Phase 13 tables safely without dropping existing data.

Revision ID: 0003_phases_7_to_13
Revises: 0002_assistant_and_conversations
Create Date: 2026-11-23
"""

from alembic import op
import sqlalchemy as sa

revision = "0003_phases_7_to_13"
down_revision = "0002_assistant_and_conversations"
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Phase 7: Smart Personal Memory
    op.create_table(
        "memory_categories",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=True),
        sa.Column("name", sa.String(length=60), nullable=False),
        sa.Column("description", sa.String(length=200), nullable=False, server_default=""),
        sa.Column("is_system", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_memory_categories_user_id", "memory_categories", ["user_id"])

    op.create_table(
        "memory_records",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("category", sa.String(length=60), nullable=False, server_default="Custom Notes"),
        sa.Column("content", sa.Text(), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("family_shared", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_memory_records_user_id", "memory_records", ["user_id"])
    op.create_index("ix_memory_records_user_category", "memory_records", ["user_id", "category"])

    op.create_table(
        "memory_audit_events",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("memory_id", sa.String(length=36), nullable=True),
        sa.Column("action", sa.String(length=40), nullable=False),
        sa.Column("category", sa.String(length=60), nullable=False, server_default=""),
        sa.Column("detail", sa.String(length=255), nullable=False, server_default=""),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_memory_audit_events_user_id", "memory_audit_events", ["user_id"])
    op.create_index("ix_memory_audit_events_memory_id", "memory_audit_events", ["memory_id"])

    # Phase 8 & 9: Phone Control & Messaging Preferences
    op.create_table(
        "phone_action_preferences",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False, unique=True),
        sa.Column("confirm_before_call", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("allow_volume_control", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("allow_media_control", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("allow_contact_lookup", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_phone_action_preferences_user_id", "phone_action_preferences", ["user_id"], unique=True)

    op.create_table(
        "app_preferences",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("alias_name", sa.String(length=80), nullable=False),
        sa.Column("package_name", sa.String(length=180), nullable=False),
        sa.Column("is_favourite", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_app_preferences_user_id", "app_preferences", ["user_id"])

    op.create_table(
        "contact_aliases",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("nickname", sa.String(length=60), nullable=False),
        sa.Column("contact_name", sa.String(length=120), nullable=False),
        sa.Column("phone_hint", sa.String(length=40), nullable=False, server_default=""),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_contact_aliases_user_id", "contact_aliases", ["user_id"])
    op.create_index("ix_contact_aliases_user_nickname", "contact_aliases", ["user_id", "nickname"])

    op.create_table(
        "messaging_preferences",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False, unique=True),
        sa.Column("default_language", sa.String(length=30), nullable=False, server_default="Hinglish"),
        sa.Column("always_confirm_before_open", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("signature_text", sa.String(length=120), nullable=False, server_default=""),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_messaging_preferences_user_id", "messaging_preferences", ["user_id"], unique=True)

    op.create_table(
        "messaging_templates",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=100), nullable=False),
        sa.Column("language", sa.String(length=30), nullable=False, server_default="Hinglish"),
        sa.Column("body", sa.Text(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_messaging_templates_user_id", "messaging_templates", ["user_id"])

    # Phase 10: Daily Life Assistant
    op.create_table(
        "reminders",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=200), nullable=False),
        sa.Column("notes", sa.Text(), nullable=False, server_default=""),
        sa.Column("remind_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("timezone_name", sa.String(length=64), nullable=False, server_default="Asia/Kolkata"),
        sa.Column("recurrence", sa.String(length=30), nullable=False, server_default="NONE"),
        sa.Column("is_completed", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("notify_enabled", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_reminders_user_id", "reminders", ["user_id"])
    op.create_index("ix_reminders_remind_at", "reminders", ["remind_at"])

    op.create_table(
        "tasks",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=200), nullable=False),
        sa.Column("priority", sa.String(length=20), nullable=False, server_default="MEDIUM"),
        sa.Column("due_date", sa.DateTime(timezone=True), nullable=True),
        sa.Column("is_completed", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_tasks_user_id", "tasks", ["user_id"])

    op.create_table(
        "timetable_entries",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("day_of_week", sa.String(length=20), nullable=False),
        sa.Column("start_time", sa.String(length=16), nullable=False),
        sa.Column("end_time", sa.String(length=16), nullable=False),
        sa.Column("subject_or_activity", sa.String(length=160), nullable=False),
        sa.Column("location", sa.String(length=120), nullable=False, server_default=""),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_timetable_entries_user_id", "timetable_entries", ["user_id"])
    op.create_index("ix_timetable_entries_day_of_week", "timetable_entries", ["day_of_week"])

    op.create_table(
        "calendar_events",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=200), nullable=False),
        sa.Column("description", sa.Text(), nullable=False, server_default=""),
        sa.Column("start_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("end_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("timezone_name", sa.String(length=64), nullable=False, server_default="Asia/Kolkata"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_calendar_events_user_id", "calendar_events", ["user_id"])
    op.create_index("ix_calendar_events_start_at", "calendar_events", ["start_at"])

    op.create_table(
        "daily_routines",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("name", sa.String(length=120), nullable=False),
        sa.Column("trigger_time", sa.String(length=16), nullable=False),
        sa.Column("summary_prompt", sa.String(length=255), nullable=False, server_default=""),
        sa.Column("is_enabled", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_daily_routines_user_id", "daily_routines", ["user_id"])

    op.create_table(
        "notification_preferences",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False, unique=True),
        sa.Column("morning_greeting_enabled", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("evening_summary_enabled", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("reminder_notifications_enabled", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("battery_alert_threshold", sa.Integer(), nullable=False, server_default="20"),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_notification_preferences_user_id", "notification_preferences", ["user_id"], unique=True)

    # Phase 11: Family Mode
    op.create_table(
        "family_groups",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("name", sa.String(length=120), nullable=False),
        sa.Column("owner_user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_family_groups_owner_user_id", "family_groups", ["owner_user_id"])

    op.create_table(
        "family_members",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("group_id", sa.String(length=36), sa.ForeignKey("family_groups.id", ondelete="CASCADE"), nullable=False),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("role", sa.String(length=30), nullable=False, server_default="MEMBER"),
        sa.Column("display_label", sa.String(length=80), nullable=False, server_default="Family Member"),
        sa.Column("father_friendly_mode", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("joined_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_family_members_group_id", "family_members", ["group_id"])
    op.create_index("ix_family_members_user_id", "family_members", ["user_id"])
    op.create_index("ix_family_members_group_user", "family_members", ["group_id", "user_id"], unique=True)

    op.create_table(
        "family_invitations",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("group_id", sa.String(length=36), sa.ForeignKey("family_groups.id", ondelete="CASCADE"), nullable=False),
        sa.Column("invited_by_user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("invitee_email", sa.String(length=255), nullable=False),
        sa.Column("invite_code_hash", sa.String(length=128), nullable=False),
        sa.Column("role", sa.String(length=30), nullable=False, server_default="FATHER"),
        sa.Column("display_label", sa.String(length=80), nullable=False, server_default="Papa"),
        sa.Column("father_friendly_mode", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("status", sa.String(length=24), nullable=False, server_default="PENDING"),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_family_invitations_group_id", "family_invitations", ["group_id"])
    op.create_index("ix_family_invitations_invitee_email", "family_invitations", ["invitee_email"])
    op.create_index("ix_family_invitations_invite_code_hash", "family_invitations", ["invite_code_hash"])

    op.create_table(
        "shared_reminders",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("group_id", sa.String(length=36), sa.ForeignKey("family_groups.id", ondelete="CASCADE"), nullable=False),
        sa.Column("created_by_user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=200), nullable=False),
        sa.Column("notes", sa.Text(), nullable=False, server_default=""),
        sa.Column("remind_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("is_completed", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_shared_reminders_group_id", "shared_reminders", ["group_id"])

    op.create_table(
        "family_permissions",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("group_id", sa.String(length=36), sa.ForeignKey("family_groups.id", ondelete="CASCADE"), nullable=False),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("share_shared_reminders", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("share_family_routines", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_family_permissions_group_id", "family_permissions", ["group_id"])
    op.create_index("ix_family_permissions_user_id", "family_permissions", ["user_id"])
    op.create_index("ix_family_permissions_group_user", "family_permissions", ["group_id", "user_id"], unique=True)

    # Phase 12 & 13: Security & Smart Automation
    op.create_table(
        "security_events",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=True),
        sa.Column("event_type", sa.String(length=60), nullable=False),
        sa.Column("severity", sa.String(length=20), nullable=False, server_default="INFO"),
        sa.Column("detail", sa.String(length=255), nullable=False, server_default=""),
        sa.Column("ip_address", sa.String(length=64), nullable=False, server_default=""),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_security_events_user_id", "security_events", ["user_id"])
    op.create_index("ix_security_events_event_type", "security_events", ["event_type"])

    op.create_table(
        "login_attempts",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="SET NULL"), nullable=True),
        sa.Column("masked_email", sa.String(length=120), nullable=False, server_default=""),
        sa.Column("success", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("ip_address", sa.String(length=64), nullable=False, server_default=""),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_login_attempts_user_id", "login_attempts", ["user_id"])

    op.create_table(
        "user_security_settings",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False, unique=True),
        sa.Column("pin_lock_enabled", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("confirm_sensitive_actions", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("microphone_allowed", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("wake_word_monitoring_allowed", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_user_security_settings_user_id", "user_security_settings", ["user_id"], unique=True)

    op.create_table(
        "session_revocations",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("session_id", sa.String(length=36), nullable=False),
        sa.Column("reason", sa.String(length=120), nullable=False, server_default="USER_REVOKED"),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_session_revocations_user_id", "session_revocations", ["user_id"])
    op.create_index("ix_session_revocations_session_id", "session_revocations", ["session_id"])

    op.create_table(
        "automation_rules",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("name", sa.String(length=120), nullable=False),
        sa.Column("trigger_type", sa.String(length=40), nullable=False),
        sa.Column("trigger_value", sa.String(length=160), nullable=False),
        sa.Column("is_enabled", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("requires_confirmation", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_automation_rules_user_id", "automation_rules", ["user_id"])

    op.create_table(
        "automation_conditions",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("rule_id", sa.String(length=36), sa.ForeignKey("automation_rules.id", ondelete="CASCADE"), nullable=False),
        sa.Column("condition_type", sa.String(length=40), nullable=False),
        sa.Column("condition_value", sa.String(length=160), nullable=False),
    )
    op.create_index("ix_automation_conditions_rule_id", "automation_conditions", ["rule_id"])

    op.create_table(
        "automation_actions",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("rule_id", sa.String(length=36), sa.ForeignKey("automation_rules.id", ondelete="CASCADE"), nullable=False),
        sa.Column("action_type", sa.String(length=40), nullable=False),
        sa.Column("action_payload", sa.Text(), nullable=False, server_default=""),
    )
    op.create_index("ix_automation_actions_rule_id", "automation_actions", ["rule_id"])

    op.create_table(
        "automation_execution_logs",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("rule_id", sa.String(length=36), sa.ForeignKey("automation_rules.id", ondelete="CASCADE"), nullable=False),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("status", sa.String(length=30), nullable=False, server_default="EXECUTED"),
        sa.Column("summary", sa.String(length=255), nullable=False, server_default=""),
        sa.Column("executed_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_automation_execution_logs_rule_id", "automation_execution_logs", ["rule_id"])
    op.create_index("ix_automation_execution_logs_user_id", "automation_execution_logs", ["user_id"])


def downgrade() -> None:
    for table in [
        "automation_execution_logs",
        "automation_actions",
        "automation_conditions",
        "automation_rules",
        "session_revocations",
        "user_security_settings",
        "login_attempts",
        "security_events",
        "family_permissions",
        "shared_reminders",
        "family_invitations",
        "family_members",
        "family_groups",
        "notification_preferences",
        "daily_routines",
        "calendar_events",
        "timetable_entries",
        "tasks",
        "reminders",
        "messaging_templates",
        "messaging_preferences",
        "contact_aliases",
        "app_preferences",
        "phone_action_preferences",
        "memory_audit_events",
        "memory_records",
        "memory_categories",
    ]:
        op.drop_table(table)
