import logging
import time
import uuid
from fastapi import Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from app.core.config import get_settings
from app.security.headers import apply_security_headers

logger = logging.getLogger("golu.http")


class RequestLoggingAndSecurityMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        settings = get_settings()
        request_id = str(uuid.uuid4())[:8]
        is_prod = settings.ENVIRONMENT.lower() == "production"

        content_length = request.headers.get("content-length")
        if content_length:
            try:
                if int(content_length) > settings.MAX_REQUEST_SIZE_BYTES:
                    resp = JSONResponse(
                        status_code=413,
                        content={
                            "success": False,
                            "error": "payload_too_large",
                            "detail": "Request payload exceeds server limit.",
                        },
                    )
                    apply_security_headers(resp, request.url.path, is_production=is_prod)
                    resp.headers["X-Request-ID"] = request_id
                    return resp
            except ValueError:
                pass

        start = time.monotonic()
        response = await call_next(request)
        elapsed_ms = (time.monotonic() - start) * 1000
        client_ip = request.client.host if request.client else "unknown"

        logger.info(
            "rid=%s ip=%s method=%s path=%s status=%s duration_ms=%.1f",
            request_id,
            client_ip,
            request.method,
            request.url.path,
            response.status_code,
            elapsed_ms,
        )

        response.headers["X-Request-ID"] = request_id
        apply_security_headers(response, request.url.path, is_production=is_prod)
        return response
