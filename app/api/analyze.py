from datetime import datetime
from pathlib import Path
import time

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

router = APIRouter(prefix="/api", tags=["analyze"])


@router.post("/analyze")
async def analyze_image(
    user_id: str = Form(...),
    frame_id: int = Form(...),
    captured_at: str = Form(...),
    image: UploadFile = File(...)
):
    start_time = time.perf_counter()

    if not image.filename:
        raise HTTPException(status_code=400, detail="이미지 파일이 없습니다.")

    if not image.content_type or not image.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="이미지 파일만 업로드 가능합니다.")

    try:
        parsed_captured_at = datetime.fromisoformat(captured_at)
    except ValueError:
        raise HTTPException(
            status_code=400,
            detail="captured_at은 ISO 8601 형식이어야 합니다. 예: 2026-03-28T19:30:15.123+09:00"
        )

    # 1) 업로드 파일 읽기
    file_bytes = await image.read()
    np_arr = np.frombuffer(file_bytes, np.uint8)
    frame_bgr = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

    if frame_bgr is None:
        raise HTTPException(status_code=400, detail="이미지를 디코딩할 수 없습니다.")

    # 2) 파일 저장
    timestamp_str = parsed_captured_at.strftime("%Y%m%dT%H%M%S_%f")
    extension = Path(image.filename).suffix if Path(image.filename).suffix else ".jpg"
    save_filename = f"{user_id}_{frame_id}_{timestamp_str}{extension}"
    save_path = UPLOAD_DIR / save_filename

    try:
        with save_path.open("wb") as buffer:
            buffer.write(file_bytes)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"파일 저장 중 오류가 발생했습니다: {str(e)}")

    # 3) 모델 로드 확인
    if inference.seg is None:
        raise HTTPException(status_code=500, detail="SegFormer 모델이 아직 로드되지 않았습니다.")

    # 4) 실제 추론
    try:
        _, pred, det_rows = inference.run_single_image_inference(frame_bgr)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"추론 중 오류가 발생했습니다: {str(e)}")

    # 5) scene json / gt 생성
    try:
        image_id = f"{user_id}_{frame_id}"

        scene_json = build_scene_from_predictions(
            image_id=image_id,
            pred=pred,
            det_rows=det_rows,
            seg_model=inference.seg,
        )

        gt = derive_gt(scene_json)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"scene/guide 생성 중 오류가 발생했습니다: {str(e)}")

    # 6) 처리시간 계산
    processing_time_ms = int((time.perf_counter() - start_time) * 1000)
    processed_at = datetime.now().astimezone().isoformat(timespec="milliseconds")

    # 7) timeout 여부에 따라 payload 생성
    if processing_time_ms > int(AI_TIMEOUT_SECONDS * 1000):
        payload = build_timeout_payload(
            user_id=user_id,
            frame_id=frame_id,
            processed_at=processed_at,
            processing_time_ms=processing_time_ms,
        )
    else:
        payload = build_backend_payload(
            user_id=user_id,
            frame_id=frame_id,
            processed_at=processed_at,
            processing_time_ms=processing_time_ms,
            scene_json=scene_json,
            gt=gt,
        )

    #8) 백엔드로 결과 전송
    try:
        backend_response = await send_guide_event_to_backend(
            payload=payload,
            backend_url=BACKEND_GUIDE_EVENT_URL
        )
    except Exception as e:
        raise HTTPException(
            status_code=502,
            detail=f"백엔드 /api/guide/event 전송 실패: {str(e)}"
        )

    return {
        "status": "forwarded",
        "saved_filename": save_filename,
        "scene_json": scene_json,
        "gt": gt,
        "ai_result": payload,
        "backend_response": backend_response
    }