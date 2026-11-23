def _register(client, email: str, name: str = "Boss"):
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


def test_daily_life_crud_and_isolation(client):
    u1 = _register(client, "dl1@example.com", "Boss 1")
    u2 = _register(client, "dl2@example.com", "Boss 2")
    h1 = {"Authorization": f"Bearer {u1['access_token']}"}
    h2 = {"Authorization": f"Bearer {u2['access_token']}"}

    rem = client.post(
        "/api/v1/reminders",
        json={
            "title": "Take medicine",
            "notes": "After dinner",
            "remind_at": "2026-12-01T20:00:00Z",
            "timezone_name": "Asia/Kolkata",
            "recurrence": "DAILY",
        },
        headers=h1,
    )
    assert rem.status_code == 201
    rem_id = rem.json()["id"]
    assert client.put(f"/api/v1/reminders/{rem_id}", json={"is_completed": True}, headers=h2).status_code == 404

    task = client.post("/api/v1/tasks", json={"title": "Finish report", "priority": "HIGH"}, headers=h1)
    assert task.status_code == 201

    tt = client.post(
        "/api/v1/timetable",
        json={
            "day_of_week": "Monday",
            "start_time": "09:00",
            "end_time": "10:00",
            "subject_or_activity": "Mathematics",
        },
        headers=h1,
    )
    assert tt.status_code == 201

    ev = client.post(
        "/api/v1/calendar/events",
        json={
            "title": "Family Dinner",
            "start_at": "2026-12-02T19:00:00Z",
            "end_at": "2026-12-02T21:00:00Z",
        },
        headers=h1,
    )
    assert ev.status_code == 201
    assert len(client.get("/api/v1/calendar/events", headers=h2).json()) == 0


def test_family_mode_and_private_data_isolation(client):
    boss = _register(client, "boss.family@example.com", "Boss")
    papa = _register(client, "papa.family@example.com", "Papa")
    h_boss = {"Authorization": f"Bearer {boss['access_token']}"}
    h_papa = {"Authorization": f"Bearer {papa['access_token']}"}

    grp = client.post("/api/v1/family/groups", json={"name": "Home Family"}, headers=h_boss)
    assert grp.status_code == 201
    group_id = grp.json()["id"]

    inv = client.post(
        "/api/v1/family/invitations",
        json={
            "group_id": group_id,
            "invitee_email": "papa.family@example.com",
            "role": "FATHER",
            "display_label": "Papa",
            "father_friendly_mode": True,
        },
        headers=h_boss,
    )
    assert inv.status_code == 201
    invite_code = inv.json()["invite_code"]

    acc = client.post(
        "/api/v1/family/invitations/accept",
        json={"invite_code": invite_code},
        headers=h_papa,
    )
    assert acc.status_code == 200
    assert acc.json()["father_friendly_mode"] is True

    # Shared reminder is visible to both
    sr = client.post(
        "/api/v1/family/shared-reminders",
        json={"group_id": group_id, "title": "Electricity bill", "remind_at": "2026-12-05T10:00:00Z"},
        headers=h_boss,
    )
    assert sr.status_code == 201
    assert len(client.get("/api/v1/family/shared-reminders", headers=h_papa).json()) == 1

    # Papa's private memory remains strictly invisible to family admin Boss
    papa_mem = client.post(
        "/api/v1/memory",
        json={"category": "Custom Notes", "content": "Papa private note", "explicit_user_confirmed": True},
        headers=h_papa,
    )
    assert papa_mem.status_code == 201
    assert client.get("/api/v1/memory", headers=h_boss).json()["total"] == 0


def test_security_dashboard_and_automation_rules(client):
    user = _register(client, "sec.auto@example.com", "Boss Sec")
    hdr = {"Authorization": f"Bearer {user['access_token']}"}

    dash = client.get("/api/v1/security/dashboard", headers=hdr)
    assert dash.status_code == 200
    assert len(dash.json()["active_sessions"]) >= 1
    assert any(e["event_type"] == "ACCOUNT_REGISTERED" for e in dash.json()["recent_events"])

    # Sensitive automation requires explicit confirmation
    unconfirmed = client.post(
        "/api/v1/automations",
        json={
            "name": "Morning Open App",
            "trigger_type": "TIME",
            "trigger_value": "08:00",
            "actions": [{"action_type": "OPEN_APP", "action_payload": "Calendar"}],
            "confirmed_sensitive": False,
        },
        headers=hdr,
    )
    assert unconfirmed.status_code == 400

    rule = client.post(
        "/api/v1/automations",
        json={
            "name": "Morning Greeting",
            "trigger_type": "TIME",
            "trigger_value": "08:00",
            "actions": [{"action_type": "SPEAK_MESSAGE", "action_payload": "Good morning Boss!"}],
        },
        headers=hdr,
    )
    assert rule.status_code == 201
    rule_id = rule.json()["id"]

    assert client.post(f"/api/v1/automations/{rule_id}/disable", headers=hdr).json()["is_enabled"] is False
    assert client.post(f"/api/v1/automations/{rule_id}/enable", headers=hdr).json()["is_enabled"] is True

    log = client.post(
        f"/api/v1/automations/{rule_id}/logs",
        json={"status": "EXECUTED", "summary": "Spoke morning greeting"},
        headers=hdr,
    )
    assert log.status_code == 200
    assert len(client.get("/api/v1/automations/history", headers=hdr).json()) == 1
