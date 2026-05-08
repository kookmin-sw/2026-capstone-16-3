from __future__ import annotations

import time
from typing import Any

import cv2
import numpy as np
import torch
from PIL import Image
from transformers import pipeline
import transformers.pipelines as hf_pipelines
from ultralytics import YOLO

from app.core.config import (
    CONF_DET,
    CONF_SEG,
    DEPTH_MODEL_NAME,
    IMG_SIZE,
    YOLO_DET_WEIGHTS,
    YOLO_SEG_WEIGHTS,
)
from app.services.guide_builder import build_scene_from_model_outputs

# =========================================================
# Device / global models
# =========================================================
device = "cuda" if torch.cuda.is_available() else "cpu"
depth_device = 0 if torch.cuda.is_available() else -1

det_model: YOLO | None = None
seg_model: YOLO | None = None
depth_pipe: Any | None = None

# Backward-compatible aliases. 기존 analyze.py/main.py가 inference.seg를 보던 구조 대응.
yolo: YOLO | None = None
seg: YOLO | None = None

# Swagger 단일 이미지 테스트에서는 track보다 predict가 안정적임
USE_TRACKING = False

# 서버 터미널에 YOLO 검출 결과 출력
DEBUG_YOLO_DETECTION = True


def _sync() -> None:
    if torch.cuda.is_available():
        torch.cuda.synchronize()


# =========================================================
# Debug util
# =========================================================
def debug_yolo_result(title: str, result, names) -> None:
    print(f"\n========== {title} ==========")

    if result.boxes is None or len(result.boxes) == 0:
        print("no boxes")
        return

    boxes = result.boxes.xyxy.cpu().numpy()
    confs = result.boxes.conf.cpu().numpy()
    clss = result.boxes.cls.cpu().numpy().astype(int)

    ids = None
    if getattr(result.boxes, "id", None) is not None and result.boxes.id is not None:
        ids = result.boxes.id.cpu().numpy().astype(int)

    for i, (box, conf, cls_id) in enumerate(zip(boxes, confs, clss)):
        name = names[int(cls_id)]
        tid = ids[i] if ids is not None else "-"
        print(
            f"id={tid} | class={name:<24s} | conf={float(conf):.3f} | "
            f"bbox={[round(float(v), 1) for v in box.tolist()]}"
        )


# =========================================================
# Model loading
# =========================================================
def load_models(
    yolo_det_weights: str | None = None,
    yolo_seg_weights: str | None = None,
    depth_model_name: str | None = None,
    **_: Any,
) -> None:
    """Load YOLO detection, YOLO segmentation, and metric depth models."""
    global det_model, seg_model, depth_pipe, yolo, seg

    det_weights = str(yolo_det_weights or YOLO_DET_WEIGHTS)
    seg_weights = str(yolo_seg_weights or YOLO_SEG_WEIGHTS)
    depth_name = depth_model_name or DEPTH_MODEL_NAME

    print("YOLO detection weights:", det_weights)
    print("YOLO segmentation weights:", seg_weights)

    det_model = YOLO(det_weights)
    seg_model = YOLO(seg_weights)

    # transformers pipeline 내부 torch 참조 오류 방지
    hf_pipelines.torch = torch

    depth_pipe = pipeline(
        task="depth-estimation",
        model=depth_name,
        device=depth_device,
    )

    yolo = det_model
    seg = seg_model

    print("YOLO detection classes:", det_model.names)
    print("YOLO segmentation classes:", seg_model.names)
    print("Depth model:", depth_name)
    print("Device:", device)
    print("USE_TRACKING:", USE_TRACKING)


def is_model_loaded() -> bool:
    return det_model is not None and seg_model is not None and depth_pipe is not None


def reset_tracker() -> None:
    """Reset Ultralytics internal tracker state.

    필요할 때만 사용. 서버에서 연속 프레임 tracking을 유지하려면 호출하지 않는다.
    """
    if det_model is not None:
        det_model.predictor = None


# =========================================================
# Depth
# =========================================================
def predict_depth(pil_image: Image.Image) -> np.ndarray:
    if depth_pipe is None:
        raise RuntimeError("Depth 모델이 로드되지 않았습니다.")

    out = depth_pipe(pil_image)
    depth = out["predicted_depth"]

    if hasattr(depth, "squeeze"):
        depth = depth.squeeze()

    if hasattr(depth, "detach"):
        depth = depth.detach().cpu().numpy()
    elif hasattr(depth, "cpu"):
        depth = depth.cpu().numpy()
    else:
        depth = np.asarray(depth)

    return depth.astype(np.float32)


# =========================================================
# Detection
# =========================================================
def run_yolo_detection(frame_bgr: np.ndarray):
    """YOLO detection 실행.

    - USE_TRACKING=False: predict() 사용
      Swagger 단일 이미지 테스트에 적합.
    - USE_TRACKING=True: track() 사용
      실제 연속 프레임 처리에서 ID 유지가 필요할 때 사용.
    """
    if det_model is None:
        raise RuntimeError("YOLO detection 모델이 로드되지 않았습니다.")

    if USE_TRACKING:
        result = det_model.track(
            source=frame_bgr,
            imgsz=IMG_SIZE,
            conf=CONF_DET,
            persist=True,
            tracker="bytetrack.yaml",
            save=False,
            verbose=False,
        )[0]
    else:
        result = det_model.predict(
            source=frame_bgr,
            imgsz=IMG_SIZE,
            conf=CONF_DET,
            save=False,
            verbose=False,
        )[0]

    if DEBUG_YOLO_DETECTION:
        mode = "TRACK" if USE_TRACKING else "PREDICT"
        debug_yolo_result(f"SERVER YOLO {mode}", result, det_model.names)

    return result


# =========================================================
# Main inference pipeline
# =========================================================
def run_single_image_inference(
    frame_bgr: np.ndarray,
    image_id: str | None = None,
    frame_idx: int = 0,
) -> dict[str, Any]:
    """Run the integrated pipeline for a single frame.

    반환:
    {
      "scene_json": {"id", "frame_idx", "scene"},
      "gt": {...},
      "timings_ms": {...},
      "det_count": int,
    }
    """
    if not is_model_loaded():
        raise RuntimeError("모델이 아직 로드되지 않았습니다.")

    if frame_bgr is None:
        raise ValueError("frame_bgr is None")

    assert det_model is not None
    assert seg_model is not None

    timings: dict[str, float] = {}
    total_start = time.perf_counter()

    img_h, img_w = frame_bgr.shape[:2]
    pil_img = Image.fromarray(cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB))

    # =====================================================
    # 1. YOLO segmentation
    # =====================================================
    _sync()
    t0 = time.perf_counter()
    seg_result = seg_model.predict(
        source=frame_bgr,
        imgsz=IMG_SIZE,
        conf=CONF_SEG,
        save=False,
        verbose=False,
        retina_masks=True,
    )[0]
    _sync()
    timings["seg_inference"] = (time.perf_counter() - t0) * 1000

    # =====================================================
    # 2. YOLO detection
    #    기존 track() 대신 기본 predict() 사용
    # =====================================================
    _sync()
    t0 = time.perf_counter()
    det_result = run_yolo_detection(frame_bgr)
    _sync()
    timings["det_inference"] = (time.perf_counter() - t0) * 1000

    if hasattr(det_result, "speed") and det_result.speed:
        timings["det_preprocess_only"] = float(det_result.speed.get("preprocess", 0.0))
        timings["det_inference_only"] = float(det_result.speed.get("inference", 0.0))
        timings["det_postprocess_only"] = float(det_result.speed.get("postprocess", 0.0))

    # =====================================================
    # 3. Depth estimation
    # =====================================================
    _sync()
    t0 = time.perf_counter()
    depth_m = predict_depth(pil_img)
    _sync()
    timings["depth_inference"] = (time.perf_counter() - t0) * 1000

    # =====================================================
    # 4. Scene + guide build
    # =====================================================
    t0 = time.perf_counter()
    packed = build_scene_from_model_outputs(
        image_id=image_id or "frame",
        frame_idx=frame_idx,
        img_w=img_w,
        img_h=img_h,
        det_result=det_result,
        seg_result=seg_result,
        depth_m=depth_m,
        det_class_names=det_model.names,
    )
    timings["scene_guide_build"] = (time.perf_counter() - t0) * 1000

    timings["total"] = (time.perf_counter() - total_start) * 1000

    scene_json = {
        "id": packed["id"],
        "frame_idx": packed["frame_idx"],
        "scene": packed["scene"],
    }
    gt = packed["gt"]

    det_count = len(scene_json["scene"].get("objects", []))

    return {
        "scene_json": scene_json,
        "gt": gt,
        "timings_ms": {key: round(value, 1) for key, value in timings.items()},
        "det_count": det_count,
    }