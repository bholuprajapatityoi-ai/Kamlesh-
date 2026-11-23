def _register(client, email: str, name: str = "Boss User"):
    res = client.post(
        "/api/v1/auth/register",
        json={
            "full_name": name,
            "email": email,
            "password": "StrongPassword!123",
            "confirm_password": "StrongPassword!123",
        },
    )
    assert res.status_code == 201
    return res.json()


def test_database_health_and_status_endpoints(client):
    res = client.get("/api/v1/health/database")
    assert res.status_code == 200
    body = res.json()
    assert body["database_ok"] is True
    assert body["status"] == "connected"
    assert "latency_ms" in body


def test_devices_and_notifications_lifecycle_and_profile_alias(client):
    auth = _register(client, "devices_boss@example.com")
    headers = {"Authorization": f"Bearer {auth['access_token']}"}

    # Profile alias
    prof = client.get("/api/v1/profile", headers=headers)
    assert prof.status_code == 200
    assert prof.json()["email"] == "devices_boss@example.com"

    # Register device (push token masked)
    dev = client.post(
        "/api/v1/devices",
        headers=headers,
        json={
            "device_identifier": "pixel-9-pro-001",
            "device_name": "Boss Pixel 9",
            "platform": "android",
            "os_version": "15",
            "app_version": "1.1.0",
            "push_token": "fcm_secret_token_abcdef123456",
        },
    )
    assert dev.status_code == 201
    dev_json = dev.json()
    assert dev_json["device_name"] == "Boss Pixel 9"
    assert "abcdef123456" not in dev_json["push_token_masked"]

    # Create and mark notification read
    notif = client.post(
        "/api/v1/notifications",
        headers=headers,
        json={"title": "Morning Briefing", "body": "2 tasks today, Boss!", "category": "SUMMARY"},
    )
    assert notif.status_code == 201
    nid = notif.json()["id"]

    read_res = client.post(f"/api/v1/notifications/{nid}/read", headers=headers)
    assert read_res.status_code == 200
    assert read_res.json()["status"] == "READ"
