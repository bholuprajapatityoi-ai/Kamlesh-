# GOLU — Full-Stack Android AI Voice Assistant (Phases 1–15)

GOLU is a personal Android AI voice assistant (Kotlin + Jetpack Compose + Material 3 + WorkManager + Keystore) paired with a Python FastAPI + PostgreSQL + Alembic + OpenAI backend.

## Implemented Phases (1–15)
- **Phases 1–3:** Modular Android MVVM architecture, FastAPI + PostgreSQL + Alembic migrations, Argon2 auth & JWT/refresh rotation with Android Keystore token vault.
- **Phases 4–6:** Real `SpeechRecognizer` & `TextToSpeech` (Hindi/Hinglish/English), `"Hey Golu"` foreground wake-word engine (opt-in), Hybrid AI Router (Offline Command Engine + Android Actions + OpenAI backend).
- **Phase 7 (Smart Personal Memory):** Explicit-only saving (`"GOLU, ye yaad rakhna..."`), secret/password rejection, audit trail, category management, clear all.
- **Phases 8–9 (Phone Control & WhatsApp Assistant):** Volume, media playback, quick settings, runtime permission-gated Contacts + nickname aliases (`Papa`, `Mummy`, `Brother`, `Sister`), call confirmation via `ACTION_DIAL`, editable Hindi/Hinglish/English WhatsApp drafts with explicit confirmation before opening WhatsApp/Share.
- **Phase 10 (Daily Life Assistant):** Reminders (`WorkManager` + Done/Snooze notifications), Tasks, Timetable, Calendar Events, Routines, Morning/Evening briefings, truthful exact-alarm check.
- **Phase 11 (Family Mode):** Family groups, invite codes, separate accounts, strict private memory/conversation isolation, and **Papa / Elder-Friendly Interface**.
- **Phase 12 (Complete Security System):** Keystore-encrypted 4–6 digit App PIN lock, active session revocation, security event log, permission controls, personal data wipe.
- **Phase 13 (Smart Automation):** Safe Trigger → Condition → Action builder, enable/disable/edit/delete/test-run, execution history.
- **Phases 14–15:** Robolectric unit tests (`app/src/test`), FastAPI `pytest` suites (`backend/tests`), and full documentation (`/docs/USER_GUIDE.md`, `/docs/API_AND_DEPLOYMENT.md`).

---

## Quick Start

### Android
```bash
gradle :app:testDebugUnitTest
gradle assembleDebug
```
For release builds (requires `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`):
```bash
gradle assembleRelease
gradle bundleRelease
```

### Backend (`/backend`)
```bash
cd backend
cp .env.example .env
pip install -r requirements.txt
alembic upgrade head
python3 scripts/verify_db.py
pytest -v
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

See `/docs/USER_GUIDE.md` and `/docs/API_AND_DEPLOYMENT.md` for complete API examples, deployment instructions, backup/recovery, and troubleshooting.
