import time
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.redis_client import redis_client
from app.database.connection import check_database_health
from app.database.session import get_db
from app.schemas.common import DatabaseHealthResponse, HealthResponse

router = APIRouter(tags=["System"])


@router.get("/health", response_model=HealthResponse)
def get_health(response: Response, db: Session = Depends(get_db)):
    settings = get_settings()
    db_ok, db_error = check_database_health(db)
    redis_ok, _ = redis_client.ping()
    missing = settings.missing_production_config()
    if not db_ok:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
    return HealthResponse(
        status="ok" if db_ok else "degraded",
        backend_ok=True,
        database_ok=db_ok,
        database_error=db_error,
        redis_ok=redis_ok,
        missing_config=missing,
        timestamp=datetime.now(timezone.utc).isoformat(),
    )


@router.get("/health/database", response_model=DatabaseHealthResponse)
def get_database_health(response: Response, db: Session = Depends(get_db)):
    start = time.monotonic()
    db_ok, db_error = check_database_health(db)
    latency_ms = round((time.monotonic() - start) * 1000, 2)
    redis_ok, _ = redis_client.ping()
    dialect = db.bind.dialect.name if db.bind is not None else "unknown"
    if not db_ok:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
    return DatabaseHealthResponse(
        status="connected" if db_ok else "unavailable",
        database_ok=db_ok,
        database_engine=dialect,
        latency_ms=latency_ms,
        redis_ok=redis_ok,
        database_error=db_error,
        timestamp=datetime.now(timezone.utc).isoformat(),
    )
