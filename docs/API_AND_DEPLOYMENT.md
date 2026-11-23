# GOLU — API Reference, Release & Deployment Guide (Phases 1–15)

## 1. Interactive OpenAPI Documentation
When the FastAPI backend is running in development/docs mode (`ENVIRONMENT != production`):
- Swagger UI: `https://your-domain.com/docs`
- ReDoc: `https://your-domain.com/redoc`

---

## 2. Complete Backend API v1 Endpoints

### Phase 1–6 (Core, Auth, Voice, Hybrid AI)
- `GET /api/v1/health` — DB `SELECT 1` & Redis liveness check (503 if DB down)
- `GET /api/v1/health/database` — PostgreSQL latency, dialect, pool & Redis connectivity check
- `GET /api/v1/status` — Version, PostgreSQL status, Redis status, OpenAI & SMTP readiness
- `GET /api/v1/config` — Languages, wake phrase, capability flags
- `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, `POST /api/v1/auth/forgot-password`, `POST /api/v1/auth/reset-password`, `POST /api/v1/auth/verify-email/request`, `POST /api/v1/auth/verify-email`, `GET /api/v1/auth/me`
- `GET/PUT /api/v1/users/me` and `GET/PUT /api/v1/profile`
- `GET/PUT /api/v1/voice/settings`, `GET/PUT /api/v1/assistant/settings`, `GET/PUT /api/v1/assistant/wake-word/settings`
- `POST /api/v1/ai/chat`, `POST /api/v1/ai/intent`, `GET /api/v1/ai/status`, `POST /api/v1/ai/context/clear`, `GET/DELETE /api/v1/conversations` (and `/api/v1/ai/conversations`)
- `GET/POST /api/v1/devices`, `DELETE /api/v1/devices/{device_id}`
- `GET/POST /api/v1/notifications`, `POST /api/v1/notifications/{notification_id}/read`, `DELETE /api/v1/notifications/{notification_id}`

### Phase 7 — Smart Personal Memory
- `POST /api/v1/memory` — Requires `explicit_user_confirmed: true`; rejects secrets/passwords/tokens.
- `GET /api/v1/memory?category=...`, `GET /api/v1/memory/audit`, `GET/PUT/DELETE /api/v1/memory/{memory_id}`, `DELETE /api/v1/memory`

### Phase 8 & 9 — Phone & WhatsApp Messaging Preferences
- `GET /PUT /api/v1/phone/preferences`
- `GET /POST /api/v1/phone/contact-aliases`, `GET/DELETE /api/v1/phone/contact-aliases/{alias_id}`
- `GET/POST/DELETE /api/v1/phone/app-preferences`
- `GET /PUT /api/v1/messaging/preferences`, `GET/POST/DELETE /api/v1/messaging/templates`

### Phase 10 — Daily Life Assistant
- `GET /POST /api/v1/reminders`, `PUT /DELETE /api/v1/reminders/{id}`
- `GET /POST /api/v1/tasks`, `PUT /DELETE /api/v1/tasks/{id}`
- `GET /POST /api/v1/timetable`, `PUT /DELETE /api/v1/timetable/{id}`
- `GET /POST /api/v1/calendar/events`, `PUT /DELETE /api/v1/calendar/events/{id}`
- `GET /POST /api/v1/daily-life/routines`, `GET/PUT /api/v1/daily-life/notifications`

### Phase 11 — Family Mode
- `GET /api/v1/family/dashboard`, `POST /api/v1/family/groups`, `POST /api/v1/family/invitations`, `POST /api/v1/family/invitations/accept`, `PUT /api/v1/family/members/{member_id}`, `POST/DELETE /api/v1/family/shared-reminders`

### Phase 12 — Complete Security System
- `GET /api/v1/security/dashboard`, `PUT /api/v1/security/settings`, `POST /api/v1/security/sessions/{session_id}/revoke`, `DELETE /api/v1/security/personal-data`

### Phase 13 — Smart Automation
- `GET /POST /api/v1/automations` (and `/api/v1/automation`), `GET /api/v1/automations/history`, `GET/PUT/DELETE /api/v1/automations/{rule_id}`, `POST /api/v1/automations/{rule_id}/enable|disable|logs`

---

## 3. PostgreSQL, Redis & Alembic Migrations (`0001` → `0004_devices_notifications_and_verification`)
```bash
cd backend
cp .env.example .env
# Populate DATABASE_URL, REDIS_URL, SECRET_KEY, JWT_SECRET_KEY, OPENAI_API_KEY
pip install -r requirements.txt
alembic upgrade head
python3 scripts/verify_db.py
pytest -v
```

## 4. Production HTTPS, Nginx & Automatic Android Connection
- Deploy `/backend` via Docker (`backend/Dockerfile` + `docker-compose.yml`), Cloud Run + Cloud SQL, Render, Fly.io, or VPS behind Nginx TLS (`backend/deploy/nginx.conf`).
- Configure the central HTTPS backend URL in root `.env` (`GOLU_BACKEND_URL=https://.../`) so the Android app connects automatically via `BuildConfig.GOLU_BACKEND_URL` and `BackendConnectionManager`.
- **Backup & Restore:**
  ```bash
  DATABASE_URL="postgresql://golu_user:PASS@host:5432/golu_db" ./backend/scripts/backup_postgres.sh ./backups
  pg_restore --no-owner --dbname="$DATABASE_URL" ./backups/golu_backup_YYYYMMDD_HHMMSS.dump
  ```

---

## 5. Android Release Build & Play Store Readiness Checklist
- **Application ID:** `com.aistudio.golu.xkmqzp` (`versionCode = 2`, `versionName = "1.1.0"`)
- **Build Commands:**
  ```bash
  gradle :app:testDebugUnitTest
  gradle assembleDebug
  gradle assembleRelease
  gradle bundleRelease
  ```
- **Google Play Policy Compliance Verified:** `ACTION_DIAL` (not `CALL_PHONE`), `WorkManager`, `https://wa.me/...` + `ACTION_SEND` after explicit confirmation, zero broad storage/SMS/package-query permissions.
