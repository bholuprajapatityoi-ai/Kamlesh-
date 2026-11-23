# GOLU Production Deployment Checklist

## Option A: One-Click Managed HTTPS Hosting (Render Blueprint)
1. Push this repository (including `/backend/render.yaml`) to GitHub.
2. In Render Dashboard, choose **New -> Blueprint** and select your repository.
3. Render automatically provisions:
   - **golu-postgres** (PostgreSQL 16)
   - **golu-redis** (Redis 7)
   - **golu-api** (FastAPI Docker service with automatic `alembic upgrade head` & TLS HTTPS)
   - **golu-worker** (Background notification & session cleanup worker)
4. Add your `OPENAI_API_KEY` in Render service environment variables.
5. Note your real HTTPS URL (e.g., `https://golu-api.onrender.com`).

## Option B: Docker Compose + Nginx TLS (VPS / Cloud VM)
1. Provision an Ubuntu/Debian server with Docker, Docker Compose, and a domain pointing to the server IP.
2. Generate strong secrets:
   ```bash
   python3 backend/scripts/generate_secrets.py
   ```
3. Copy `backend/.env.example` to `backend/.env` and set `ENVIRONMENT=production`, `DATABASE_URL`, `REDIS_URL`, `SECRET_KEY`, `JWT_SECRET_KEY`, and `OPENAI_API_KEY`.
4. Start services and verify schema + tests:
   ```bash
   cd backend
   docker compose up -d --build
   docker compose exec backend alembic upgrade head
   docker compose exec backend python scripts/verify_db.py
   docker compose exec backend pytest -v
   ```
5. Configure Nginx HTTPS (`backend/deploy/nginx.conf`) with Let's Encrypt (`certbot --nginx`) to terminate TLS 1.2/1.3.

## Verify Live Endpoints
```bash
curl -fsS https://<your-real-domain>/api/v1/health
curl -fsS https://<your-real-domain>/api/v1/health/database
curl -fsS https://<your-real-domain>/api/v1/status
```

## Connect Android Production Build (Automatic, Zero Manual URL UI)
1. Set `GOLU_BACKEND_URL=https://<your-real-domain>/` in the root `.env` (or AI Studio Secrets panel).
2. Rebuild the Android APK/AAB. `BuildConfig.GOLU_BACKEND_URL` automatically feeds `BackendConnectionManager`, verifying `/api/v1/health` and `/api/v1/health/database` without any manual URL entry in the UI.
