# GOLU FastAPI, PostgreSQL & Redis Backend (Phases 1–15)

Production-oriented Python FastAPI + PostgreSQL + SQLAlchemy + Alembic (`0001` → `0004_devices_notifications_and_verification`) + Redis + OpenAI backend for the **GOLU** Android AI Assistant.

---

## 1. PostgreSQL & Redis Setup

```sql
CREATE USER golu_user WITH PASSWORD 'STRONG_PASSWORD_HERE';
CREATE DATABASE golu_db OWNER golu_user;
```

Or start PostgreSQL + Redis + FastAPI + Worker locally with Docker Compose:

```bash
cd backend
cp .env.example .env
docker compose up --build
```

---

## 2. Environment Configuration

Copy `.env.example` to `.env` and configure all required secrets (never commit `.env`):

- `DATABASE_URL` — PostgreSQL connection URI (`postgresql+psycopg://user:pass@host:5432/golu_db`)
- `REDIS_URL` — Redis URL (`redis://localhost:6379/0`; optional fail-safe fallback if unset)
- `SECRET_KEY` — 64-char random app secret
- `JWT_SECRET_KEY` — 64-char random JWT signing secret
- `AI_PROVIDER` / `OPENAI_API_KEY` / `OPENAI_MODEL` — Server-side AI credentials (never shipped in Android APK)
- `CORS_ALLOWED_ORIGINS` — Comma-separated allowed origins
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM_EMAIL` — Required for password reset & email verification emails (if blank, email endpoints report `delivery_configured: false` and never fake delivery)

---

## 3. Alembic Migrations

```bash
cd backend
alembic current
alembic upgrade head
python3 scripts/verify_db.py
```

Migration `0004_devices_notifications_and_verification` adds `users.email_verified_at`, `email_verification_tokens`, `device_registrations`, and `notification_records`.

---

## 4. Database Backup & Recovery

```bash
DATABASE_URL="postgresql://golu_user:PASS@host:5432/golu_db" ./scripts/backup_postgres.sh ./backups
pg_restore --no-owner --dbname="$DATABASE_URL" ./backups/golu_backup_YYYYMMDD_HHMMSS.dump
```

---

## 5. Running Server, Worker & Automated Tests

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Run all unit, integration, and security tests
pytest -v

# Start development server
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# Start production server behind Nginx HTTPS proxy (backend/deploy/nginx.conf)
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4

# Start background notification & session cleanup worker
python -m app.workers.background_worker
```

---

## 6. External Hosting, HTTPS & Automatic Android Connection

Deploy `/backend` to Render, Railway, Fly.io, Google Cloud Run + Cloud SQL, or an Ubuntu VPS with Nginx (`backend/deploy/nginx.conf`) + Let's Encrypt TLS.

Configure the deployed HTTPS URL centrally in the root project `.env` (`GOLU_BACKEND_URL=https://your-domain.example.com/`). The Android client reads `BuildConfig.GOLU_BACKEND_URL` and connects automatically with `BackendConnectionManager` (no manual URL input exists in the app UI).

---

## 7. API Endpoints (v1)

- **System:** `GET /api/v1/health`, `GET /api/v1/health/database`, `GET /api/v1/status`, `GET /api/v1/config`
- **Auth & Users:** `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, `POST /api/v1/auth/forgot-password`, `POST /api/v1/auth/reset-password`, `POST /api/v1/auth/verify-email/request`, `POST /api/v1/auth/verify-email`, `GET /api/v1/auth/me`, `GET/PUT /api/v1/users/me`, `GET/PUT /api/v1/profile`
- **Voice & Wake-Word:** `GET/PUT /api/v1/voice/settings`, `GET/PUT /api/v1/assistant/settings`, `GET/PUT /api/v1/assistant/wake-word/settings`
- **Hybrid AI & Conversations:** `POST /api/v1/ai/chat`, `POST /api/v1/ai/intent`, `GET /api/v1/ai/status`, `POST /api/v1/ai/context/clear`, `GET/DELETE /api/v1/conversations` (and `/api/v1/ai/conversations`)
- **Devices & Notifications:** `GET/POST /api/v1/devices`, `DELETE /api/v1/devices/{id}`, `GET/POST /api/v1/notifications`, `POST /api/v1/notifications/{id}/read`, `DELETE /api/v1/notifications/{id}`
- **Personal Memory:** `POST/GET/DELETE /api/v1/memory`, `GET /api/v1/memory/audit`, `GET/PUT/DELETE /api/v1/memory/{id}`
- **Phone & WhatsApp:** `GET/PUT /api/v1/phone/preferences`, `GET/POST/DELETE /api/v1/phone/contact-aliases`, `GET/PUT /api/v1/messaging/preferences`, `GET/POST/DELETE /api/v1/messaging/templates`
- **Daily Life:** `/api/v1/reminders`, `/api/v1/tasks`, `/api/v1/timetable`, `/api/v1/calendar/events`, `/api/v1/daily-life/routines`, `/api/v1/daily-life/notifications`
- **Family Mode:** `GET /api/v1/family/dashboard`, `/api/v1/family/groups`, `/api/v1/family/invitations`, `/api/v1/family/members/{id}`, `/api/v1/family/shared-reminders`
- **Security:** `GET /api/v1/security/dashboard`, `PUT /api/v1/security/settings`, `POST /api/v1/security/sessions/{id}/revoke`, `DELETE /api/v1/security/personal-data`
- **Smart Automation:** `GET/POST /api/v1/automations` (and `/api/v1/automation`), `GET /api/v1/automations/history`, `GET/PUT/DELETE /api/v1/automations/{id}`, `POST /api/v1/automations/{id}/enable|disable|logs`
