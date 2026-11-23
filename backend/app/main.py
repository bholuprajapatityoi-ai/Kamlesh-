from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.router import api_v1_router
from app.core.config import get_settings
from app.core.exceptions import register_exception_handlers
from app.core.logging import configure_logging
from app.middleware.request_logging import RequestLoggingAndSecurityMiddleware

configure_logging()
settings = get_settings()

app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    docs_url="/docs" if settings.ENVIRONMENT.lower() != "production" else None,
    redoc_url="/redoc" if settings.ENVIRONMENT.lower() != "production" else None,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "Accept"],
)
app.add_middleware(RequestLoggingAndSecurityMiddleware)
register_exception_handlers(app)

app.include_router(api_v1_router, prefix="/api/v1")
