import os
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

os.environ.setdefault("DATABASE_URL", "sqlite:///./test_golu.db")
os.environ.setdefault("SECRET_KEY", "test_only_app_secret_key_for_pytest_validation_1234567890")
os.environ.setdefault("JWT_SECRET_KEY", "test_only_jwt_secret_key_for_pytest_validation_1234567890")
os.environ.setdefault("OPENAI_API_KEY", "")
os.environ.setdefault("ENVIRONMENT", "development")

from app.database.base import Base  # noqa: E402
from app.database.session import get_db  # noqa: E402
from app.main import app  # noqa: E402
import app.models  # noqa: E402, F401


TEST_ENGINE = create_engine("sqlite:///./test_golu.db", connect_args={"check_same_thread": False})
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=TEST_ENGINE)


@pytest.fixture(autouse=True)
def setup_test_db():
    Base.metadata.drop_all(bind=TEST_ENGINE)
    Base.metadata.create_all(bind=TEST_ENGINE)
    yield
    Base.metadata.drop_all(bind=TEST_ENGINE)


@pytest.fixture()
def db_session():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()


@pytest.fixture()
def client():
    def override_get_db():
        db = TestingSessionLocal()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()
