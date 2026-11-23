from sqlalchemy import inspect
from app.database.session import get_db
from app.main import app
from tests.conftest import TEST_ENGINE


def test_tables_created_and_transaction_rollback(db_session):
    tables = set(inspect(TEST_ENGINE).get_table_names())
    expected = {
        "users",
        "user_profiles",
        "user_sessions",
        "password_reset_tokens",
        "assistant_settings",
        "conversation_sessions",
        "conversation_messages",
    }
    assert expected.issubset(tables)


def test_health_status_config_endpoints(client):
    health = client.get("/api/v1/health")
    assert health.status_code == 200
    h = health.json()
    assert h["backend_ok"] is True
    assert h["database_ok"] is True
    assert "OPENAI_API_KEY" in h["missing_config"]

    st = client.get("/api/v1/status")
    assert st.status_code == 200
    assert st.json()["database_status"] == "connected"

    cfg = client.get("/api/v1/config")
    assert cfg.status_code == 200
    assert cfg.json()["wake_phrase"] == "Hey Golu"


def test_health_reports_503_when_database_unavailable(client):
    class BrokenSession:
        def execute(self, *_args, **_kwargs):
            raise RuntimeError("Simulated DB connection failure")

        def close(self):
            pass

    def broken_db():
        yield BrokenSession()

    app.dependency_overrides[get_db] = broken_db
    res = client.get("/api/v1/health")
    assert res.status_code == 503
    body = res.json()
    assert body["backend_ok"] is True
    assert body["database_ok"] is False
    assert body["status"] == "degraded"
    app.dependency_overrides.clear()
