from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_ok():
    r = client.get("/health")
    assert r.status_code == 200
    body = r.json()
    assert body["status"] == "ok"
    assert body["version"] == "0.2.0-stage2"
    assert body["database"] in {"not_configured", "configured_unchecked"}


def test_unknown_route_is_404():
    assert client.get("/owner/dashboard").status_code == 404


def test_health_rejects_post():
    assert client.post("/health").status_code == 405
