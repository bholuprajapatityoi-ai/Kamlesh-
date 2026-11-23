def _register(client, email: str):
    res = client.post(
        "/api/v1/auth/register",
        json={
            "full_name": "Boss",
            "email": email,
            "password": "StrongPassword!123",
            "confirm_password": "StrongPassword!123",
        },
    )
    assert res.status_code == 201
    return res.json()


def test_security_headers_present_on_auth_and_health(client):
    res = client.get("/api/v1/health")
    assert res.headers.get("X-Content-Type-Options") == "nosniff"
    assert res.headers.get("X-Frame-Options") == "DENY"
    assert res.headers.get("Referrer-Policy") == "no-referrer"

    auth_res = client.post("/api/v1/auth/verify-email", json={"token": "invalid_token_123456"})
    assert auth_res.status_code == 400
    assert auth_res.headers.get("Cache-Control") == "no-store"


def test_device_and_notification_user_isolation(client):
    user_a = _register(client, "isolation_a@example.com")
    user_b = _register(client, "isolation_b@example.com")
    headers_a = {"Authorization": f"Bearer {user_a['access_token']}"}
    headers_b = {"Authorization": f"Bearer {user_b['access_token']}"}

    dev_a = client.post(
        "/api/v1/devices",
        headers=headers_a,
        json={"device_identifier": "dev-a-1", "device_name": "User A Phone"},
    ).json()
    notif_a = client.post(
        "/api/v1/notifications",
        headers=headers_a,
        json={"title": "Private Alert", "body": "Only User A"},
    ).json()

    assert client.delete(f"/api/v1/devices/{dev_a['id']}", headers=headers_b).status_code == 404
    assert client.post(f"/api/v1/notifications/{notif_a['id']}/read", headers=headers_b).status_code == 404
    assert client.delete(f"/api/v1/notifications/{notif_a['id']}", headers=headers_b).status_code == 404
