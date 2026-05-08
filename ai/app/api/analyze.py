from __future__ import annotations

from collections import defaultdict
from datetime import datetime
from pathlib import Path
import logging
import time

import cv2
import numpy as np
from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from app.core.config import AI_TIMEOUT_SECONDS, BACKEND_GUIDE_EVENT_URL, UPLOAD_DIR
from app.services import inference
from app.services.backend_client import send_guide_event_to_backend
from app.services.guide_builder import build_timeout_payload

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api", tags=["analyze"])

# user별 frame index. tracking/cooldown 판단에 사용.
_user_frame_counter: dict[str, int] = defaultdict(int)


def build_backend_event_payload(
    user_id: str,
    processed_at: str,
    processing_time_ms: int,
    gt: dict,
) -> dict:
    """
    백엔드와 합의한 전송용 payload.
    timings_ms, scene_json, gt 전체는 백엔드로 보내지 않고,
    Swagger 응답/터미널 로그에서만 확인한다.
    """
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


@router.post("/analyze")
async def analyze_image(
    user_id: str = Form(...),
    captured_at: str = Form(...),
    image: UploadFile = File(...),
):
    start_time = time.perf_counter()

    logger.info(
        "analyze request received | user_id=%s | filename=%s | content_type=%s | captured_at=%s",
        user_id,
        image.filename,
        image.content_type,
        captured_at,
    )

    # =========================================================
    # 1. 요청 검증
    # =========================================================
    if not image.filename:
        raise HTTPException(status_code=400, detail="이미지 파일이 없습니다.")

    if not image.content_type or not image.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="이미지 파일만 업로드 가능합니다.")

    try:
        parsed_captured_at = datetime.fromisoformat(captured_at)
    except ValueError:
        raise HTTPException(
            status_code=400,
            detail="captured_at은 ISO 8601 형식이어야 합니다. 예: 2026-03-28T19:30:15.123+09:00",
        )

    # =========================================================
    # 2. 이미지 읽기
    # =========================================================
    file_bytes = await image.read()
    np_arr = np.frombuffer(file_bytes, np.uint8)
    frame_bgr = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

    if frame_bgr is None:
        raise HTTPException(status_code=400, detail="이미지를 디코딩할 수 없습니다.")

    # =========================================================
    # 3. 업로드 이미지 저장
    # =========================================================
    timestamp_str = parsed_captured_at.strftime("%Y%m%dT%H%M%S_%f")
    extension = Path(image.filename).suffix if Path(image.filename).suffix else ".jpg"
    save_filename = f"{user_id}_{timestamp_str}{extension}"
    save_path = UPLOAD_DIR / save_filename

    try:
        with save_path.open("wb") as buffer:
            buffer.write(file_bytes)
    except Exception as exc:
        logger.exception("파일 저장 중 오류 | user_id=%s | save_path=%s", user_id, save_path)
        raise HTTPException(status_code=500, detail=f"파일 저장 중 오류가 발생했습니다: {exc}")

    # =========================================================
    # 4. 모델 로드 확인
    # =========================================================
    if not inference.is_model_loaded():
        logger.error("AI 모델이 아직 로드되지 않았습니다.")
        raise HTTPException(status_code=500, detail="AI 모델이 아직 로드되지 않았습니다.")

    # =========================================================
    # 5. frame index / image id 생성
    # =========================================================
    frame_idx = _user_frame_counter[user_id]
    _user_frame_counter[user_id] += 1
    image_id = f"{user_id}_{timestamp_str}"

    # =========================================================
    # 6. AI 추론
    # =========================================================
    try:
        result = inference.run_single_image_inference(
            frame_bgr=frame_bgr,
            image_id=image_id,
            frame_idx=frame_idx,
        )

        scene_json = result["scene_json"]
        gt = result["gt"]
        timings_ms = result.get("timings_ms", {})

        logger.info(
            "추론 완료 | user_id=%s | frame_idx=%d | det_count=%d | alert_level=%s | action=%s | guide_text=%s",
            user_id,
            frame_idx,
            result.get("det_count", 0),
            gt.get("alert_level"),
            gt.get("action"),
            gt.get("voice_guide", ""),
        )

    except Exception as exc:
        logger.exception("추론 중 오류 | user_id=%s", user_id)
        raise HTTPException(status_code=500, detail=f"추론 중 오류가 발생했습니다: {exc}")

    # =========================================================
    # 7. 처리 시간 / timeout 판단
    # =========================================================
    processing_time_ms = int((time.perf_counter() - start_time) * 1000)
    processed_at = datetime.now().astimezone().isoformat(timespec="milliseconds")
    is_timeout = processing_time_ms > int(AI_TIMEOUT_SECONDS * 1000)
    gt_alert_level = gt.get("alert_level", "safe")

    # 터미널 로그에만 상세 처리 시간 출력
    logger.info(
        "AI 처리시간 상세 | user_id=%s | frame_idx=%d | processing_time_ms=%d | timings_ms=%s",
        user_id,
        frame_idx,
        processing_time_ms,
        timings_ms,
    )

    # =========================================================
    # 8. 백엔드 전송용 payload 생성
    # =========================================================
    try:
        if is_timeout:
            # 백엔드 전송용 timeout payload는 최소 정보만 유지
            payload = build_timeout_payload(
                user_id=user_id,
                processed_at=processed_at,
                processing_time_ms=processing_time_ms,
            )
            payload["alert_level"] = gt_alert_level

            logger.warning(
                "AI timeout | user_id=%s | frame_idx=%d | processing_time_ms=%d | alert_level=%s | gt=%s | timings_ms=%s",
                user_id,
                frame_idx,
                processing_time_ms,
                gt_alert_level,
                gt,
                timings_ms,
            )
        else:
            # 백엔드와 합의한 형식
            payload = build_backend_event_payload(
                user_id=user_id,
                processed_at=processed_at,
                processing_time_ms=processing_time_ms,
                gt=gt,
            )

    except Exception as exc:
        logger.exception("payload 생성 중 오류 | user_id=%s", user_id)
        raise HTTPException(status_code=500, detail=f"payload 생성 중 오류가 발생했습니다: {exc}")

    logger.info(
        "백엔드 전송 payload 미리보기 | user_id=%s | payload=%s",
        user_id,
        payload,
    )

    # =========================================================
    # 9. 백엔드 전송
    # high/medium만 백엔드로 전송. low/safe는 서버 응답에는 포함하지만 전송하지 않음.
    # =========================================================
    should_send = gt_alert_level in {"high", "medium"}

    if should_send:
        try:
            backend_response = await send_guide_event_to_backend(
                payload=payload,
                backend_url=BACKEND_GUIDE_EVENT_URL,
            )
            logger.info(
                "백엔드 전송 성공 | user_id=%s | alert_level=%s | backend_url=%s",
                user_id,
                gt_alert_level,
                BACKEND_GUIDE_EVENT_URL,
            )
        except Exception as exc:
            logger.warning("백엔드 /api/guide/event 전송 실패 (무시하고 계속) | user_id=%s | error=%s", user_id, exc)
            backend_response = {"status": "failed", "reason": str(exc)}
    else:
        backend_response = {
            "status": "skipped",
            "reason": f"gt_alert_level={gt_alert_level}",
        }
        logger.info("백엔드 전송 스킵 | user_id=%s | alert_level=%s", user_id, gt_alert_level)

    # =========================================================
    # 10. Swagger / 프론트 확인용 응답
    # 여기에는 디버깅을 위해 scene_json, gt, timings_ms를 유지
    # =========================================================
    return {
        "status": "forwarded" if should_send else "skipped",
        "saved_filename": save_filename,
        "scene_json": scene_json,
        "gt": gt,
        "timings_ms": timings_ms,
        "processing_time_ms": processing_time_ms,
        "ai_result": payload,
        "backend_response": backend_response,
    }