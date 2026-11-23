import re
from fastapi import HTTPException, status

PASSWORD_COMPLEXITY_RE = re.compile(r"^(?=.*[A-Za-z])(?=.*\d).{8,128}$")


def validate_password_strength(password: str) -> None:
    if len(password) < 8:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Password must be at least 8 characters long.",
        )
    if not PASSWORD_COMPLEXITY_RE.match(password):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Password must contain at least one letter and one number.",
        )
