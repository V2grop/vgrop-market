# Optional FastAPI/PostgreSQL research service
Not deployed automatically. Requires Python3.11+, PostgreSQL, and TLS reverse proxy.

1. Create a dedicated PostgreSQL database/user; apply schema.sql with psql.
2. Install requirements in a venv. Set DATABASE_URL and VGROP_ACCESS_TOKEN (random >=32 characters) in a protected environment file; never APK or git.
3. Start `uvicorn app:app --host 127.0.0.1 --port 8080` from this folder, behind HTTPS Caddy/nginx.
4. Run `python collect.py`; VGROP_SYMBOLS defaults to BTCUSDT,ETHUSDT. The collector only implements Binance spot and updates last1000 bars. Historical pagination and other providers remain future work.
5. Optional systemd collector service/timer templates in ../deploy; adjust paths and create the unprivileged vgrop user. They are not installed by this task.

API: /health, authenticated /v1/capabilities, /v1/instruments, /v1/candles/{id}. OpenAPI /docs describes schemas. received_at is ingestion time, open_time is exchange candle time. Treat an empty/stale response as unavailable.
This API is separate from the legacy optional AI gateway ../server.py; Android's existing AI settings use that gateway, not the v2 storage API yet. No trained-model inference endpoint is advertised. Bearer access is service-level, not per-user authentication. OIDC, per-user authorization, rate limits, monitoring, migrations and deployment testing remain production gates.
