"""AK SUPER AGENT AI backend — Stage 2 skeleton.

Only /health exists so far. Auth, billing and chat arrive in Stages 3–5.
No secrets live in this file; configuration comes from environment variables.
"""
import os
import time

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

APP_VERSION = "0.2.0-stage2"
STARTED_AT = time.time()

app = FastAPI(title="AK SUPER AGENT AI API", version=APP_VERSION)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[o for o in os.getenv("CORS_ORIGINS", "").split(",") if o],
    allow_methods=["GET", "POST"],
    allow_headers=["Authorization", "Content-Type"],
)


def database_status() -> str:
    """Report the database honestly: not configured until Stage 3 adds PostgreSQL."""
    if not os.getenv("DATABASE_URL"):
        return "not_configured"
    return "configured_unchecked"


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "service": "ak-super-agent-api",
        "version": APP_VERSION,
        "uptime_seconds": int(time.time() - STARTED_AT),
        "database": database_status(),
    }
