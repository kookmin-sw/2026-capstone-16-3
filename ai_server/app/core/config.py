from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
UPLOAD_DIR = BASE_DIR / "uploads"
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

BACKEND_GUIDE_EVENT_URL = "http://127.0.0.1:8080/api/guide/event"
AI_TIMEOUT_SECONDS = 1.0