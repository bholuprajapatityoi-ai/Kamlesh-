def _auth_header(client, email: str):
    res = client.post(
        "/api/v1/auth/register",
        json={
            "full_name": "Voice Boss",
            "email": email,
            "password": "Password123",
            "confirm_password": "Password123",
        },
    )
    return {"Authorization": f"Bearer {res.json()['access_token']}"}


def test_voice_and_wakeword_settings_auth_and_update(client):
    # Unauthenticated blocked
    assert client.get("/api/v1/voice/settings").status_code == 401
    assert client.get("/api/v1/assistant/wake-word/settings").status_code == 401

    headers = _auth_header(client, "voice@example.com")

    v_get = client.get("/api/v1/voice/settings", headers=headers)
    assert v_get.status_code == 200
    assert v_get.json()["language"] == "Hinglish"

    v_put = client.put(
        "/api/v1/voice/settings",
        json={"language": "Hindi", "voice_gender": "Female", "speech_speed": 1.15},
        headers=headers,
    )
    assert v_put.status_code == 200
    assert v_put.json()["voice_gender"] == "Female"

    w_get = client.get("/api/v1/assistant/wake-word/settings", headers=headers)
    assert w_get.status_code == 200
    assert w_get.json()["wake_word_enabled"] is False

    w_put = client.put(
        "/api/v1/assistant/wake-word/settings",
        json={"wake_word_enabled": True},
        headers=headers,
    )
    assert w_put.status_code == 200
    assert w_put.json()["wake_word_enabled"] is True

    a_put = client.put(
        "/api/v1/assistant/settings",
        json={"greeting_message": "Namaste Boss!", "keep_conversation_history": False},
        headers=headers,
    )
    assert a_put.status_code == 200
    assert a_put.json()["keep_conversation_history"] is False
