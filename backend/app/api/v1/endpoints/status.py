from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.redis_client import redis_client
from app.database.connection import check_database_health
from app.database.session import get_db
from app.schemas.common import SystemStatusResponse

router = APIRouter(tags=["System"])


@router.get("/status", response_model=SystemStatusResponse)
def get_status(db: Session = Depends(get_db)):
    settings = get_settings()
    db_ok, _ = check_database_health(db)
    redis_ok, _ = redis_client.ping()
    redis_status = (
        "connected" if redis_ok else ("unreachable" if settings.redis_configured else "unconfigured")
    )
    return SystemStatusResponse(
        app=settings.APP_NAME,
        version=settings.APP_VERSION,
        environment=settings.ENVIRONMENT,
        database_status="connected" if db_ok else "unreachable",
        redis_status=redis_status,
        openai_configured=settings.openai_configured,
        email_delivery_configured=settings.email_delivery_configured,
        missing_config=settings.missing_production_config(),
    )
