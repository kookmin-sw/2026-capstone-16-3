from pathlib import Path
import os
from dotenv import load_dotenv

load_dotenv()

# app/core/config.py 기준 -> AI/ 루트
BASE_DIR = Path(__file__).resolve().parent.parent.parent

UPLOAD_DIR = BASE_DIR / "uploads"
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

# 기본값은 프로젝트 루트 기준 상대경로
YOLO_MODEL_PATH = Path(
    os.getenv("YOLO_MODEL_PATH", str(BASE_DIR / "models" / "yolo" / "best.pt"))
)

SEG_MODEL_DIR = Path(
    os.getenv("SEG_MODEL_DIR", str(BASE_DIR / "models" / "segformer"))
)

BACKEND_GUIDE_EVENT_URL = os.getenv(
    "BACKEND_GUIDE_EVENT_URL",
    "http://127.0.0.1:8080/api/guide/event"
)

AI_TIMEOUT_SECONDS = float(
    os.getenv("AI_TIMEOUT_SECONDS", "1.0")
)