import re
from fastapi import HTTPException, status

_CONTROL_CHARS = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")
_SECRET_PATTERNS = [
    re.compile(r"sk-[A-Za-z0-9]{16,}"),
    re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----"),
    re.compile(r"\b\d{6}\s*(?:is your otp|otp)\b", re.IGNORECASE),
    re.compile(r"\b(?:password|pin|cvv|secret_key)\s*[:=]", re.IGNORECASE),
]


def sanitize_text(value: str, max_length: int = 2000) -> str:
    cleaned = _CONTROL_CHARS.sub("", value or "").strip()
    return cleaned[:max_length]


def contains_secret_pattern(value: str) -> bool:
    text = value or ""
    return any(pattern.search(text) for pattern in _SECRET_PATTERNS)


def mask_email(email: str) -> str:
    cleaned = (email or "").strip()
    if "@" not in cleaned:
        return "***"
    local, domain = cleaned.split("@", 1)
    if len(local) <= 2:
        masked_local = "*" * len(local)
    else:
        masked_local = f"{local[0]}***{local[-1]}"
    return f"{masked_local}@{domain}"


def ensure_owner(resource_user_id: str, actor_user_id: str, detail: str = "Resource not found.") -> None:
    if resource_user_id != actor_user_id:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=detail)
