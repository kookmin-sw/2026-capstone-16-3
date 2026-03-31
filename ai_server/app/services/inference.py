import cv2
import numpy as np
import torch
import torch.nn.functional as F

from PIL import Image
from ultralytics import YOLO
from transformers import AutoImageProcessor, AutoModelForSemanticSegmentation

# =========================================================
# 기본 설정
# =========================================================
IMGSZ = 640
CONF = 0.25

device = "cuda" if torch.cuda.is_available() else "cpu"

# 전역 모델 객체
yolo = None
processor = None
seg = None

# =========================================================
# 클래스 정규화 / 위험도 맵
# =========================================================
YOLO_CANONICAL_MAP = {
    "bicycle": "bicycle",
    "bus": "bus",
    "car": "car",
    "motorcycle": "motorcycle",
    "scooter": "scooter",
    "truck": "truck",
    "person": "person",
    "wheelchair": "wheelchair",
    "barricade": "barricade",
    "bench": "bench",
    "bollard": "bollard",
    "fire_hydrant": "fire_hydrant",
    "pole": "pole",
    "stop": "stop",
    "table": "table",
    "traffic_light": "traffic_light",
    "tree_trunk": "tree_trunk",
}

SEVERITY_MAP = {
    "bicycle": 2,
    "bus": 3,
    "car": 3,
    "motorcycle": 3,
    "scooter": 3,
    "truck": 3,
    "person": 2,
    "wheelchair": 2,
    "barricade": 2,
    "bench": 2,
    "bollard": 2,
    "fire_hydrant": 2,
    "pole": 2,
    "stop": 2,
    "table": 2,
    "traffic_light": 1,
    "tree_trunk": 2,
}

# =========================================================
# 기본 유틸
# =========================================================
def canonicalize_yolo_name(name: str) -> str:
    return YOLO_CANONICAL_MAP.get(name, name)


def default_motion_for_class(cls_name: str) -> str:
    """
    현재는 single-frame 기준이라 motion을 안정적으로 추정하기 어려움.
    우선 기본값으로 채우고, 나중에 user별 이전 프레임 비교로 개선 가능.
    """
    if cls_name in {"bicycle", "bus", "car", "motorcycle", "scooter", "truck"}:
        return "parked"
    if cls_name in {"person", "wheelchair"}:
        return "static"
    return "static"


def motion_risk_value(motion: str) -> int:
    if motion in {"moving", "approaching"}:
        return 2
    return 0


def immediacy_value(distance: str) -> int:
    if distance == "near":
        return 3
    if distance == "mid":
        return 2
    return 1


def infer_distance(x1: float, y1: float, x2: float, y2: float, width: int, height: int) -> str:
    """
    bbox 면적 비율 기반 단순 거리 추정
    """
    area_ratio = ((x2 - x1) * (y2 - y1)) / float(width * height)

    if area_ratio >= 0.10:
        return "near"
    if area_ratio >= 0.03:
        return "mid"
    return "far"


def infer_clock_direction(cx: float, width: int) -> str:
    ratio = cx / float(width)

    if ratio < 0.2:
        return "10시"
    if ratio < 0.4:
        return "11시"
    if ratio < 0.6:
        return "12시"
    if ratio < 0.8:
        return "1시"
    return "2시"


def infer_h_region(cx: float, width: int) -> str:
    ratio = cx / float(width)

    if ratio < 0.33:
        return "left"
    if ratio < 0.66:
        return "center"
    return "right"


def estimate_on_path(h_region: str, distance: str) -> bool:
    """
    매우 단순한 1차 휴리스틱:
    - 중앙(center)이고
    - near/mid면 경로상으로 간주
    """
    return h_region == "center" and distance in {"near", "mid"}


def estimate_narrows_path(h_region: str, distance: str) -> bool:
    """
    좌/우에 있더라도 near/mid면 통로를 좁힐 수 있다고 간단 판단
    """
    return h_region in {"left", "right", "center"} and distance in {"near", "mid"}


# =========================================================
# 모델 로드
# =========================================================
def load_models(yolo_pt: str, seg_model_dir: str) -> None:
    global yolo, processor, seg

    yolo = YOLO(yolo_pt)
    processor = AutoImageProcessor.from_pretrained(seg_model_dir, reduce_labels=False)
    seg = AutoModelForSemanticSegmentation.from_pretrained(seg_model_dir).to(device).eval()


def is_model_loaded() -> bool:
    return yolo is not None and processor is not None and seg is not None


# =========================================================
# SegFormer 추론
# =========================================================
@torch.inference_mode()
def seg_infer(img_pil: Image.Image) -> np.ndarray:
    if seg is None or processor is None:
        raise RuntimeError("SegFormer 모델이 로드되지 않았습니다.")

    inputs = processor(images=img_pil, return_tensors="pt").to(device)
    outputs = seg(**inputs)

    logits_up = F.interpolate(
        outputs.logits,
        size=img_pil.size[::-1],   # (H, W)
        mode="bilinear",
        align_corners=False,
    )

    pred = logits_up.argmax(dim=1)[0].detach().cpu().numpy().astype(np.int32)
    return pred


# =========================================================
# YOLO 추론
# =========================================================
def run_yolo_detect(frame_bgr: np.ndarray):
    if yolo is None:
        raise RuntimeError("YOLO 모델이 로드되지 않았습니다.")

    result = yolo.predict(
        source=frame_bgr,
        imgsz=IMGSZ,
        conf=CONF,
        verbose=False
    )[0]

    det_rows = []

    if result.boxes is None or len(result.boxes) == 0:
        return result, det_rows

    boxes = result.boxes.xyxy.detach().cpu().numpy()
    confs = result.boxes.conf.detach().cpu().numpy()
    clss = result.boxes.cls.detach().cpu().numpy().astype(int)

    height, width = frame_bgr.shape[:2]

    for box, conf, cls_id in zip(boxes, confs, clss):
        cls_name = yolo.names[int(cls_id)]
        canonical = canonicalize_yolo_name(cls_name)

        x1, y1, x2, y2 = box.tolist()
        cx = (x1 + x2) / 2.0

        distance = infer_distance(x1, y1, x2, y2, width, height)
        clock_direction = infer_clock_direction(cx, width)
        h_region = infer_h_region(cx, width)

        motion = default_motion_for_class(canonical)
        severity = SEVERITY_MAP.get(canonical, 1)
        immediacy = immediacy_value(distance)
        motion_risk = motion_risk_value(motion)

        on_path = estimate_on_path(h_region, distance)
        narrows_path = estimate_narrows_path(h_region, distance)

        guide_priority = severity + immediacy + motion_risk
        if on_path:
            guide_priority += 1
        if narrows_path:
            guide_priority += 1

        det_rows.append({
            "class": canonical,
            "confidence": float(conf),
            "bbox": [float(x1), float(y1), float(x2), float(y2)],
            "motion": motion,
            "h_region": h_region,
            "clock_direction": clock_direction,
            "distance": distance,
            "on_path": on_path,
            "narrows_path": narrows_path,
            "blocks_tactile": False,
            "severity": severity,
            "immediacy": immediacy,
            "motion_risk": motion_risk,
            "guide_priority": int(guide_priority),
        })

    return result, det_rows


# =========================================================
# 최종 단일 이미지 추론
# =========================================================
def run_single_image_inference(frame_bgr: np.ndarray):
    """
    반환:
    - yolo_result
    - seg_pred (H, W) np.ndarray
    - det_rows (list[dict])
    """
    if not is_model_loaded():
        raise RuntimeError("모델이 아직 로드되지 않았습니다.")

    img_pil = Image.fromarray(cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB))

    yolo_result, det_rows = run_yolo_detect(frame_bgr)
    seg_pred = seg_infer(img_pil)

    return yolo_result, seg_pred, det_rows