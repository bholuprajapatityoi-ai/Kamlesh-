from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session
from typing import Optional, Tuple

from app.core.config import get_settings


def create_db_engine(database_url: Optional[str] = None) -> Engine:
    settings = get_settings()
    url = database_url or settings.DATABASE_URL
    if url.startswith("sqlite"):
        return create_engine(url, connect_args={"check_same_thread": False})
    return create_engine(
        url,
        pool_pre_ping=True,
        pool_size=settings.DB_POOL_SIZE,
        max_overflow=settings.DB_MAX_OVERFLOW,
        pool_timeout=settings.DB_POOL_TIMEOUT,
    )


engine = create_db_engine()


def check_database_health(db: Session) -> Tuple[bool, Optional[str]]:
    try:
        db.execute(text("SELECT 1"))
        return True, None
    except Exception as exc:
        return False, type(exc).__name__
