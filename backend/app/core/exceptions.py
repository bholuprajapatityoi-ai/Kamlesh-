import logging
from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from sqlalchemy.exc import IntegrityError, SQLAlchemyError

from app.core.config import get_settings

logger = logging.getLogger("golu.errors")


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(HTTPException)
    async def http_exception_handler(request: Request, exc: HTTPException):
        return JSONResponse(
            status_code=exc.status_code,
            content={
                "success": False,
                "error": f"http_{exc.status_code}",
                "detail": exc.detail,
            },
        )

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        return JSONResponse(
            status_code=422,
            content={
                "success": False,
                "error": "validation_error",
                "detail": "Invalid request payload.",
                "fields": [
                    {"loc": list(err.get("loc", ())), "msg": err.get("msg")}
                    for err in exc.errors()
                ],
            },
        )

    @app.exception_handler(IntegrityError)
    async def integrity_exception_handler(request: Request, exc: IntegrityError):
        logger.warning("Database integrity violation on %s", request.url.path)
        return JSONResponse(
            status_code=409,
            content={
                "success": False,
                "error": "conflict",
                "detail": "Database constraint violation or duplicate record.",
            },
        )

    @app.exception_handler(SQLAlchemyError)
    async def database_exception_handler(request: Request, exc: SQLAlchemyError):
        logger.error("Database failure on %s: %s", request.url.path, type(exc).__name__)
        return JSONResponse(
            status_code=503,
            content={
                "success": False,
                "error": "database_unavailable",
                "detail": "Database operation failed. Verify PostgreSQL connectivity and migrations.",
            },
        )

    @app.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception):
        settings = get_settings()
        logger.exception("Unhandled error on %s", request.url.path)
        detail = (
            f"Internal server error: {type(exc).__name__}"
            if settings.ENVIRONMENT.lower() != "production"
            else "An unexpected server error occurred."
        )
        return JSONResponse(
            status_code=500,
            content={
                "success": False,
                "error": "internal_server_error",
                "detail": detail,
            },
        )
