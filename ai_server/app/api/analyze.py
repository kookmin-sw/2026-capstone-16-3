from datetime import datetime, timezone
from pathlib import Path
import shutil
import time

from fastapi import APIRouter, File, Form, UploadFile, HTTPException

from app.core.config import UPLOAD_DIR, BACKEND_GUIDE_EVENT_URL, AI_TIMEOUT_SECONDS
from app.services.backend_client import send_guide_event_to_backend

router = APIRouter(prefix="/api", tags=["analyze"])


def build_success_payload(
    user_id: str,
    frame_id: int,
    processing_time_ms: int
) -> dict:
    return {
        "user_id": user_id,
        "frame_id": frame_id,
        "status": "success",
        "processed_at": datetime.now().astimezone().isoformat(timespec="milliseconds"),
        "processing_time_ms": processing_time_ms,
        "guide_text": "1시 방향에 오토바이가 있습니다. 중앙을 유지하며 천천히 직진하세요.",
        "primary_object_class": "motorcycle",
        "clock_direction": "1시",
        "distance": "near",
        "alert_level": "high"
    }


def build_timeout_payload(
    user_id: str,
    frame_id: int,
    processing_time_ms: int
) -> dict:
    return {
        "user_id": user_id,
        "frame_id": frame_id,
        "status": "error",
        "processed_at": datetime.now().astimezone().isoformat(timespec="milliseconds"),
        "processing_time_ms": processing_time_ms,
        "error_code": "MODEL_TIMEOUT",
        "message": "inference timeout"
    }


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

    timestamp_str = parsed_captured_at.strftime("%Y%m%dT%H%M%S_%f")
    extension = Path(image.filename).suffix if Path(image.filename).suffix else ".jpg"
    save_filename = f"{user_id}_{frame_id}_{timestamp_str}{extension}"
    save_path = UPLOAD_DIR / save_filename

    try:
        with save_path.open("wb") as buffer:
            shutil.copyfileobj(image.file, buffer)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"파일 저장 중 오류가 발생했습니다: {str(e)}")
    finally:
        image.file.close()

    # -----------------------------
    # 여기서부터 실제 분석 로직이 들어갈 자리
    # 지금은 예시로 처리시간만 계산해서 가짜 결과 생성
    # -----------------------------
    processing_time_ms = int((time.perf_counter() - start_time) * 1000)

    if processing_time_ms > int(AI_TIMEOUT_SECONDS * 1000):
        payload = build_timeout_payload(
            user_id=user_id,
            frame_id=frame_id,
            processing_time_ms=processing_time_ms
        )
    else:
        payload = build_success_payload(
            user_id=user_id,
            frame_id=frame_id,
            processing_time_ms=processing_time_ms
        )

    # -----------------------------
    # 백엔드로 결과 전송
    # -----------------------------
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
        "ai_result": payload,
        "backend_response": backend_response
    }