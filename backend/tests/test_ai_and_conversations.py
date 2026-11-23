def _register(client, email: str):
    res = client.post(
        "/api/v1/auth/register",
        json={
            "full_name": "AI Boss",
            "email": email,
            "password": "Password123",
            "confirm_password": "Password123",
        },
    )
    return {"Authorization": f"Bearer {res.json()['access_token']}"}


def test_ai_intent_chat_isolation_and_failure_handling(client):
    assert client.post("/api/v1/ai/chat", json={"message": "Hello"}).status_code == 401

    boss1 = _register(client, "ai1@example.com")
    boss2 = _register(client, "ai2@example.com")

    # Intent classification
    intent_res = client.post(
        "/api/v1/ai/intent",
        json={"text": "Photosynthesis samjhao"},
        headers=boss1,
    )
    assert intent_res.status_code == 200
    assert intent_res.json()["recommended_route"] == "ONLINE_AI_BACKEND"

    # Greeting handled locally and saved to boss1 conversation DB
    chat_res = client.post(
        "/api/v1/ai/chat",
        json={"message": "Hey Golu", "language": "Hinglish", "session_id": "sess-1"},
        headers=boss1,
    )
    assert chat_res.status_code == 200
    assert "Boss" in chat_res.json()["reply"]

    # Online AI returns 503 honestly when OPENAI_API_KEY is unconfigured
    fail_res = client.post(
        "/api/v1/ai/chat",
        json={"message": "Photosynthesis samjhao", "language": "Hinglish"},
        headers=boss1,
    )
    assert fail_res.status_code == 503

    # User isolation: boss2 cannot read or delete boss1's conversation session
    boss1_sessions = client.get("/api/v1/ai/conversations", headers=boss1).json()
    assert len(boss1_sessions) == 1
    assert (
        client.get("/api/v1/ai/conversations/sess-1", headers=boss2).status_code == 404
    )
    assert (
        client.delete("/api/v1/ai/conversations/sess-1", headers=boss2).status_code == 404
    )
    assert (
        client.get("/api/v1/ai/conversations/sess-1", headers=boss1).status_code == 200
    )

    # Invalid empty payload rejected with 422
    assert client.post("/api/v1/ai/chat", json={"message": ""}, headers=boss1).status_code == 422
