from pathlib import Path
import os
from dotenv import load_dotenv

load_dotenv()

BASE_DIR = Path(__file__).resolve().parent.parent.parent

UPLOAD_DIR = BASE_DIR / "uploads"
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

# =========================================================
# Model paths
# =========================================================
YOLO_DET_WEIGHTS = Path(
    os.getenv("YOLO_DET_WEIGHTS", str(BASE_DIR / "models" / "detection" / "bestv3.pt"))
)

YOLO_SEG_WEIGHTS = Path(
    os.getenv("YOLO_SEG_WEIGHTS", str(BASE_DIR / "models" / "segmentation" / "best_yoloseg.pt"))
)

DEPTH_MODEL_NAME = os.getenv(
    "DEPTH_MODEL_NAME",
    "depth-anything/Depth-Anything-V2-Metric-Outdoor-Small-hf",
)

# Backward-compatible aliases. main.py에서 기존 이름을 쓰고 있으면 깨지지 않게 유지.
YOLO_MODEL_PATH = YOLO_DET_WEIGHTS
SEG_MODEL_DIR = YOLO_SEG_WEIGHTS

# =========================================================
# Inference hyperparameters
# =========================================================
IMG_SIZE = int(os.getenv("IMG_SIZE", "480"))
CONF_DET = float(os.getenv("CONF_DET", "0.25"))
CONF_SEG = float(os.getenv("CONF_SEG", "0.25"))
MIN_GUIDE_CONF = float(os.getenv("MIN_GUIDE_CONF", "0.45"))

# Distance thresholds, meter
URGENT_DISTANCE_M = float(os.getenv("URGENT_DISTANCE_M", "5.0"))
ALERT_DISTANCE_M = float(os.getenv("ALERT_DISTANCE_M", "9.0"))
MAX_GUIDE_DISTANCE_M = float(os.getenv("MAX_GUIDE_DISTANCE_M", "13.0"))
NEAR_M = float(os.getenv("NEAR_M", "5.0"))
MID_M = float(os.getenv("MID_M", "9.0"))

# Huge bbox sanity
HUGE_BBOX_AREA_RATIO = float(os.getenv("HUGE_BBOX_AREA_RATIO", "0.20"))
HUGE_BBOX_MAX_DIST = float(os.getenv("HUGE_BBOX_MAX_DIST", "7.0"))
HUGE_BBOX_SIZE_BONUS_MAX_DIST = float(os.getenv("HUGE_BBOX_SIZE_BONUS_MAX_DIST", "4.0"))

# Vehicle approach
VEHICLE_APPROACH_THRESH = float(os.getenv("VEHICLE_APPROACH_THRESH", "0.4"))
VEHICLE_STOP_DISTANCE = float(os.getenv("VEHICLE_STOP_DISTANCE", "6.0"))
VEHICLE_MIN_TRACK_AGE = int(os.getenv("VEHICLE_MIN_TRACK_AGE", "5"))

# Motion / tracking
MOTION_PIXEL_THRESHOLD = float(os.getenv("MOTION_PIXEL_THRESHOLD", "8.0"))
TRACK_HISTORY_LEN = int(os.getenv("TRACK_HISTORY_LEN", "8"))

# Cooldown
DYNAMIC_OBJ_COOLDOWN_FRAMES = int(os.getenv("DYNAMIC_OBJ_COOLDOWN_FRAMES", "56"))
STATIC_OBJ_COOLDOWN_FRAMES = int(os.getenv("STATIC_OBJ_COOLDOWN_FRAMES", "84"))
SCENE_INFO_COOLDOWN_FRAMES = int(os.getenv("SCENE_INFO_COOLDOWN_FRAMES", "84"))

# Depth sanity
SMALL_BBOX_HEIGHT_RATIO = float(os.getenv("SMALL_BBOX_HEIGHT_RATIO", "0.15"))
SMALL_BBOX_MIN_TRUST_DIST = float(os.getenv("SMALL_BBOX_MIN_TRUST_DIST", "8.0"))
ROAD_OVERLAP_VEHICLE_PENALTY_MIN_DIST = float(
    os.getenv("ROAD_OVERLAP_VEHICLE_PENALTY_MIN_DIST", "4.0")
)

BACKEND_GUIDE_EVENT_URL = os.getenv(
    "BACKEND_GUIDE_EVENT_URL",
    "http://127.0.0.1:8080/api/guide/event",
).rstrip("/")

if not BACKEND_GUIDE_EVENT_URL.endswith("/api/guide/event"):
    BACKEND_GUIDE_EVENT_URL += "/api/guide/event"

AI_TIMEOUT_SECONDS = float(os.getenv("AI_TIMEOUT_SECONDS", "1.0"))

print("BACKEND_GUIDE_EVENT_URL =", BACKEND_GUIDE_EVENT_URL)
print("YOLO_DET_WEIGHTS =", YOLO_DET_WEIGHTS)
print("YOLO_SEG_WEIGHTS =", YOLO_SEG_WEIGHTS)
print("DEPTH_MODEL_NAME =", DEPTH_MODEL_NAME)
