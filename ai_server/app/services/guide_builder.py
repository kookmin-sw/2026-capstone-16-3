import numpy as np

# =========================================================
# 한글 클래스 맵
# =========================================================
CLASS_KO_MAP = {
    "bicycle": "자전거",
    "bus": "버스",
    "car": "차량",
    "motorcycle": "오토바이",
    "scooter": "스쿠터",
    "truck": "트럭",
    "person": "보행자",
    "wheelchair": "휠체어",
    "barricade": "바리케이드",
    "bench": "벤치",
    "bollard": "볼라드",
    "fire_hydrant": "소화전",
    "pole": "기둥",
    "stop": "표지판",
    "table": "탁자",
    "traffic_light": "신호등",
    "tree_trunk": "가로수",
}


def class_to_korean(cls_name: str) -> str:
    return CLASS_KO_MAP.get(cls_name, cls_name)


# =========================================================
# Seg label 유틸
# =========================================================
def resolve_label_ids(id2label: dict, keywords: list[str]) -> list[int]:
    ids = []
    for idx, name in id2label.items():
        label_name = str(name).lower()
        if any(keyword.lower() in label_name for keyword in keywords):
            ids.append(int(idx))
    return ids


def mask_from_ids(pred: np.ndarray, ids: list[int]) -> np.ndarray:
    if len(ids) == 0:
        return np.zeros_like(pred, dtype=bool)
    return np.isin(pred, ids)


def create_obstacle_mask(boxes: list[list[float]], height: int, width: int) -> np.ndarray:
    mask = np.zeros((height, width), dtype=bool)

    for box in boxes:
        x1, y1, x2, y2 = map(int, box)

        x1 = max(0, min(width - 1, x1))
        y1 = max(0, min(height - 1, y1))
        x2 = max(0, min(width, x2))
        y2 = max(0, min(height, y2))

        if x2 > x1 and y2 > y1:
            mask[y1:y2, x1:x2] = True

    return mask


# =========================================================
# Scene 해석 유틸
# =========================================================
def choose_free_space(walkable_mask: np.ndarray, obstacle_mask: np.ndarray) -> str:
    """
    하단 40% 영역에서 obstacle을 제외한 walkable column 합을 보고
    가장 여유 있는 방향을 선택
    """
    height, width = walkable_mask.shape
    candidate = walkable_mask & (~obstacle_mask)

    lower_half = candidate[int(height * 0.6):, :]
    col_sum = lower_half.sum(axis=0)

    if col_sum.sum() == 0:
        return "12시"

    idx = int(np.argmax(col_sum))
    ratio = idx / float(width)

    if ratio < 0.2:
        return "10시"
    if ratio < 0.4:
        return "11시"
    if ratio < 0.6:
        return "12시"
    if ratio < 0.8:
        return "1시"
    return "2시"


def road_boundary_from_masks(walkable_mask: np.ndarray, road_mask: np.ndarray) -> str:
    """
    간단 휴리스틱:
    - walkable이 거의 없으면 unknown
    - road 비율이 매우 크면 both_close로 간주
    - 아니면 open
    """
    if walkable_mask.sum() == 0:
        return "unknown"

    road_ratio = road_mask.mean()
    if road_ratio > 0.4:
        return "both_close"
    return "open"


def tactile_broken_heuristic(tactile_mask: np.ndarray) -> bool:
    """
    매우 단순한 휴리스틱:
    tactile이 존재하는데 하단부 연속성이 약하면 broken 추정
    """
    if tactile_mask.sum() == 0:
        return False

    height, width = tactile_mask.shape
    lower = tactile_mask[int(height * 0.55):, :]
    row_presence = lower.sum(axis=1) > 0
    fill_ratio = row_presence.mean()

    return fill_ratio < 0.35


# =========================================================
# 객체 선택 / 레벨
# =========================================================
def distance_rank(distance: str) -> int:
    if distance == "near":
        return 0
    if distance == "mid":
        return 1
    return 2


def rank_primary_object(objects_ranked: list[dict]) -> dict | None:
    if not objects_ranked:
        return None

    return sorted(
        objects_ranked,
        key=lambda obj: (
            -obj.get("guide_priority", 0),
            0 if obj.get("on_path", False) else 1,
            distance_rank(obj.get("distance", "far")),
            -obj.get("motion_risk", 0),
            0 if obj.get("narrows_path", False) else 1,
        )
    )[0]


def alert_level_from_priority(priority: int) -> str:
    if priority >= 7:
        return "high"
    if priority >= 5:
        return "medium"
    if priority >= 3:
        return "low"
    return "safe"


# =========================================================
# Scene JSON 생성
# =========================================================
def build_scene_from_predictions(image_id: str, pred: np.ndarray, det_rows: list[dict], seg_model) -> dict:
    height, width = pred.shape
    id2label = seg_model.config.id2label

    tactile_ids = resolve_label_ids(id2label, ["tactile", "braille"])
    walkway_ids = resolve_label_ids(id2label, ["sidewalk", "walkway", "footpath", "pavement", "path"])
    crosswalk_ids = resolve_label_ids(id2label, ["crosswalk", "zebra"])
    road_ids = resolve_label_ids(id2label, ["road", "street", "lane", "carriageway"])

    walkable_ids = sorted(set(tactile_ids + walkway_ids + crosswalk_ids))

    tactile_mask = mask_from_ids(pred, tactile_ids)
    walkable_mask = mask_from_ids(pred, walkable_ids)
    road_mask = mask_from_ids(pred, road_ids)

    obstacle_boxes = [obj["bbox"] for obj in det_rows]
    obstacle_mask = create_obstacle_mask(obstacle_boxes, height, width)

    free_space = choose_free_space(walkable_mask, obstacle_mask)
    road_boundary = road_boundary_from_masks(walkable_mask, road_mask)

    tactile_exists = bool(tactile_mask.sum() > 0)
    tactile_broken = tactile_broken_heuristic(tactile_mask)

    scene_objects = []
    for idx, row in enumerate(det_rows, start=1):
        obj = dict(row)
        obj["object_id"] = f"obj_{idx}"
        scene_objects.append(obj)

    scene_json = {
        "id": image_id,
        "scene": {
            "weather": "unknown",
            "surface": "unknown",
            "walkway_exists": bool(walkable_mask.sum() > 0),
            "tactile_block": {
                "exists": tactile_exists,
                "blocked": False,
                "broken": tactile_broken,
            },
            "objects": scene_objects,
            "free_space": free_space,
            "road_boundary": road_boundary,
        }
    }

    return scene_json


# =========================================================
# GT / guide 생성
# =========================================================
def build_safe_response(scene_json: dict) -> dict:
    free_space = scene_json["scene"]["free_space"]

    return {
        "warning_needed": False,
        "primary_object_id": None,
        "primary_object_class": None,
        "clock_direction": free_space,
        "distance": None,
        "action": "move_forward",
        "priority": 0,
        "reason": "no significant obstacle on path",
        "voice_guide": f"전방에 큰 장애물이 없습니다. {free_space} 방향으로 직진하세요.",
        "alert_level": "safe",
    }


def build_warning_response(scene_json: dict, primary: dict) -> dict:
    free_space = scene_json["scene"]["free_space"]
    cls_name = primary["class"]
    cls_kor = class_to_korean(cls_name)
    direction = primary["clock_direction"]
    distance = primary["distance"]
    priority = primary["guide_priority"]

    alert_level = alert_level_from_priority(priority)

    # 단순 action 규칙
    if free_space in {"10시", "11시"}:
        action = "move_left"
    elif free_space in {"1시", "2시"}:
        action = "move_right"
    else:
        action = "move_forward"

    if alert_level == "high":
        guide_text = f"{direction} 방향에 {cls_kor}이 있습니다. {free_space} 방향으로 즉시 이동하세요."
    elif alert_level == "medium":
        guide_text = f"{direction} 방향에 {cls_kor}이 있습니다. {free_space} 방향으로 피해 주세요."
    else:
        guide_text = f"{direction} 방향에 {cls_kor}이 있습니다. {free_space} 방향으로 주의하며 이동하세요."

    return {
        "warning_needed": True,
        "primary_object_id": primary["object_id"],
        "primary_object_class": cls_name,
        "clock_direction": direction,
        "distance": distance,
        "action": action,
        "priority": priority,
        "reason": f"primary obstacle is {cls_name} at {direction}, distance={distance}",
        "voice_guide": guide_text,
        "alert_level": alert_level,
    }


def derive_gt(scene_json: dict) -> dict:
    objects = scene_json["scene"]["objects"]

    if len(objects) == 0:
        return build_safe_response(scene_json)

    primary = rank_primary_object(objects)
    if primary is None:
        return build_safe_response(scene_json)

    # primary가 있지만 위험도가 너무 낮으면 safe 처리 가능
    if primary["guide_priority"] <= 2:
        return build_safe_response(scene_json)

    return build_warning_response(scene_json, primary)


# =========================================================
# 백엔드 전송용 payload 생성
# =========================================================
def build_backend_payload(
    user_id: str,
    frame_id: int,
    processed_at: str,
    processing_time_ms: int,
    scene_json: dict,
    gt: dict,
) -> dict:
    return {
        "user_id": user_id,
        "frame_id": frame_id,
        "status": "success",
        "processed_at": processed_at,
        "processing_time_ms": processing_time_ms,
        "guide_text": gt["voice_guide"],
        "primary_object_class": gt["primary_object_class"],
        "clock_direction": gt["clock_direction"],
        "distance": gt["distance"],
        "alert_level": gt["alert_level"],
    }


def build_timeout_payload(
    user_id: str,
    frame_id: int,
    processed_at: str,
    processing_time_ms: int,
) -> dict:
    return {
        "user_id": user_id,
        "frame_id": frame_id,
        "status": "error",
        "processed_at": processed_at,
        "processing_time_ms": processing_time_ms,
        "error_code": "MODEL_TIMEOUT",
        "message": "inference timeout",
    }