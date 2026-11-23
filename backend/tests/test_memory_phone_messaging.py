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
    assert res.status_code == 201, res.text
    return res.json()


def test_memory_crud_and_user_isolation(client):
    user_a = _register(client, "boss.a@example.com", "Boss A")
    user_b = _register(client, "boss.b@example.com", "Boss B")
    hdr_a = {"Authorization": f"Bearer {user_a['access_token']}"}
    hdr_b = {"Authorization": f"Bearer {user_b['access_token']}"}

    # Explicit confirmation required
    unconfirmed = client.post(
        "/api/v1/memory",
        json={
            "category": "Preferences",
            "content": "I prefer Hinglish replies",
            "explicit_user_confirmed": False,
        },
        headers=hdr_a,
    )
    assert unconfirmed.status_code == 400

    # Reject sensitive secret content
    secret_res = client.post(
        "/api/v1/memory",
        json={
            "category": "Custom Notes",
            "content": "My bank password is Secret1234",
            "explicit_user_confirmed": True,
        },
        headers=hdr_a,
    )
    assert secret_res.status_code == 400

    # Create valid memory
    created = client.post(
        "/api/v1/memory",
        json={
            "category": "Favourite Apps",
            "content": "YouTube and Spotify are my favourite apps",
            "explicit_user_confirmed": True,
        },
        headers=hdr_a,
    )
    assert created.status_code == 201
    mem_id = created.json()["id"]

    # User B must not read, update, or delete User A's memory
    assert client.get(f"/api/v1/memory/{mem_id}", headers=hdr_b).status_code == 404
    assert (
        client.put(f"/api/v1/memory/{mem_id}", json={"content": "Tampered"}, headers=hdr_b).status_code
        == 404
    )
    assert client.delete(f"/api/v1/memory/{mem_id}", headers=hdr_b).status_code == 404

    # Owner CRUD works
    listed = client.get("/api/v1/memory", headers=hdr_a).json()
    assert listed["total"] == 1
    updated = client.put(
        f"/api/v1/memory/{mem_id}",
        json={"content": "Spotify and YouTube Music"},
        headers=hdr_a,
    )
    assert updated.status_code == 200
    assert "Spotify" in updated.json()["content"]

    assert client.delete(f"/api/v1/memory/{mem_id}", headers=hdr_a).status_code == 200
    assert client.get("/api/v1/memory", headers=hdr_a).json()["total"] == 0

    # Bulk clear
    client.post(
        "/api/v1/memory",
        json={"category": "Timetable", "content": "Gym at 7 AM", "explicit_user_confirmed": True},
        headers=hdr_a,
    )
    assert client.delete("/api/v1/memory", headers=hdr_a).status_code == 200
    assert client.get("/api/v1/memory", headers=hdr_a).json()["total"] == 0


def test_phone_and_messaging_preferences_and_templates(client):
    user = _register(client, "phone.boss@example.com")
    other = _register(client, "other.boss@example.com", "Other Boss")
    hdr = {"Authorization": f"Bearer {user['access_token']}"}
    hdr_other = {"Authorization": f"Bearer {other['access_token']}"}

    # Phone action preferences defaults & update
    default_prefs = client.get("/api/v1/phone/preferences", headers=hdr)
    assert default_prefs.status_code == 200
    assert default_prefs.json()["confirm_before_call"] is True

    prefs = client.put(
        "/api/v1/phone/preferences",
        json={"confirm_before_call": True, "allow_volume_control": True, "allow_media_control": False},
        headers=hdr,
    )
    assert prefs.status_code == 200
    assert prefs.json()["confirm_before_call"] is True
    assert prefs.json()["allow_media_control"] is False

    # Whitespace-only nickname rejected
    bad_alias = client.post(
        "/api/v1/phone/contact-aliases",
        json={"nickname": "   ", "contact_name": "Rajesh Sharma", "phone_hint": "98xxxx"},
        headers=hdr,
    )
    assert bad_alias.status_code == 400

    # Contact alias CRUD + query + isolation
    alias = client.post(
        "/api/v1/phone/contact-aliases",
        json={"nickname": "Papa", "contact_name": "Rajesh Sharma", "phone_hint": "98xxxx"},
        headers=hdr,
    )
    assert alias.status_code == 201
    alias_id = alias.json()["id"]

    assert len(client.get("/api/v1/phone/contact-aliases", headers=hdr).json()) == 1
    assert len(client.get("/api/v1/phone/contact-aliases?q=papa", headers=hdr).json()) == 1
    assert len(client.get("/api/v1/phone/contact-aliases?q=unknown", headers=hdr).json()) == 0
    assert client.get(f"/api/v1/phone/contact-aliases/{alias_id}", headers=hdr).status_code == 200
    assert client.get(f"/api/v1/phone/contact-aliases/{alias_id}", headers=hdr_other).status_code == 404

    updated_alias = client.put(
        f"/api/v1/phone/contact-aliases/{alias_id}",
        json={"contact_name": "Papa Ji", "phone_hint": "+919800000000"},
        headers=hdr,
    )
    assert updated_alias.status_code == 200
    assert updated_alias.json()["contact_name"] == "Papa Ji"
    assert client.delete(f"/api/v1/phone/contact-aliases/{alias_id}", headers=hdr_other).status_code == 404
    assert client.delete(f"/api/v1/phone/contact-aliases/{alias_id}", headers=hdr).status_code == 200

    # App preferences upsert & delete
    app_pref = client.put(
        "/api/v1/phone/app-preferences",
        json={"alias_name": "Music", "package_name": "com.spotify.music", "is_favourite": True},
        headers=hdr,
    )
    assert app_pref.status_code == 200
    pref_id = app_pref.json()["id"]
    assert len(client.get("/api/v1/phone/app-preferences", headers=hdr).json()) == 1
    assert client.delete(f"/api/v1/phone/app-preferences/{pref_id}", headers=hdr_other).status_code == 404
    assert client.delete(f"/api/v1/phone/app-preferences/{pref_id}", headers=hdr).status_code == 200

    # Messaging preferences & templates
    msg_prefs = client.put(
        "/api/v1/messaging/preferences",
        json={"default_language": "Hinglish", "always_confirm_before_open": True},
        headers=hdr,
    )
    assert msg_prefs.status_code == 200
    tmpl = client.post(
        "/api/v1/messaging/templates",
        json={"title": "Reach Home", "language": "Hinglish", "body": "Papa, main ghar pahunch gaya."},
        headers=hdr,
    )
    assert tmpl.status_code == 201
    assert len(client.get("/api/v1/messaging/templates", headers=hdr).json()) == 1
