from __future__ import annotations

from collections import defaultdict, deque
from typing import Any

import cv2
import numpy as np

from app.core.config import (
    ALERT_DISTANCE_M,
    DEPTH_SCALE_FACTOR,    
    DEPTH_OFFSET_M,        
    DYNAMIC_OBJ_COOLDOWN_FRAMES,
    HUGE_BBOX_AREA_RATIO,
    HUGE_BBOX_MAX_DIST,
    HUGE_BBOX_SIZE_BONUS_MAX_DIST,
    MAX_GUIDE_DISTANCE_M,
    MID_M,
    MIN_GUIDE_CONF,
    MOTION_PIXEL_THRESHOLD,
    NEAR_M,
    ROAD_OVERLAP_VEHICLE_PENALTY_MIN_DIST,
    SCENE_INFO_COOLDOWN_FRAMES,
    SMALL_BBOX_HEIGHT_RATIO,
    SMALL_BBOX_MIN_TRUST_DIST,
    STATIC_OBJ_COOLDOWN_FRAMES,
    TRACK_HISTORY_LEN,
    URGENT_DISTANCE_M,
    VEHICLE_APPROACH_THRESH,
    VEHICLE_MIN_TRACK_AGE,
    VEHICLE_STOP_DISTANCE,
)

# =========================================================
# Label / category maps
# =========================================================
KO_LABEL = {
    "wheelchair": "휠체어",
    "truck": "트럭",
    "tree_trunk": "나무",
    "traffic_sign": "교통표지판",
    "traffic_light": "신호등",
    "traffic_light_controller": "신호제어기",
    "table": "탁자",
    "stroller": "유모차",
    "stop": "정지표지판",
    "scooter": "스쿠터",
    "potted_plant": "화분",
    "power_controller": "전력제어함",
    "pole": "기둥",
    "person": "사람",
    "parking_meter": "주차정산기",
    "movable_signage": "이동식 안내표지",
    "motorcycle": "오토바이",
    "motorbike": "오토바이",
    "kiosk": "키오스크",
    "fire_hydrant": "소화전",
    "dog": "개",
    "chair": "의자",
    "cat": "고양이",
    "carrier": "운반수레",
    "car": "차량",
    "vehicle": "차량",
    "bus": "버스",
    "bollard": "볼라드",
    "bicycle": "자전거",
    "bike": "자전거",
    "bench": "벤치",
    "barricade": "바리케이드",
    "crosswalk": "횡단보도",
}

SEG_CLASS_MAP = {
    0: "walkable_surface",
    1: "braille_guide_blocks",
    2: "caution_surface",
    3: "roadway",
    4: "crosswalk",
}

WALKING_VIEW_HOURS = {9, 10, 11, 12, 1, 2, 3}

# bbox 하단 영역 중 점자블록 mask와 5% 이상 겹치면 점자블록 위 장애물로 판단
TACTILE_OVERLAP_THRESH = 0.05

# 보행 경로/도로 필터 threshold
PATH_OVERLAP_THRESH = 0.10
ROAD_OVERLAP_SUPPRESS_THRESH = 0.50
ROAD_VEHICLE_NEAR_EXCEPTION_M = 2.0       # 이 거리 이내 차량은 roadway여도 경고
ROAD_VEHICLE_APPROACH_EXCEPTION_M = 4.0  # 빠르게 접근 중인 차량은 이 거리까지 경고

# 안내문 길이 옵션
VALID_SENTENCE_LENGTHS = {"SHORT", "MEDIUM", "LONG"}

# 서버 프로세스가 살아있는 동안 tracking/cooldown 유지
track_history: dict[int, deque] = defaultdict(lambda: deque(maxlen=TRACK_HISTORY_LEN))
last_announced_frame: dict[int, int] = {}
last_scene_announced: dict[str, int] = {}


def reset_runtime_state() -> None:
    track_history.clear()
    last_announced_frame.clear()
    last_scene_announced.clear()


# =========================================================
# Korean text utils
# =========================================================
def to_ko(name: str) -> str:
    n = name.lower().strip()
    if n in KO_LABEL:
        return KO_LABEL[n]
    for key, value in KO_LABEL.items():
        if key in n:
            return value
    return name


def has_jongseong(word: str) -> bool:
    if not word:
        return False
    code = ord(word[-1])
    if 0xAC00 <= code <= 0xD7A3:
        return (code - 0xAC00) % 28 != 0
    return False


def jo_iga(word: str) -> str:
    return f"{word}이" if has_jongseong(word) else f"{word}가"


def jo_ro(word: str) -> str:
    if not word:
        return "로"
    code = ord(word[-1])
    if 0xAC00 <= code <= 0xD7A3:
        offset = (code - 0xAC00) % 28
        if offset == 0 or offset == 8:
            return f"{word}로"
        return f"{word}으로"
    return f"{word}로"


# =========================================================
# Direction / distance utils
# =========================================================
def x_ratio_to_clock(x_ratio: float) -> str:
    val = (x_ratio - 0.5) * 6.0
    idx = int(round(val))
    idx = max(-3, min(3, idx))
    if idx == 0:
        return "12시"
    if idx > 0:
        return f"{idx}시"
    return f"{12 + idx}시"


def clock_to_int(clock: str) -> int:
    return int(str(clock).replace("시", ""))


def int_to_clock(hour: int) -> str:
    hour = ((hour - 1) % 12) + 1
    return f"{hour}시"


def in_walking_view(clock_str: str) -> bool:
    return clock_to_int(clock_str) in WALKING_VIEW_HOURS


def detour_direction(danger_clock: str, safe_clocks: set[str]) -> str | None:
    h = clock_to_int(danger_clock)
    if 9 <= h <= 12:
        candidates = [int_to_clock(h + 2), int_to_clock(h - 2)]
    elif 1 <= h <= 3:
        candidates = [int_to_clock(h - 2), int_to_clock(h + 2)]
    else:
        candidates = [int_to_clock(h + 2), int_to_clock(h - 2)]

    for candidate in candidates:
        if candidate in safe_clocks:
            return candidate

    fallback = candidates[0] if candidates else None
    if fallback and clock_to_int(fallback) in WALKING_VIEW_HOURS:
        return fallback
    return None


def distance_bin(distance_m: float) -> str:
    if distance_m < NEAR_M:
        return "near"
    if distance_m < MID_M:
        return "mid"
    return "far"


def horiz_region(x_ratio: float) -> str:
    if x_ratio < 1 / 3:
        return "left"
    if x_ratio < 2 / 3:
        return "center"
    return "right"


# =========================================================
# Sentence length / guide formatting
# =========================================================
def normalize_sentence_length(sentence_length: str | None) -> str:
    if not sentence_length:
        return "MEDIUM"

    value = str(sentence_length).strip().upper()
    if value not in VALID_SENTENCE_LENGTHS:
        return "MEDIUM"

    return value


def opposite_detour_text(clock_direction: str | None) -> str:
    """
    위험 객체 방향을 기준으로 간단한 우회 방향을 정한다.
    오른쪽에 위험이 있으면 왼쪽, 왼쪽에 위험이 있으면 오른쪽.
    중앙이면 기본 오른쪽.
    """
    if not clock_direction:
        return "오른쪽"

    try:
        hour = clock_to_int(clock_direction)
    except Exception:
        return "오른쪽"

    if hour in {1, 2, 3}:
        return "왼쪽"
    if hour in {9, 10, 11}:
        return "오른쪽"

    return "오른쪽"


def format_tactile_object_guide(
    clock_direction: str,
    object_ko: str,
    sentence_length: str = "MEDIUM",
    detour_text: str | None = None,
) -> str:
    """
    점자블록 위 객체 안내문.
    SHORT:  12시 점자블록 위 스쿠터.
    MEDIUM: 주의. 12시 방향 점자블록 위 스쿠터, 오른쪽 우회바람.
    LONG:   주의. 12시 방향 점자블록 위에 스쿠터가 있습니다. 오른쪽으로 우회하세요.
    """
    sentence_length = normalize_sentence_length(sentence_length)
    detour_text = detour_text or opposite_detour_text(clock_direction)

    if sentence_length == "SHORT":
        return f"{clock_direction} 점자블록 위 {object_ko}."

    if sentence_length == "LONG":
        return (
            f"주의. {clock_direction} 방향 점자블록 위에 {jo_iga(object_ko)} 있습니다. "
            f"{detour_text}으로 우회하세요."
        )

    return f"주의. {clock_direction} 방향 점자블록 위 {object_ko}, {detour_text} 우회바람."

def format_tactile_unknown_guide(
    clock_direction: str,
    sentence_length: str = "MEDIUM",
) -> str:
    sentence_length = normalize_sentence_length(sentence_length)
    detour_text = opposite_detour_text(clock_direction)

    if sentence_length == "SHORT":
        return f"{clock_direction} 점자블록 가림."

    if sentence_length == "LONG":
        return (
            f"주의. {clock_direction} 방향 점자블록 위에 장애물이 있거나 "
            f"점자블록이 가려져 있습니다. {detour_text}으로 우회하세요."
        )

    return f"주의. {clock_direction} 점자블록 가림, {detour_text} 우회."


def format_tactile_broken_guide(
    sentence_length: str = "MEDIUM",
) -> str:
    sentence_length = normalize_sentence_length(sentence_length)

    if sentence_length == "SHORT":
        return "전방 점자블록 끊김."

    if sentence_length == "LONG":
        return "주의. 전방 점자블록이 끊겨 있습니다. 주변 보행로를 확인하며 천천히 이동하세요."

    return "주의. 전방 점자블록 끊김, 천천히 이동."


# =========================================================
# Class categorization
# =========================================================
def categorize_det_class(name: str) -> dict[str, bool]:
    n = name.lower()
    is_tl = ("traffic" in n and "light" in n) or n.startswith("tl_") or "signal" in n

    return {
        "is_person": any(k in n for k in ["person", "pedestrian", "people"]),
        "is_vehicle": any(
            k in n
            for k in [
                "car",
                "truck",
                "bus",
                "van",
                "vehicle",
                "motorcycle",
                "motorbike",
                "scooter",
                "bike",
                "bicycle",
            ]
        ),
        "is_traffic_light": is_tl,
        "is_tl_red": is_tl and "red" in n,
        "is_tl_green": is_tl and ("green" in n or "blue" in n),
        "is_crosswalk": "crosswalk" in n or "cross_walk" in n or "zebra" in n,
        "is_obstacle": any(
            k in n
            for k in [
                "pole",
                "barrier",
                "barricade",
                "bollard",
                "cone",
                "fence",
                "tree",
                "bench",
                "bin",
                "hydrant",
                "sign",
                "post",
                "table",
                "kiosk",
                "chair",
                "stroller",
                "wheelchair",
                "potted_plant",
                "power_controller",
                "parking_meter",
                "movable_signage",
                "carrier",
            ]
        ),
    }


def classify_object(det_name: str) -> dict[str, Any]:
    cat = categorize_det_class(det_name)
    if cat["is_vehicle"]:
        return {"risk_type": "dynamic_watch", "severity": 3, "is_dynamic": True}
    if cat["is_person"]:
        return {"risk_type": "informational", "severity": 1, "is_dynamic": True}
    if cat["is_obstacle"]:
        return {"risk_type": "path_blocking", "severity": 2, "is_dynamic": False}
    if cat["is_traffic_light"]:
        return {"risk_type": "signal", "severity": 1, "is_dynamic": False}
    return {"risk_type": "informational", "severity": 1, "is_dynamic": False}


# =========================================================
# Depth / mask helpers
# =========================================================
def bbox_distance_raw(depth_m: np.ndarray, bbox_xyxy: list[float], src_wh: tuple[int, int]) -> float:
    depth_h, depth_w = depth_m.shape
    src_w, src_h = src_wh
    sx = depth_w / src_w
    sy = depth_h / src_h

    x1, y1, x2, y2 = bbox_xyxy
    bbox_h = y2 - y1
    foot_h = max(8, bbox_h * 0.10)
    y_lo = y2 - foot_h

    xa = int(max(0, x1 * sx))
    xb = int(min(depth_w, x2 * sx))
    ya = int(max(0, y_lo * sy))
    yb = int(min(depth_h, y2 * sy))

    if xb <= xa or yb <= ya:
        xa = int(max(0, x1 * sx))
        xb = int(min(depth_w, x2 * sx))
        ya = int(max(0, y1 * sy))
        yb = int(min(depth_h, y2 * sy))

    if xb <= xa or yb <= ya:
        return float("nan")

    patch = depth_m[ya:yb, xa:xb]
    if patch.size == 0:
        return float("nan")

    # bbox 안에서 "가까운 쪽" depth만 사용 (도로면 픽셀 제거)
    # 하단 10% 띠 안에서도 가장 가까운 25% 픽셀만 골라서 중앙값을 취함
    # 이러면 차량 아래 도로면이 섞여도 차체 픽셀이 우선됨
    flat = patch.flatten()
    flat = flat[np.isfinite(flat) & (flat > 0)]
    if flat.size == 0:
        return float("nan")
    k = max(1, int(flat.size * 0.25))
    nearest_pixels = np.partition(flat, k - 1)[:k]
    raw = float(np.median(nearest_pixels))

    # scale factor
    # print(f"[DEPTH_RAW] {raw:.2f}m  (bbox h_ratio={bbox_h/src_h:.2f})")
    return raw * DEPTH_SCALE_FACTOR + DEPTH_OFFSET_M


def bbox_distance(depth_m: np.ndarray, bbox_xyxy: list[float], src_wh: tuple[int, int]) -> float:
    raw_d = bbox_distance_raw(depth_m, bbox_xyxy, src_wh)
    if raw_d != raw_d:
        return raw_d

    src_w, src_h = src_wh
    x1, y1, x2, y2 = bbox_xyxy
    bbox_h_ratio = (y2 - y1) / src_h

    if bbox_h_ratio < SMALL_BBOX_HEIGHT_RATIO and raw_d < SMALL_BBOX_MIN_TRUST_DIST:
        return max(raw_d, SMALL_BBOX_MIN_TRUST_DIST)
    return raw_d


def analyze_zones(depth_m: np.ndarray, min_pixels: int = 100) -> dict[str, dict[str, Any]]:
    h, w = depth_m.shape
    roi = depth_m[int(h * 0.30) :, :]
    rh, rw = roi.shape

    zones = {
        "left": roi[:, : rw // 3],
        "center": roi[:, rw // 3 : 2 * rw // 3],
        "right": roi[:, 2 * rw // 3 :],
    }

    results = {}
    for name, zone in zones.items():
        flat = zone.flatten()
        nearest = float(np.sort(flat)[:10].max()) if flat.size >= 10 else float(np.min(flat))
        urgent_pixels = int((zone < URGENT_DISTANCE_M).sum())
        alert_pixels = int((zone < ALERT_DISTANCE_M).sum())

        if urgent_pixels >= min_pixels:
            status, priority = "urgent", 2
        elif alert_pixels >= min_pixels:
            status, priority = "alert", 1
        else:
            status, priority = "safe", 0

        results[name] = {
            "nearest_m": round(nearest, 2),
            "status": status,
            "priority": priority,
        }

    return results


def analyze_segmentation(seg_result: Any, img_w: int, img_h: int) -> dict[str, Any]:
    info = {
        "walkable_exists": False,
        "tactile_exists": False,
        "crosswalk_exists": False,
        "caution_exists": False,
        "roadway_exists": False,
        "path_mask": np.zeros((img_h, img_w), dtype=bool),
        "road_mask": np.zeros((img_h, img_w), dtype=bool),
        "tactile_mask": np.zeros((img_h, img_w), dtype=bool),
    }

    if seg_result.masks is None or seg_result.boxes is None or len(seg_result.boxes) == 0:
        return info

    masks = seg_result.masks.data.detach().cpu().numpy()
    cls_ids = seg_result.boxes.cls.detach().cpu().numpy().astype(int)

    mask_h, mask_w = masks.shape[1], masks.shape[2]

    path_acc = np.zeros((mask_h, mask_w), dtype=bool)
    road_acc = np.zeros((mask_h, mask_w), dtype=bool)
    tactile_acc = np.zeros((mask_h, mask_w), dtype=bool)

    for mask, class_id in zip(masks, cls_ids):
        class_name = SEG_CLASS_MAP.get(int(class_id), str(class_id))
        mask_bool = mask > 0.5

        if class_name == "walkable_surface":
            info["walkable_exists"] = True
            path_acc |= mask_bool
        elif class_name == "braille_guide_blocks":
            info["tactile_exists"] = True
            path_acc |= mask_bool
            tactile_acc |= mask_bool
        elif class_name == "crosswalk":
            info["crosswalk_exists"] = True
            path_acc |= mask_bool
        elif class_name == "caution_surface":
            info["caution_exists"] = True
        elif class_name == "roadway":
            info["roadway_exists"] = True
            road_acc |= mask_bool

    if (mask_h, mask_w) != (img_h, img_w):
        path_u8 = path_acc.astype(np.uint8) * 255
        road_u8 = road_acc.astype(np.uint8) * 255
        tactile_u8 = tactile_acc.astype(np.uint8) * 255

        info["path_mask"] = cv2.resize(path_u8, (img_w, img_h), interpolation=cv2.INTER_NEAREST) > 127
        info["road_mask"] = cv2.resize(road_u8, (img_w, img_h), interpolation=cv2.INTER_NEAREST) > 127
        info["tactile_mask"] = cv2.resize(tactile_u8, (img_w, img_h), interpolation=cv2.INTER_NEAREST) > 127
    else:
        info["path_mask"] = path_acc
        info["road_mask"] = road_acc
        info["tactile_mask"] = tactile_acc

    return info


def path_overlap_ratio(bbox_xyxy: list[float], path_mask: np.ndarray) -> float:
    if not path_mask.any():
        return 0.0

    h, w = path_mask.shape
    x1, y1, x2, y2 = bbox_xyxy
    x1, y1 = int(max(0, x1)), int(max(0, y1))
    x2, y2 = int(min(w, x2)), int(min(h, y2))

    if x2 <= x1 or y2 <= y1:
        return 0.0

    y_lo = y1 + (y2 - y1) * 2 // 3
    region = path_mask[y_lo:y2, x1:x2]
    return float(region.mean()) if region.size else 0.0


def road_overlap_ratio(
    bbox_xyxy: list[float],
    road_mask: np.ndarray,
    use_lower_part: bool = True,
    lower_ratio: float = 1 / 3,
) -> float:
    if not road_mask.any():
        return 0.0

    h, w = road_mask.shape
    x1, y1, x2, y2 = bbox_xyxy
    x1, y1 = int(max(0, x1)), int(max(0, y1))
    x2, y2 = int(min(w, x2)), int(min(h, y2))

    if x2 <= x1 or y2 <= y1:
        return 0.0

    # 객체 발밑(하단부)이 도로에 있는지를 기준으로 판단
    if use_lower_part:
        box_h = y2 - y1
        y1 = int(y2 - box_h * lower_ratio)
        y1 = max(0, y1)

    region = road_mask[y1:y2, x1:x2]
    return float(region.mean()) if region.size else 0.0


def tactile_overlap_ratio(
    bbox_xyxy: list[float],
    tactile_mask: np.ndarray,
    use_lower_part: bool = True,
    lower_ratio: float = 1 / 3,
) -> float:
    """
    객체 bbox와 점자블록 mask의 overlap 비율 계산.
    bbox 전체가 아니라 하단 1/3 영역을 기준으로 계산한다.
    """
    if not tactile_mask.any():
        return 0.0

    h, w = tactile_mask.shape
    x1, y1, x2, y2 = bbox_xyxy

    x1 = int(max(0, min(w - 1, x1)))
    y1 = int(max(0, min(h - 1, y1)))
    x2 = int(max(0, min(w, x2)))
    y2 = int(max(0, min(h, y2)))

    if x2 <= x1 or y2 <= y1:
        return 0.0

    if use_lower_part:
        box_h = y2 - y1
        y1 = int(y2 - box_h * lower_ratio)
        y1 = max(0, y1)

    roi = tactile_mask[y1:y2, x1:x2]
    return float(roi.mean()) if roi.size else 0.0


# =========================================================
# Tactile interruption helpers
# =========================================================
def build_expected_tactile_corridor(
    tactile_mask: np.ndarray,
    min_band_width: int = 24,
    band_scale: float = 2.2,
) -> tuple[np.ndarray, dict[str, Any]]:
    """
    현재 보이는 점자블록 mask를 기반으로 예상 점자블록 corridor를 생성한다.
    detection이 잡지 못한 장애물/가림으로 인해 점자블록 mask가 비는 구간을 찾기 위한 용도.
    """
    h, w = tactile_mask.shape
    ys, xs = np.where(tactile_mask)

    if len(xs) < 30:
        return np.zeros_like(tactile_mask, dtype=bool), {
            "ok": False,
            "reason": "tactile pixels too few",
        }

    row_widths = []
    for y in np.unique(ys):
        row_xs = xs[ys == y]
        if len(row_xs) >= 2:
            row_widths.append(row_xs.max() - row_xs.min() + 1)

    median_width = float(np.median(row_widths)) if row_widths else float(min_band_width)
    band_width = int(max(min_band_width, median_width * band_scale))
    half_band = band_width // 2

    pts = np.column_stack([xs, ys]).astype(np.float32)
    vx, vy, x0, y0 = cv2.fitLine(pts, cv2.DIST_L2, 0, 0.01, 0.01).flatten()

    expected = np.zeros_like(tactile_mask, dtype=bool)

    y_min = int(max(0, ys.min()))
    y_max = h - 1

    if abs(vy) < 1e-6:
        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (band_width, band_width))
        expected = cv2.dilate(tactile_mask.astype(np.uint8), kernel, iterations=1) > 0
    else:
        for y in range(y_min, y_max + 1):
            x_center = float(x0 + (y - y0) * vx / vy)
            x1 = int(max(0, round(x_center - half_band)))
            x2 = int(min(w, round(x_center + half_band)))
            if x2 > x1:
                expected[y, x1:x2] = True

    return expected, {
        "ok": True,
        "band_width": band_width,
        "y_min": y_min,
        "y_max": y_max,
        "line": {
            "vx": float(vx),
            "vy": float(vy),
            "x0": float(x0),
            "y0": float(y0),
        },
    }


def find_row_segments(row_visible: np.ndarray) -> list[tuple[int, int]]:
    segments = []
    start = None

    for i, value in enumerate(row_visible):
        if value and start is None:
            start = i
        elif not value and start is not None:
            segments.append((start, i - 1))
            start = None

    if start is not None:
        segments.append((start, len(row_visible) - 1))

    return segments


def analyze_tactile_interruption(
    tactile_mask: np.ndarray,
    min_gap_ratio: float = 0.06,
    visible_ratio_thresh: float = 0.04,
) -> dict[str, Any]:
    """
    점자블록 mask만으로 가림/끊김을 분석한다.

    status:
    - clear
    - blocked_or_occluded
    - broken
    - no_tactile
    """
    h, w = tactile_mask.shape

    if tactile_mask.sum() < 30:
        return {
            "status": "no_tactile",
            "warning_needed": False,
            "reason": "점자블록 mask가 거의 없음",
            "clock_direction": None,
            "gap_bbox": None,
            "expected_corridor": np.zeros_like(tactile_mask, dtype=bool),
            "gap_mask": np.zeros_like(tactile_mask, dtype=bool),
        }

    expected_corridor, meta = build_expected_tactile_corridor(tactile_mask)

    if not meta.get("ok", False) or not expected_corridor.any():
        return {
            "status": "clear",
            "warning_needed": False,
            "reason": "expected corridor 생성 실패",
            "clock_direction": None,
            "gap_bbox": None,
            "expected_corridor": expected_corridor,
            "gap_mask": np.zeros_like(tactile_mask, dtype=bool),
        }

    expected_count = expected_corridor.sum(axis=1)
    tactile_in_corridor = (expected_corridor & tactile_mask).sum(axis=1)

    row_ratio = np.zeros(h, dtype=np.float32)
    valid_rows = expected_count > 0
    row_ratio[valid_rows] = tactile_in_corridor[valid_rows] / expected_count[valid_rows]

    row_visible = (row_ratio >= visible_ratio_thresh) & valid_rows

    # 세로 방향 노이즈 제거
    row_visible_u8 = row_visible.astype(np.uint8) * 255
    kernel = np.ones((9, 1), dtype=np.uint8)
    row_visible_smooth = cv2.morphologyEx(
        row_visible_u8.reshape(-1, 1),
        cv2.MORPH_CLOSE,
        kernel,
    ).flatten() > 0

    visible_segments = find_row_segments(row_visible_smooth)
    gap_mask = np.zeros_like(tactile_mask, dtype=bool)

    if len(visible_segments) == 0:
        return {
            "status": "no_tactile",
            "warning_needed": False,
            "reason": "예상 corridor 안에서 점자블록이 보이지 않음",
            "clock_direction": None,
            "gap_bbox": None,
            "expected_corridor": expected_corridor,
            "gap_mask": gap_mask,
        }

    min_gap_len = max(12, int(h * min_gap_ratio))

    gaps = []
    for (s1, e1), (s2, e2) in zip(visible_segments[:-1], visible_segments[1:]):
        gap_start = e1 + 1
        gap_end = s2 - 1
        gap_len = gap_end - gap_start + 1

        if gap_len >= min_gap_len:
            gaps.append((gap_start, gap_end, gap_len))

    if gaps:
        gap_start, gap_end, _ = sorted(gaps, key=lambda item: -item[2])[0]
        gap_mask[gap_start : gap_end + 1, :] = expected_corridor[gap_start : gap_end + 1, :]

        gap_ys, gap_xs = np.where(gap_mask)
        if len(gap_xs) > 0:
            x1, x2 = int(gap_xs.min()), int(gap_xs.max())
            y1, y2 = int(gap_ys.min()), int(gap_ys.max())
            cx = (x1 + x2) / 2.0
            clock_direction = x_ratio_to_clock(cx / float(w))
            gap_bbox = [x1, y1, x2, y2]
        else:
            clock_direction = "12시"
            gap_bbox = None

        return {
            "status": "blocked_or_occluded",
            "warning_needed": True,
            "reason": "점자블록 corridor 중간에 큰 가림 구간 감지",
            "clock_direction": clock_direction,
            "gap_bbox": gap_bbox,
            "expected_corridor": expected_corridor,
            "gap_mask": gap_mask,
        }

    bottom_start = int(h * 0.65)
    bottom_visible = row_visible_smooth[bottom_start:].mean() > 0.05

    mid_start = int(h * 0.25)
    mid_end = int(h * 0.65)
    mid_part = row_visible_smooth[mid_start:mid_end]
    mid_visible = mid_part.mean() > 0.05 if len(mid_part) > 0 else False

    if bottom_visible and not mid_visible:
        return {
            "status": "broken",
            "warning_needed": True,
            "reason": "하단에는 점자블록이 있으나 전방으로 이어지지 않음",
            "clock_direction": "12시",
            "gap_bbox": None,
            "expected_corridor": expected_corridor,
            "gap_mask": gap_mask,
        }

    return {
        "status": "clear",
        "warning_needed": False,
        "reason": "점자블록이 비교적 이어져 있음",
        "clock_direction": None,
        "gap_bbox": None,
        "expected_corridor": expected_corridor,
        "gap_mask": gap_mask,
    }


# =========================================================
# Tracking / cooldown
# =========================================================
def update_track(track_id: int, frame_idx: int, cx: float, cy: float, dist_m: float) -> None:
    track_history[track_id].append((frame_idx, cx, cy, dist_m))


def estimate_motion(track_id: int) -> str:
    hist = track_history[track_id]
    if len(hist) < 2:
        return "static"

    (_, x0, y0, _), (_, x1, y1, _) = hist[-2], hist[-1]
    disp = ((x1 - x0) ** 2 + (y1 - y0) ** 2) ** 0.5
    return "moved" if disp >= MOTION_PIXEL_THRESHOLD else "static"


def estimate_approach_speed(track_id: int, motion: str) -> float:
    if motion != "moved":
        return 0.0

    hist = track_history[track_id]
    if len(hist) < VEHICLE_MIN_TRACK_AGE:
        return 0.0

    dists = [distance for (_, _, _, distance) in hist if distance == distance]
    if len(dists) < VEHICLE_MIN_TRACK_AGE:
        return 0.0

    diffs = [dists[i] - dists[i + 1] for i in range(len(dists) - 1)]
    return float(np.mean(diffs)) if diffs else 0.0


def is_in_cooldown(track_id: int, current_frame: int, cooldown_frames: int) -> bool:
    return track_id in last_announced_frame


def mark_announced(track_id: int, frame_idx: int) -> None:
    last_announced_frame[track_id] = frame_idx


def is_scene_in_cooldown(scene_key: str, current_frame: int) -> bool:
    last = last_scene_announced.get(scene_key)
    return last is not None and (current_frame - last) < SCENE_INFO_COOLDOWN_FRAMES


def mark_scene_announced(scene_key: str, frame_idx: int) -> None:
    last_scene_announced[scene_key] = frame_idx


def is_scene_announced_once(scene_key: str) -> bool:
    return scene_key in last_scene_announced


def mark_scene_announced_once(scene_key: str, frame_idx: int) -> None:
    last_scene_announced[scene_key] = frame_idx


# =========================================================
# Scene build / guide policy
# =========================================================
def build_scene_from_model_outputs(
    image_id: str,
    frame_idx: int,
    img_w: int,
    img_h: int,
    det_result: Any,
    seg_result: Any,
    depth_m: np.ndarray,
    det_class_names: dict[int, str],
    sentence_length: str = "MEDIUM",
) -> dict[str, Any]:
    sentence_length = normalize_sentence_length(sentence_length)

    seg_info = analyze_segmentation(seg_result, img_w, img_h)
    zones = analyze_zones(depth_m)

    tactile_interruption = analyze_tactile_interruption(seg_info["tactile_mask"])
    tactile_status = tactile_interruption["status"]

    if det_result.boxes is not None and len(det_result.boxes) > 0:
        boxes_obj = det_result.boxes
        xyxy = boxes_obj.xyxy.detach().cpu().numpy()
        cls_ids = boxes_obj.cls.detach().cpu().numpy().astype(int)
        confs = boxes_obj.conf.detach().cpu().numpy()

        if boxes_obj.id is not None:
            track_ids = boxes_obj.id.detach().cpu().numpy().astype(int)
        else:
            track_ids = np.arange(len(xyxy)) + 10000
    else:
        xyxy = np.zeros((0, 4))
        cls_ids = np.zeros(0, dtype=int)
        confs = np.zeros(0)
        track_ids = np.zeros(0, dtype=int)

    objects = []
    traffic_light_summary = {"exists": False, "color": "none"}
    tl_candidates = []

    for xy, class_id, confidence, track_id in zip(xyxy, cls_ids, confs, track_ids):
        class_name = det_class_names[int(class_id)]
        cat = categorize_det_class(class_name)
        class_info = classify_object(class_name)

        x1, y1, x2, y2 = xy.tolist()
        cx = (x1 + x2) / 2.0
        cy = (y1 + y2) / 2.0
        x_ratio = cx / img_w

        dist_m = bbox_distance(depth_m, xy.tolist(), (img_w, img_h))
        update_track(int(track_id), frame_idx, cx, cy, dist_m)

        motion = estimate_motion(int(track_id))
        approach = estimate_approach_speed(int(track_id), motion)

        path_ovl = path_overlap_ratio(xy.tolist(), seg_info["path_mask"])
        road_ovl = road_overlap_ratio(xy.tolist(), seg_info["road_mask"])

        tactile_ovl = tactile_overlap_ratio(xy.tolist(), seg_info["tactile_mask"])
        blocks_tactile = tactile_ovl >= TACTILE_OVERLAP_THRESH

        on_path = path_ovl >= 0.10 and road_ovl < 0.50

        tl_color = "none"
        if cat["is_traffic_light"]:
            if cat["is_tl_red"]:
                tl_color = "red"
            elif cat["is_tl_green"]:
                tl_color = "green"
            else:
                tl_color = "unknown"

            traffic_light_summary["exists"] = True
            tl_candidates.append((dist_m if dist_m == dist_m else 1e9, tl_color))

        objects.append(
            {
                "object_id": f"obj_{int(track_id)}",
                "class": class_name,
                "class_ko": to_ko(class_name),
                "confidence": round(float(confidence), 3),
                "bbox_xyxy": [round(float(value), 1) for value in xy.tolist()],
                "bbox": [round(float(value), 1) for value in xy.tolist()],
                "motion": motion,
                "h_region": horiz_region(x_ratio),
                "clock_direction": x_ratio_to_clock(x_ratio),
                "distance_m": round(float(dist_m), 2) if dist_m == dist_m else None,
                "distance": distance_bin(dist_m) if dist_m == dist_m else "unknown",
                "on_path": bool(on_path),
                "narrows_path": bool(on_path and (dist_m == dist_m) and dist_m < ALERT_DISTANCE_M),
                "blocks_tactile": bool(blocks_tactile),
                "tactile_overlap": round(float(tactile_ovl), 4),
                "risk_type": class_info["risk_type"],
                "severity": class_info["severity"],
                "is_dynamic": class_info["is_dynamic"],
                "track_age": len(track_history[int(track_id)]),
                "_cat": cat,
                "_tl_color": tl_color,
                "_approach_mps": approach,
                "_path_overlap": path_ovl,
                "_road_overlap": road_ovl,
            }
        )

    if tl_candidates:
        tl_candidates.sort(key=lambda item: item[0])
        chosen = next((color for _, color in tl_candidates if color in {"red", "green"}), None)
        traffic_light_summary["color"] = chosen if chosen else tl_candidates[0][1]

    tactile_blocked_by_object = any(obj.get("blocks_tactile", False) for obj in objects)

    scene = {
        "tactile_block": {
            "exists": bool(seg_info["tactile_exists"]),
            "blocked": bool(tactile_blocked_by_object or tactile_status == "blocked_or_occluded"),
            "broken": bool(tactile_status == "broken"),
            "status": tactile_status,
            "clock_direction": tactile_interruption.get("clock_direction"),
            "gap_bbox": tactile_interruption.get("gap_bbox"),
            "reason": tactile_interruption.get("reason"),
        },
        "crosswalk": {"exists": bool(seg_info["crosswalk_exists"])},
        "walkable": {"exists": bool(seg_info["walkable_exists"])},
        "caution_zone": {"exists": bool(seg_info["caution_exists"])},
        "roadway": {"exists": bool(seg_info["roadway_exists"])},
        "traffic_light": traffic_light_summary,
        "depth_zones": zones,
        "objects": objects,
        "_zones": zones,
    }

    gt = build_voice_guide(
        scene=scene,
        frame_idx=frame_idx,
        img_w=img_w,
        img_h=img_h,
        sentence_length=sentence_length,
    )

    # 내부 계산용 키 제거
    for obj in scene["objects"]:
        for key in ["_cat", "_tl_color", "_approach_mps", "_path_overlap", "_road_overlap"]:
            obj.pop(key, None)

    scene.pop("_zones", None)

    return {
        "id": image_id,
        "frame_idx": frame_idx,
        "scene": scene,
        "gt": gt,
    }


def is_guide_candidate_object(obj: dict[str, Any], scene: dict[str, Any], img_area: float) -> bool:
    """
    실제 음성 안내 후보로 포함할 객체인지 판단한다.

    - 보행 경로/점자블록 위 장애물은 후보 유지
    - roadway 위 차량은 기본 제외
    - 단, 매우 가까움 / 빠른 접근 / 화면 크게 차지 / 횡단보도 상황은 예외
    """
    cat = obj["_cat"]
    distance_m = obj.get("distance_m")

    if distance_m is None or distance_m != distance_m:
        return False
    if obj.get("confidence", 0.0) < MIN_GUIDE_CONF:
        return False
    if cat.get("is_person", False):
        return False

    # 점자블록 위 객체는 무조건 후보
    if obj.get("blocks_tactile", False):
        return True

    # 신호등은 횡단보도가 있을 때만 후보
    if cat.get("is_traffic_light", False):
        return bool(scene.get("crosswalk", {}).get("exists", False))

    # 보행 가능 영역 위 객체는 후보
    if obj.get("on_path", False):
        return True

    road_ovl = obj.get("_road_overlap", 0.0)
    path_ovl = obj.get("_path_overlap", 0.0)

    x1, y1, x2, y2 = obj["bbox_xyxy"]
    area_ratio = max(0.0, (x2 - x1) * (y2 - y1)) / img_area if img_area > 0 else 0.0
    is_huge = area_ratio >= HUGE_BBOX_AREA_RATIO and distance_m < HUGE_BBOX_MAX_DIST

    # 도로 위 차량은 기본 제외, 단 예외 조건 충족 시 유지
    if cat.get("is_vehicle", False) and road_ovl >= ROAD_OVERLAP_SUPPRESS_THRESH and path_ovl < PATH_OVERLAP_THRESH:
        # 예외 1: 너무 가까움 — 보행 영역을 이미 침범했을 가능성
        if distance_m <= ROAD_VEHICLE_NEAR_EXCEPTION_M:
            return True
        # 예외 2: 빠르게 접근 중
        if (
            obj.get("_approach_mps", 0.0) > VEHICLE_APPROACH_THRESH
            and obj.get("track_age", 0) >= VEHICLE_MIN_TRACK_AGE
            and obj.get("motion") == "moved"
            and distance_m <= ROAD_VEHICLE_APPROACH_EXCEPTION_M
        ):
            return True
        # 예외 3: 화면을 크게 차지함
        if is_huge:
            return True
        return False

    return False


def compute_risk_score(obj: dict[str, Any], scene_img_area: float) -> float:
    cat = obj["_cat"]
    distance_m = obj["distance_m"]

    if distance_m is None or distance_m != distance_m:
        return -1.0
    if obj["confidence"] < MIN_GUIDE_CONF:
        return -1.0
    if cat["is_person"]:
        return -1.0

    if cat["is_vehicle"]:
        base = 5.0
    elif cat["is_obstacle"]:
        base = 3.0
    elif cat["is_person"]:
        base = 2.0
    elif cat["is_traffic_light"]:
        base = 1.0
    else:
        base = 1.0

    if distance_m < URGENT_DISTANCE_M:
        dist_bonus = 10.0
    elif distance_m < ALERT_DISTANCE_M:
        dist_bonus = 6.0
    elif distance_m < MAX_GUIDE_DISTANCE_M:
        dist_bonus = 2.0
    else:
        dist_bonus = 0.0

    x1, y1, x2, y2 = obj["bbox_xyxy"]
    bbox_area = max(0.0, (x2 - x1) * (y2 - y1))
    area_ratio = bbox_area / scene_img_area if scene_img_area > 0 else 0.0

    size_bonus = (
        4.0
        if area_ratio >= HUGE_BBOX_AREA_RATIO and distance_m < HUGE_BBOX_SIZE_BONUS_MAX_DIST
        else 0.0
    )

    view_bonus = 2.0 if in_walking_view(obj["clock_direction"]) else -3.0

    if obj["on_path"]:
        path_bonus = 1.0
    elif distance_m < URGENT_DISTANCE_M:
        path_bonus = 0.0
    else:
        path_bonus = -1.0

    road_penalty = 0.0
    if (
        cat["is_vehicle"]
        and obj["_road_overlap"] > 0.30
        and distance_m > ROAD_OVERLAP_VEHICLE_PENALTY_MIN_DIST
    ):
        road_penalty = -3.0

    approach_bonus = 0.0
    if (
        cat["is_vehicle"]
        and obj["_approach_mps"] > VEHICLE_APPROACH_THRESH
        and obj["track_age"] >= VEHICLE_MIN_TRACK_AGE
        and obj["motion"] == "moved"
        and distance_m < VEHICLE_STOP_DISTANCE
    ):
        approach_bonus = 3.0

    # 점자블록 위 장애물은 일반 위험보다 우선적으로 고려
    tactile_bonus = 4.0 if obj.get("blocks_tactile", False) else 0.0

    return base + dist_bonus + size_bonus + view_bonus + path_bonus + road_penalty + approach_bonus + tactile_bonus


def build_voice_guide(
    scene: dict[str, Any],
    frame_idx: int,
    img_w: int,
    img_h: int,
    sentence_length: str = "MEDIUM",
) -> dict[str, Any]:
    sentence_length = normalize_sentence_length(sentence_length)

    # 점자블록 위 장애물/가림/끊김은 일반 객체 안내보다 우선
    tactile_gt = _decide_tactile_guide_once(
        scene=scene,
        frame_idx=frame_idx,
        sentence_length=sentence_length,
    )
    if tactile_gt is not None:
        tactile_gt["scene_info_added"] = []
        tactile_gt["sentence_length"] = sentence_length
        tactile_gt["alert_level"] = alert_level_from_gt(tactile_gt)
        return tactile_gt

    gt = _decide_primary_guide(scene, frame_idx, img_w, img_h, sentence_length)
    gt = _append_scene_info(gt, scene, frame_idx, sentence_length)
    gt["sentence_length"] = sentence_length
    gt["alert_level"] = alert_level_from_gt(gt)
    return gt


def _decide_tactile_guide_once(
    scene: dict[str, Any],
    frame_idx: int,
    sentence_length: str = "MEDIUM",
) -> dict[str, Any] | None:
    """
    점자블록 관련 위험은 최초 1회만 안내한다.

    1. 객체 bbox 하단부와 점자블록 mask가 겹침
    2. detection 객체는 없지만 점자블록 mask가 중간에서 끊기거나 가려짐
    3. 점자블록이 하단에는 보이지만 전방으로 이어지지 않음
    """
    sentence_length = normalize_sentence_length(sentence_length)
    objects = scene["objects"]

    blocking_objects = [
        obj
        for obj in objects
        if obj.get("blocks_tactile", False)
        and obj.get("confidence", 0.0) >= MIN_GUIDE_CONF
    ]

    tactile_block = scene.get("tactile_block", {})
    tactile_status = tactile_block.get("status")

    # 상황이 사라지면 플래그 초기화 → 새 구간에서 다시 안내 가능
    if not blocking_objects:
        last_scene_announced.pop("tactile_blocked_by_object", None)
    if tactile_status != "blocked_or_occluded":
        last_scene_announced.pop("tactile_blocked_or_occluded", None)
    if tactile_status != "broken":
        last_scene_announced.pop("tactile_broken", None)

    # 1. detection 객체가 점자블록 위에 있는 경우

    if blocking_objects:
        primary = sorted(
            blocking_objects,
            key=lambda obj: (
                -obj.get("tactile_overlap", 0.0),
                -obj.get("confidence", 0.0),
            ),
        )[0]

        ko = to_ko(primary["class"])
        direction = primary["clock_direction"]

        if not is_scene_announced_once("tactile_blocked_by_object"):
            voice = format_tactile_object_guide(
                clock_direction=direction,
                object_ko=ko,
                sentence_length=sentence_length,
            )
            mark_scene_announced_once("tactile_blocked_by_object", frame_idx)
        else:
            voice = ""

        return _make_gt(
            primary,
            "caution",
            2,
            f"{direction} 방향 점자블록 위 {ko} 감지",
            voice,
        )

    # 2. detection은 없지만 점자블록이 가려졌거나 알 수 없는 장애물이 있는 경우
    if tactile_status == "blocked_or_occluded":
        direction = tactile_block.get("clock_direction") or "12시"

        if not is_scene_announced_once("tactile_blocked_or_occluded"):
            voice = format_tactile_unknown_guide(
                clock_direction=direction,
                sentence_length=sentence_length,
            )
            mark_scene_announced_once("tactile_blocked_or_occluded", frame_idx)
        else:
            voice = ""

        return {
            "warning_needed": True,
            "primary_object_id": None,
            "primary_object_class": "장애물",
            "primary_object_class_ko": "장애물",
            "clock_direction": direction,
            "distance": None,
            "distance_m": None,
            "action": "caution",
            "priority": 2,
            "reason": "점자블록 위 장애물 또는 가림 감지",
            "voice_guide": voice,
        }

    # 3. 점자블록 끊김
    if tactile_status == "broken":
        if not is_scene_announced_once("tactile_broken"):
            voice = format_tactile_broken_guide(sentence_length)
            mark_scene_announced_once("tactile_broken", frame_idx)
        else:
            voice = ""

        return {
            "warning_needed": True,
            "primary_object_id": None,
            "primary_object_class": "tactile_block",
            "primary_object_class_ko": "점자블록",
            "clock_direction": "12시",
            "distance": None,
            "distance_m": None,
            "action": "caution",
            "priority": 2,
            "reason": "전방 점자블록 끊김",
            "voice_guide": voice,
        }

    return None


def _decide_primary_guide(scene: dict[str, Any], frame_idx: int, img_w: int, img_h: int, sentence_length: str = "MEDIUM") -> dict[str, Any]:
    sentence_length = normalize_sentence_length(sentence_length)
    objects = scene["objects"]
    zones = scene["_zones"]
    img_area = float(img_w * img_h)

    red_light = next(
        (
            obj
            for obj in objects
            if obj["_cat"]["is_traffic_light"]
            and obj["_tl_color"] == "red"
            and obj["confidence"] >= MIN_GUIDE_CONF
        ),
        None,
    )

    if red_light and scene["crosswalk"]["exists"]:
        red_voice = "정지. 빨간불." if sentence_length != "LONG" else "정지. 빨간불입니다."
        return _make_gt(
            red_light,
            "stop",
            1,
            "빨간 신호",
            red_voice,
            primary_class_override="traffic_light_red",
            primary_class_ko_override="빨간 신호등",
        )

    candidates = []
    for obj in objects:
        distance_m = obj["distance_m"]

        if distance_m is None or distance_m != distance_m:
            continue
        if obj["confidence"] < MIN_GUIDE_CONF:
            continue

        x1, y1, x2, y2 = obj["bbox_xyxy"]
        area_ratio = max(0.0, (x2 - x1) * (y2 - y1)) / img_area
        is_huge = area_ratio >= HUGE_BBOX_AREA_RATIO and distance_m < HUGE_BBOX_MAX_DIST

        if distance_m <= MAX_GUIDE_DISTANCE_M:
            if not in_walking_view(obj["clock_direction"]) and not is_huge:
                continue
            if not is_guide_candidate_object(obj, scene, img_area):
                continue
            candidates.append(obj)

    if not candidates:
        green = next(
            (
                obj
                for obj in objects
                if obj["_cat"]["is_traffic_light"]
                and obj["_tl_color"] == "green"
                and obj["confidence"] >= MIN_GUIDE_CONF
            ),
            None,
        )

        if green and scene["crosswalk"]["exists"]:
            green_voice = "파란불. 직진." if sentence_length != "LONG" else "파란불입니다. 직진하세요."
            return _make_gt(
                green,
                "go",
                3,
                "파란 신호",
                green_voice,
                primary_class_override="traffic_light_green",
                primary_class_ko_override="파란 신호등",
            )

        return _empty_gt()

    scored = [(compute_risk_score(obj, img_area), obj) for obj in candidates]
    scored = [(score, obj) for score, obj in scored if score > 0]
    scored.sort(key=lambda item: -item[0])

    if not scored:
        return _empty_gt()

    def is_urgent_candidate(score: float, obj: dict[str, Any]) -> bool:
        cat = obj["_cat"]
        distance_m = obj["distance_m"]
        if cat["is_person"]:
            return False
        is_dynamic = cat["is_vehicle"]

        x1, y1, x2, y2 = obj["bbox_xyxy"]
        area_ratio = max(0.0, (x2 - x1) * (y2 - y1)) / img_area
        is_huge = area_ratio >= HUGE_BBOX_AREA_RATIO and distance_m < HUGE_BBOX_MAX_DIST

        if is_dynamic and distance_m < URGENT_DISTANCE_M:
            return True
        if cat["is_vehicle"] and is_huge:
            return True
        if (
            cat["is_vehicle"]
            and obj["_approach_mps"] > VEHICLE_APPROACH_THRESH
            and obj["track_age"] >= VEHICLE_MIN_TRACK_AGE
            and obj["motion"] == "moved"
            and distance_m < VEHICLE_STOP_DISTANCE
        ):
            return True

        return False

    urgent_pool = [
        (score, obj)
        for score, obj in scored
        if is_urgent_candidate(score, obj) and score >= 4.0
    ]

    if urgent_pool:
        vehicle_urgent = [(score, obj) for score, obj in urgent_pool if obj["_cat"]["is_vehicle"]]
        non_vehicle_urgent = [(score, obj) for score, obj in urgent_pool if not obj["_cat"]["is_vehicle"]]

        primary = None

        if vehicle_urgent:
            for _, obj in vehicle_urgent:
                tid = int(obj["object_id"].replace("obj_", ""))
                cooldown = STATIC_OBJ_COOLDOWN_FRAMES if not obj["is_dynamic"] else DYNAMIC_OBJ_COOLDOWN_FRAMES
                if not is_in_cooldown(tid, frame_idx, cooldown):
                    primary = obj
                    break
        else:
            for _, obj in non_vehicle_urgent:
                tid = int(obj["object_id"].replace("obj_", ""))
                cooldown = STATIC_OBJ_COOLDOWN_FRAMES if not obj["is_dynamic"] else DYNAMIC_OBJ_COOLDOWN_FRAMES

                if not is_in_cooldown(tid, frame_idx, cooldown):
                    primary = obj
                    break

        if primary is not None:
            tid = int(primary["object_id"].replace("obj_", ""))
            ko = to_ko(primary["class"])
            cat = primary["_cat"]
            clock = primary["clock_direction"]

            if cat["is_vehicle"] and primary["_approach_mps"] > VEHICLE_APPROACH_THRESH and primary["motion"] == "moved":
                reason = f"{jo_iga(ko)} 빠르게 접근 중"
                if sentence_length == "LONG":
                    guide = f"정지! {clock} 방향에서 {jo_iga(ko)} 빠르게 다가오고 있습니다."
                else:
                    guide = f"정지! {clock} {ko}."
            else:
                reason = f"{clock} 방향 매우 가까운 {ko}"
                if sentence_length == "LONG":
                    guide = f"정지! {clock} 방향에 {jo_iga(ko)} 있습니다."
                else:
                    guide = f"정지! {clock} {ko}."

            mark_announced(tid, frame_idx)
            return _make_gt(primary, "stop", 1, reason, guide)

    general_pool = [(score, obj) for score, obj in scored if score >= 5.0]

    if not general_pool:
        return _empty_gt()

    primary = None
    primary_tid = None

    for _, obj in general_pool:
        tid = int(obj["object_id"].replace("obj_", ""))
        cooldown = STATIC_OBJ_COOLDOWN_FRAMES if not obj["is_dynamic"] else DYNAMIC_OBJ_COOLDOWN_FRAMES

        if not is_in_cooldown(tid, frame_idx, cooldown):
            primary = obj
            primary_tid = tid
            break

    if primary is None or primary_tid is None:
        return _empty_gt(reason="cooldown 내 동일 객체")

    cat = primary["_cat"]
    distance_m = primary["distance_m"]
    is_dynamic = cat["is_vehicle"] or cat["is_person"]
    is_moving_vehicle = cat["is_vehicle"] and primary["motion"] == "moved"
    ko = to_ko(primary["class"])
    clock = primary["clock_direction"]

    safe_zones = {zone for zone, info in zones.items() if info["status"] == "safe"}
    zone_to_clock = {"left": "10시", "center": "12시", "right": "2시"}
    safe_clocks = {zone_to_clock[zone] for zone in safe_zones}

    if is_moving_vehicle:
        if distance_m < ALERT_DISTANCE_M:
            action = "stop"
            priority = 1
            reason = f"{clock} 방향 움직이는 {ko}"
            if sentence_length == "LONG":
                guide = f"정지! {clock} 방향에서 {jo_iga(ko)} 움직이고 있습니다."
            else:
                guide = f"정지! {clock} {ko}."
        else:
            action = "caution"
            priority = 2
            reason = f"{clock} 방향 움직이는 {ko}"
            if sentence_length == "LONG":
                guide = f"주의. {clock} 방향에 움직이는 {jo_iga(ko)} 있습니다."
            else:
                guide = f"주의. {clock} {ko}."
    else:
        detour = detour_direction(clock, safe_clocks)

        if detour is None:
            if is_dynamic and distance_m < ALERT_DISTANCE_M:
                action = "stop"
                priority = 1
                reason = "안전 우회 방향 없음"
                if sentence_length == "LONG":
                    guide = f"정지! {clock} 방향에 {jo_iga(ko)} 있습니다."
                else:
                    guide = f"정지! {clock} {ko}."
            else:
                action = "caution"
                priority = 2
                reason = f"{clock} 방향 {ko}"
                if sentence_length == "LONG":
                    guide = f"주의. {clock} 방향에 {jo_iga(ko)} 있습니다."
                else:
                    guide = f"주의. {clock} {ko}."
        else:
            action = "detour"
            priority = 2
            reason = f"{clock} 방향에 {ko}"
            if sentence_length == "LONG":
                guide = f"{clock} 방향에 {jo_iga(ko)} 있습니다. {detour} 방향으로 우회하세요."
            elif sentence_length == "MEDIUM":
                guide = f"주의. {clock} {ko}, {detour} 우회."
            else:
                guide = f"주의. {clock} {ko}."

    mark_announced(primary_tid, frame_idx)
    return _make_gt(primary, action, priority, reason, guide)


def _make_gt(
    primary: dict[str, Any],
    action: str,
    priority: int,
    reason: str,
    voice: str,
    primary_class_override: str | None = None,
    primary_class_ko_override: str | None = None,
) -> dict[str, Any]:
    cls_orig = primary_class_override or primary["class"]
    cls_ko = primary_class_ko_override or to_ko(primary["class"])

    return {
        "warning_needed": action != "go",
        "primary_object_id": primary["object_id"],
        "primary_object_class": cls_orig,
        "primary_object_class_ko": cls_ko,
        "clock_direction": primary["clock_direction"],
        "distance": primary["distance"],
        "distance_m": primary.get("distance_m"),
        "action": action,
        "priority": priority,
        "reason": reason,
        "voice_guide": voice,
    }


def _empty_gt(reason: str = "경로상 위험 없음") -> dict[str, Any]:
    return {
        "warning_needed": False,
        "primary_object_id": None,
        "primary_object_class": None,
        "primary_object_class_ko": None,
        "clock_direction": None,
        "distance": None,
        "distance_m": None,
        "action": "go",
        "priority": 4,
        "reason": reason,
        "voice_guide": "",
    }


def _append_scene_info(gt: dict[str, Any], scene: dict[str, Any], frame_idx: int, sentence_length: str = "MEDIUM") -> dict[str, Any]:
    sentence_length = normalize_sentence_length(sentence_length)
    extras = []
    added = []
    is_signal_guide = gt.get("primary_object_class") in {"traffic_light_red", "traffic_light_green"}

    if scene["crosswalk"]["exists"] and not is_signal_guide:
        if not is_scene_in_cooldown("crosswalk", frame_idx):
            extras.append("전방 횡단보도." if sentence_length == "SHORT" else "전방에 횡단보도가 있습니다.")
            added.append("crosswalk")
            mark_scene_announced("crosswalk", frame_idx)

    if scene["tactile_block"]["exists"]:
        if not is_scene_in_cooldown("tactile_block", frame_idx):
            extras.append("전방 점자블록." if sentence_length == "SHORT" else "전방에 점자블록이 있습니다.")
            added.append("tactile_block")
            mark_scene_announced("tactile_block", frame_idx)

    if extras:
        existing = gt.get("voice_guide", "") or ""
        gt["voice_guide"] = (existing + " " + " ".join(extras)).strip() if existing else " ".join(extras)

    gt["scene_info_added"] = added
    return gt


def alert_level_from_gt(gt: dict[str, Any]) -> str:
    action = gt.get("action")
    if action == "stop":
        return "high"
    if action in {"detour", "caution"}:
        return "medium"
    if gt.get("voice_guide"):
        return "low"
    return "safe"


# =========================================================
# Backend payload
# =========================================================
def build_backend_payload(
    user_id: str,
    processed_at: str,
    processing_time_ms: int,
    scene_json: dict[str, Any],
    gt: dict[str, Any],
) -> dict[str, Any]:
    return {
        "user_id": user_id,
        "status": "success",
        "processed_at": processed_at,
        "processing_time_ms": processing_time_ms,
        "guide_text": gt.get("voice_guide", ""),
        "primary_object_id": gt.get("primary_object_id"),
        "primary_object_class": gt.get("primary_object_class"),
        "clock_direction": gt.get("clock_direction"),
        "distance": gt.get("distance"),
        "alert_level": gt.get("alert_level", "safe"),
    }


def build_timeout_payload(
    user_id: str,
    processed_at: str,
    processing_time_ms: int,
) -> dict[str, Any]:
    return {
        "user_id": user_id,
        "status": "error",
        "processed_at": processed_at,
        "processing_time_ms": processing_time_ms,
        "error_code": "MODEL_TIMEOUT",
        "message": "inference timeout",
    }