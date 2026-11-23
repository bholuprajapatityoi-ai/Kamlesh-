from datetime import datetime, timedelta, timezone
from jose import jwt
from app.core.config import get_settings


def test_register_duplicate_login_refresh_logout_and_isolation(client):
    payload = {
        "full_name": "Boss One",
        "email": "boss1@example.com",
        "password": "Password123",
        "confirm_password": "Password123",
    }
    res = client.post("/api/v1/auth/register", json=payload)
    assert res.status_code == 201
    access1 = res.json()["access_token"]
    refresh1 = res.json()["refresh_token"]

    # Duplicate email prevented
    dup = client.post("/api/v1/auth/register", json=payload)
    assert dup.status_code == 409

    # Invalid password rejected
    bad = client.post(
        "/api/v1/auth/login",
        json={"email": "boss1@example.com", "password": "WrongPassword9"},
    )
    assert bad.status_code == 401

    # Valid login
    ok = client.post(
        "/api/v1/auth/login",
        json={"email": "boss1@example.com", "password": "Password123"},
    )
    assert ok.status_code == 200

    # Unauthorized blocked
    assert client.get("/api/v1/auth/me").status_code == 401

    # Refresh token rotation revokes old token
    rot = client.post("/api/v1/auth/refresh", json={"refresh_token": refresh1})
    assert rot.status_code == 200
    assert client.post("/api/v1/auth/refresh", json={"refresh_token": refresh1}).status_code == 401

    # Expired token rejected
    settings = get_settings()
    expired_jwt = jwt.encode(
        {
            "sub": res.json()["user"]["id"],
            "sid": "expired-sid",
            "type": "access",
            "exp": int((datetime.now(timezone.utc) - timedelta(minutes=5)).timestamp()),
        },
        settings.JWT_SECRET_KEY,
        algorithm=settings.JWT_ALGORITHM,
    )
    assert (
        client.get("/api/v1/auth/me", headers={"Authorization": f"Bearer {expired_jwt}"}).status_code
        == 401
    )

    # Logout revokes active session
    new_access = rot.json()["access_token"]
    assert (
        client.post("/api/v1/auth/logout", headers={"Authorization": f"Bearer {new_access}"}).status_code
        == 200
    )
    assert (
        client.get("/api/v1/auth/me", headers={"Authorization": f"Bearer {new_access}"}).status_code
        == 401
    )
    # Initial token still valid until its session is logged out; log it out too
    assert (
        client.post("/api/v1/auth/logout", headers={"Authorization": f"Bearer {access1}"}).status_code
        == 200
    )
