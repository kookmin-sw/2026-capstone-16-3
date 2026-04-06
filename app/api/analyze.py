from datetime import datetime
from pathlib import Path
import time
import logging

import cv2
import numpy as np

from fastapi import APIRouter, File, Form, UploadFile, HTTPException

from app.core.config import UPLOAD_DIR, BACKEND_GUIDE_EVENT_URL, AI_TIMEOUT_SECONDS
from app.services.backend_client import send_guide_event_to_backend
from app.services import inference
from app.services.guide_builder import (
    build_scene_from_predictions,
    derive_gt,
    build_backend_payload,
    build_timeout_payload,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api", tags=["analyze"])


@router.post("/analyze")
async def analyze_image(
    user_id: str = Form(...),
    captured_at: str = Form(...),
    image: UploadFile = File(...)
):
    start_time = time.perf_counter()

    logger.info(
        "analyze request received | user_id=%s | filename=%s | content_type=%s | captured_at=%s",
        user_id,
        image.filename,
        image.content_type,
        captured_at,
    )

    if not image.filename:
        logger.warning("이미지 파일명이 비어 있습니다. | user_id=%s", user_id)
        raise HTTPException(status_code=400, detail="이미지 파일이 없습니다.")

    if not image.content_type or not image.content_type.startswith("image/"):
        logger.warning(
            "이미지 파일이 아닌 요청입니다. | user_id=%s | content_type=%s",
            user_id,
            image.content_type,
        )
        raise HTTPException(status_code=400, detail="이미지 파일만 업로드 가능합니다.")

    try:
        parsed_captured_at = datetime.fromisoformat(captured_at)
    except ValueError:
        logger.warning("captured_at 형식 오류 | user_id=%s | captured_at=%s", user_id, captured_at)
        raise HTTPException(
            status_code=400,
            detail="captured_at은 ISO 8601 형식이어야 합니다. 예: 2026-03-28T19:30:15.123+09:00"
        )

    # 1) 업로드 파일 읽기
    file_bytes = await image.read()
    np_arr = np.frombuffer(file_bytes, np.uint8)
    frame_bgr = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

    if frame_bgr is None:
        logger.warning("이미지 디코딩 실패 | user_id=%s | filename=%s", user_id, image.filename)
        raise HTTPException(status_code=400, detail="이미지를 디코딩할 수 없습니다.")

    # 2) 파일 저장
    timestamp_str = parsed_captured_at.strftime("%Y%m%dT%H%M%S_%f")
    extension = Path(image.filename).suffix if Path(image.filename).suffix else ".jpg"
    save_filename = f"{user_id}_{timestamp_str}{extension}"
    save_path = UPLOAD_DIR / save_filename

    try:
        with save_path.open("wb") as buffer:
            buffer.write(file_bytes)
    except Exception as e:
        logger.exception("파일 저장 중 오류 | user_id=%s | save_path=%s", user_id, save_path)
        raise HTTPException(status_code=500, detail=f"파일 저장 중 오류가 발생했습니다: {str(e)}")

    # 3) 모델 로드 확인
    if inference.seg is None:
        logger.error("SegFormer 모델이 아직 로드되지 않았습니다.")
        raise HTTPException(status_code=500, detail="SegFormer 모델이 아직 로드되지 않았습니다.")

    # 4) 실제 추론
    try:
        _, pred, det_rows = inference.run_single_image_inference(frame_bgr)
        logger.info("추론 완료 | user_id=%s | det_count=%d", user_id, len(det_rows))
    except Exception as e:
        logger.exception("추론 중 오류 | user_id=%s", user_id)
        raise HTTPException(status_code=500, detail=f"추론 중 오류가 발생했습니다: {str(e)}")

    # 5) scene json / gt 생성
    try:
        image_id = f"{user_id}_{captured_at}"

        scene_json = build_scene_from_predictions(
            image_id=image_id,
            pred=pred,
            det_rows=det_rows,
            seg_model=inference.seg,
        )

        gt = derive_gt(scene_json)
        logger.info(
            "scene/guide 생성 완료 | user_id=%s | alert_level=%s | warning_needed=%s",
            user_id,
            gt.get("alert_level"),
            gt.get("warning_needed"),
        )
    except Exception as e:
        logger.exception("scene/guide 생성 중 오류 | user_id=%s", user_id)
        raise HTTPException(status_code=500, detail=f"scene/guide 생성 중 오류가 발생했습니다: {str(e)}")

    # 6) 처리시간 계산
    processing_time_ms = int((time.perf_counter() - start_time) * 1000)
    processed_at = datetime.now().astimezone().isoformat(timespec="milliseconds")

    # 7) timeout 여부에 따라 payload 생성
    try:
        if processing_time_ms > int(AI_TIMEOUT_SECONDS * 1000):
            payload = build_timeout_payload(
                user_id=user_id,
                processed_at=processed_at,
                processing_time_ms=processing_time_ms,
            )
            logger.warning(
                "timeout payload 생성 | user_id=%s | processing_time_ms=%d",
                user_id,
                processing_time_ms,
            )
        else:
            payload = build_backend_payload(
                user_id=user_id,
                processed_at=processed_at,
                processing_time_ms=processing_time_ms,
                scene_json=scene_json,
                gt=gt,
            )
            logger.info(
                "success payload 생성 | user_id=%s | alert_level=%s | processing_time_ms=%d",
                user_id,
                payload.get("alert_level"),
                processing_time_ms,
            )
    except Exception as e:
        logger.exception("payload 생성 중 오류 | user_id=%s", user_id)
        raise HTTPException(status_code=500, detail=f"payload 생성 중 오류가 발생했습니다: {str(e)}")

    # 8) high / medium / low만 백엔드로 전송, safe는 스킵
    alert_level = payload.get("alert_level")
    should_send = alert_level in {"high", "medium", "low"}

    if should_send:
        try:
            backend_response = await send_guide_event_to_backend(
                payload=payload,
                backend_url=BACKEND_GUIDE_EVENT_URL
            )
            logger.info(
                "백엔드 전송 성공 | user_id=%s | alert_level=%s | backend_url=%s",
                user_id,
                alert_level,
                BACKEND_GUIDE_EVENT_URL,
            )
        except Exception as e:
            logger.exception(
                "백엔드 /api/guide/event 전송 실패 | user_id=%s | alert_level=%s",
                user_id,
                alert_level,
            )
            raise HTTPException(
                status_code=502,
                detail=f"백엔드 /api/guide/event 전송 실패: {str(e)}"
            )
    else:
        backend_response = {
            "status": "skipped",
            "reason": "alert_level is safe"
        }
        logger.info("백엔드 전송 스킵 | user_id=%s | alert_level=%s", user_id, alert_level)

    return {
        "status": "forwarded" if should_send else "skipped",
        "saved_filename": save_filename,
        "scene_json": scene_json,
        "gt": gt,
        "ai_result": payload,
        "backend_response": backend_response
    }