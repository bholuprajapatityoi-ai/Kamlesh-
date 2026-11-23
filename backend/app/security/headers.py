from fastapi import Response


def apply_security_headers(response: Response, path: str, is_production: bool = False) -> None:
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Referrer-Policy"] = "no-referrer"
    response.headers["Permissions-Policy"] = "microphone=(), camera=(), geolocation=()"
    if is_production:
        response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
    if "/auth/" in path or "/security/" in path:
        response.headers["Cache-Control"] = "no-store"
