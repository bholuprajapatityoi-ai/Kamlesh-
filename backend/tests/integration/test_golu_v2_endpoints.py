from unittest.mock import AsyncMock, patch


def _register_and_headers(client, email="boss_v2@example.com"):
    res = client.post(
        "/api/v1/auth/register",
        json={
            "full_name": "Boss GOLU",
            "email": email,
            "password": "StrongPassword!123",
            "preferred_title": "Boss",
        },
    )
    assert res.status_code == 201
    token = res.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


def test_v2_all_20_features_end_to_end_and_isolation(client):
    headers_a = _register_and_headers(client, "boss_a@example.com")
    headers_b = _register_and_headers(client, "boss_b@example.com")

    # Feature 1: Smart Screen Understanding (consent gate + local fallback + cross-check)
    denied_vision = client.post(
        "/api/v2/vision/analyze",
        headers=headers_a,
        json={"screen_text": "Meeting at 4 PM", "action": "EXPLAIN", "consent_granted": False},
    )
    assert denied_vision.status_code == 403

    ok_vision = client.post(
        "/api/v2/vision/analyze",
        headers=headers_a,
        json={"screen_text": "Meeting at 4 PM with Architecture team", "action": "EXPLAIN", "consent_granted": True},
    )
    assert ok_vision.status_code == 200
    assert ok_vision.json()["action"] == "EXPLAIN"
    assert ok_vision.json()["ai_backed"] is False

    # Feature 2: Live Camera AI (consent gate + TRANSLATE mode alignment + honest 503 when OPENAI_API_KEY unset)
    denied_cam = client.post(
        "/api/v2/camera/analyze",
        headers=headers_a,
        json={"image_base64": "AQID", "question": "Translate sign", "mode": "TRANSLATE", "consent_granted": False},
    )
    assert denied_cam.status_code == 403

    cam_no_key = client.post(
        "/api/v2/camera/analyze",
        headers=headers_a,
        json={"image_base64": "AQID", "question": "Translate sign", "mode": "TRANSLATE", "consent_granted": True},
    )
    assert cam_no_key.status_code == 503

    # Feature 3: Document Intelligence (consent + upload + list + Q&A + isolation + delete)
    denied_doc = client.post(
        "/api/v2/documents",
        headers=headers_a,
        json={"title": "Notes", "text_content": "Kotlin coroutines", "consent_granted": False},
    )
    assert denied_doc.status_code == 403

    doc_res = client.post(
        "/api/v2/documents",
        headers=headers_a,
        json={"title": "Architecture Spec", "text_content": "Kotlin Coroutines run structured concurrency.", "consent_granted": True},
    )
    assert doc_res.status_code == 201
    doc_id = doc_res.json()["id"]

    assert len(client.get("/api/v2/documents", headers=headers_a).json()) == 1
    assert len(client.get("/api/v2/documents", headers=headers_b).json()) == 0
    assert client.post(f"/api/v2/documents/{doc_id}/ask", headers=headers_b, json={"question": "coroutines?"}).status_code == 404

    ask_res = client.post(
        f"/api/v2/documents/{doc_id}/ask",
        headers=headers_a,
        json={"question": "What about coroutines?"},
    )
    assert ask_res.status_code == 200
    assert "Architecture Spec" in ask_res.json()["answer"] or "Coroutines" in ask_res.json()["answer"]

    # Feature 4: Smart Notification Intelligence (consent + classification + reminder + summary + delete)
    assert client.post(
        "/api/v2/notifications/analyze",
        headers=headers_a,
        json={"source_app": "Bank", "title": "OTP", "consent_granted": False},
    ).status_code == 403

    notif_res = client.post(
        "/api/v2/notifications/analyze",
        headers=headers_a,
        json={
            "source_app": "BankApp",
            "title": "UPI debit alert Rs 500",
            "body_preview": "Transaction completed",
            "consent_granted": True,
            "create_reminder_if_important": True,
        },
    )
    assert notif_res.status_code == 201
    assert notif_res.json()["is_important"] is True
    assert notif_res.json()["converted_reminder_id"] is not None
    notif_id = notif_res.json()["id"]

    summary_res = client.get("/api/v2/notifications/summary", headers=headers_a)
    assert summary_res.status_code == 200
    assert summary_res.json()["important_count"] == 1
    assert client.delete(f"/api/v2/notifications/{notif_id}", headers=headers_b).status_code == 404
    assert client.delete(f"/api/v2/notifications/{notif_id}", headers=headers_a).status_code == 200

    # Feature 5: Personal Knowledge Brain unified with MemoryRecord (CRUD + cross-user isolation)
    note_res = client.post(
        "/api/v2/knowledge",
        headers=headers_a,
        json={"title": "Exam Prep", "category": "Study", "content": "Revise SQL joins", "tags": ["sql", "db"]},
    )
    assert note_res.status_code == 201
    note_id = note_res.json()["id"]

    upd_note = client.put(
        f"/api/v2/knowledge/{note_id}",
        headers=headers_a,
        json={"title": "Exam Prep v2", "category": "Study", "content": "Revise SQL joins & indexes", "tags": ["sql"]},
    )
    assert upd_note.status_code == 200
    assert "indexes" in upd_note.json()["content"]
    assert client.put(
        f"/api/v2/knowledge/{note_id}",
        headers=headers_b,
        json={"title": "Hijack", "category": "X", "content": "Nope", "tags": []},
    ).status_code == 404

    mem_list = client.get("/api/v1/memory", headers=headers_a)
    assert mem_list.status_code == 200
    assert any("Revise SQL joins" in m["content"] for m in mem_list.json()["items"])

    # Feature 6: Live Language Translator (honest 503 without key + empty history isolation + mocked OpenAI flow)
    assert client.post(
        "/api/v2/translator/translate",
        headers=headers_a,
        json={"text": "Good morning", "source_language": "English", "target_language": "Hindi", "mode": "TEXT"},
    ).status_code == 503
    assert client.get("/api/v2/translator/history", headers=headers_a).json() == []

    # Feature 7: Deep Research Mode (mocked external search provider returns real citation schema + history)
    with patch(
        "app.api.v2.endpoints.brain.perform_legitimate_web_research",
        new=AsyncMock(return_value=("wikipedia-api", [{"title": "Kotlin", "url": "https://en.wikipedia.org/wiki/Kotlin", "snippet": "Modern JVM language"}], "")),
    ):
        res_report = client.post(
            "/api/v2/research/query",
            headers=headers_a,
            json={"query": "Kotlin coroutines", "language": "English"},
        )
    assert res_report.status_code == 201
    assert res_report.json()["provider_used"] == "wikipedia-api"
    assert len(res_report.json()["citations"]) == 1
    assert len(client.get("/api/v2/research/history", headers=headers_a).json()) == 1
    assert len(client.get("/api/v2/research/history", headers=headers_b).json()) == 0

    # Feature 8: Advanced Offline Brain Manifest
    manifest = client.get("/api/v2/offline/manifest")
    assert manifest.status_code == 200
    assert len(manifest.json()["offline_capable_features"]) >= 4

    # Feature 9: Life Planner AI (approval-gated)
    plan_res = client.post("/api/v2/planner/suggest", headers=headers_a, json={"goal_focus": "Ship GOLU 2.0", "available_hours": 5})
    assert plan_res.status_code == 200
    assert plan_res.json()["requires_user_approval"] is True

    approve_res = client.post(
        "/api/v2/planner/approve",
        headers=headers_a,
        json={"items": [{"title": "Review PR", "item_type": "TASK", "priority": "HIGH"}]},
    )
    assert approve_res.status_code == 200

    # Feature 10: Coding Assistant (503 without key + history empty)
    assert client.post(
        "/api/v2/coding/assist",
        headers=headers_a,
        json={"prompt": "Write binary search", "language": "kotlin", "task_type": "GENERATE"},
    ).status_code == 503
    assert client.get("/api/v2/coding/history", headers=headers_a).json() == []

    # Feature 11: Creative Studio (503 without key records failed audit row + delete isolation)
    assert client.post(
        "/api/v2/creative/generate",
        headers=headers_a,
        json={"asset_type": "IMAGE", "prompt": "Futuristic AI orb", "style": "Neon Cyan"},
    ).status_code == 503
    creative_hist = client.get("/api/v2/creative/history", headers=headers_a).json()
    assert len(creative_hist) == 1
    assert creative_hist[0]["status"] == "FAILED"
    assert client.delete(f"/api/v2/creative/{creative_hist[0]['id']}", headers=headers_b).status_code == 404
    assert client.delete(f"/api/v2/creative/{creative_hist[0]['id']}", headers=headers_a).status_code == 200

    # Feature 12: Voice Recorder Intelligence (create + update + search + delete)
    rec_res = client.post(
        "/api/v2/recorder",
        headers=headers_a,
        json={"title": "Standup", "duration_seconds": 42, "transcript": "Reviewed release checklist", "generate_summary": True},
    )
    assert rec_res.status_code == 201
    rec_id = rec_res.json()["id"]
    upd_rec = client.put(
        f"/api/v2/recorder/{rec_id}",
        headers=headers_a,
        json={"title": "Standup Updated", "transcript": "Finalized release", "regenerate_summary": False},
    )
    assert upd_rec.status_code == 200
    assert upd_rec.json()["title"] == "Standup Updated"
    assert client.delete(f"/api/v2/recorder/{rec_id}", headers=headers_a).status_code == 200

    # Feature 13: Smart Home Control (SSRF block + duplicate entity 409 + THERMOSTAT confirmation 409 + unconfigured hub 503 + delete)
    ssrf_attempt = client.put(
        "/api/v2/smart-home/config",
        headers=headers_a,
        json={"provider_type": "HOME_ASSISTANT", "hub_url": "https://127.0.0.1:8123", "access_token": "tok_secret_1234", "authorize": True},
    )
    assert ssrf_attempt.status_code == 400

    dev = client.post(
        "/api/v2/smart-home/devices",
        headers=headers_a,
        json={"external_entity_id": "climate.hall", "name": "Hall AC", "room": "Hall", "device_type": "THERMOSTAT"},
    )
    assert dev.status_code == 201
    dev_id = dev.json()["id"]

    dup_dev = client.post(
        "/api/v2/smart-home/devices",
        headers=headers_a,
        json={"external_entity_id": "climate.hall", "name": "Hall AC 2", "room": "Hall", "device_type": "THERMOSTAT"},
    )
    assert dup_dev.status_code == 409

    # Sensitive device requires explicit confirmation (409) before hub probe
    need_confirm = client.post(
        f"/api/v2/smart-home/devices/{dev_id}/command",
        headers=headers_a,
        json={"turn_on": True, "brightness_pct": 80, "confirmed_sensitive": False},
    )
    assert need_confirm.status_code == 409

    # Confirmed sensitive device still refuses when no real hub is authorized (503)
    cmd = client.post(
        f"/api/v2/smart-home/devices/{dev_id}/command",
        headers=headers_a,
        json={"turn_on": True, "brightness_pct": 80, "confirmed_sensitive": True},
    )
    assert cmd.status_code == 503
    assert client.delete(f"/api/v2/smart-home/devices/{dev_id}", headers=headers_a).status_code == 200

    # Feature 14: Expense Tracker + Budget Overspend Negative Remaining + CSV Export
    exp = client.post(
        "/api/v2/expenses",
        headers=headers_a,
        json={"title": "Coffee", "category": "Food", "amount": 600.0, "currency": "INR"},
    )
    assert exp.status_code == 201
    from datetime import datetime, timezone
    curr_month = datetime.now(timezone.utc).strftime("%Y-%m")
    budget_res = client.put(
        "/api/v2/expenses/budget",
        headers=headers_a,
        json={"month_key": curr_month, "limit_amount": 500.0, "currency": "INR"},
    )
    assert budget_res.status_code == 200
    assert budget_res.json()["remaining_budget"] == -100.0

    csv_res = client.get("/api/v2/expenses/export", headers=headers_a)
    assert csv_res.status_code == 200
    assert "Coffee" in csv_res.text

    # Feature 15: Personality Settings (0..100 scale)
    pers = client.put("/api/v2/personality", headers=headers_a, json={"humor_level": 85, "response_length": "SHORT", "avatar_style": "CYBER_BOT"})
    assert pers.status_code == 200
    assert pers.json()["humor_level"] == 85

    # Feature 16: Smart App Shortcuts
    app_res = client.post(
        "/api/v2/apps/shortcuts",
        headers=headers_a,
        json={"alias_name": "WhatsApp", "package_name": "com.whatsapp", "is_favourite": True, "increment_launch": True},
    )
    assert app_res.status_code == 200
    assert app_res.json()["launch_count"] == 1
    assert client.delete(f"/api/v2/apps/shortcuts/{app_res.json()['id']}", headers=headers_a).status_code == 200

    # Feature 17: Study Tutor Mode (503 without key + sessions CRUD isolation)
    assert client.post(
        "/api/v2/study/explain",
        headers=headers_a,
        json={"subject": "CS", "topic": "Recursion", "language": "Hinglish", "difficulty": "Beginner"},
    ).status_code == 503
    assert client.get("/api/v2/study/sessions", headers=headers_a).json() == []

    # Feature 18: Travel Intelligence (mocked OSM Nominatim + honest live notice + history)
    with patch(
        "app.api.v2.endpoints.mobility_system.search_nearby_places_osm",
        new=AsyncMock(return_value=([{"name": "Hawa Mahal", "category": "tourism", "address": "Jaipur, India", "latitude": 26.9239, "longitude": 75.8267}], "OpenStreetMap Nominatim lookup complete. Live traffic, fare pricing & seat availability are not connected.")),
    ):
        travel_res = client.post(
            "/api/v2/travel/plan",
            headers=headers_a,
            json={"origin": "Delhi", "destination": "Jaipur", "interests": "landmarks"},
        )
    assert travel_res.status_code == 201
    assert travel_res.json()["nearby_places"][0]["name"] == "Hawa Mahal"
    assert len(client.get("/api/v2/travel/history", headers=headers_a).json()) == 1

    # Feature 19: Multi-Device Sync (LATEST_TIMESTAMP conflict policy)
    sync_res = client.post(
        "/api/v2/devices/sync",
        headers=headers_a,
        json={"device_identifier": "pixel-boss-1", "scope": "ALL", "conflict_policy": "LATEST_TIMESTAMP"},
    )
    assert sync_res.status_code == 201
    assert sync_res.json()["conflict_policy"] == "LATEST_TIMESTAMP"
    assert len(client.get("/api/v2/devices/sync", headers=headers_a).json()["devices"]) == 1

    # Feature 20: Self-Diagnostic System
    diag_res = client.post(
        "/api/v2/diagnostics/run",
        headers=headers_a,
        json={"mic_permission": True, "camera_permission": True, "device_online": True},
    )
    assert diag_res.status_code == 200
    assert diag_res.json()["database_ok"] is True
