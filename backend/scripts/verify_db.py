import sys
from sqlalchemy import inspect

from app.database.connection import check_database_health, engine
from app.database.session import SessionLocal


def main() -> int:
    db = SessionLocal()
    try:
        ok, err = check_database_health(db)
        if not ok:
            print(f"ERROR: PostgreSQL health check failed: {err}")
            return 1
        tables = set(inspect(engine).get_table_names())
        required = {
            "users",
            "user_profiles",
            "user_sessions",
            "password_reset_tokens",
            "email_verification_tokens",
            "assistant_settings",
            "conversation_sessions",
            "conversation_messages",
            "memory_categories",
            "memory_records",
            "memory_audit_events",
            "phone_action_preferences",
            "app_preferences",
            "contact_aliases",
            "messaging_preferences",
            "messaging_templates",
            "reminders",
            "tasks",
            "timetable_entries",
            "calendar_events",
            "daily_routines",
            "notification_preferences",
            "device_registrations",
            "notification_records",
            "family_groups",
            "family_members",
            "family_invitations",
            "shared_reminders",
            "family_permissions",
            "security_events",
            "login_attempts",
            "user_security_settings",
            "session_revocations",
            "automation_rules",
            "automation_conditions",
            "automation_actions",
            "automation_execution_logs",
            "knowledge_documents",
            "notification_insights",
            "translation_history",
            "research_reports",
            "coding_history",
            "creative_assets",
            "voice_recordings",
            "smart_home_configs",
            "smart_home_devices",
            "expense_budgets",
            "expense_records",
            "study_sessions",
            "travel_plans",
            "sync_checkpoints",
            "diagnostic_events",
        }
        missing = sorted(required - tables)
        if missing:
            print(f"ERROR: Missing tables (run 'alembic upgrade head'): {missing}")
            return 2
        print("OK: PostgreSQL connected and all Phase 1-20 (GOLU 2.0) tables exist.")
        return 0
    finally:
        db.close()


if __name__ == "__main__":
    sys.exit(main())
