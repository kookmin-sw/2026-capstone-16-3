import logging

from fastapi import FastAPI
from pydantic import BaseModel

from app.api.health import router as health_router
from app.api.analyze import router as analyze_router
from app.services.inference import load_models
from app.core.config import (
    YOLO_DET_WEIGHTS,
    YOLO_SEG_WEIGHTS,
    DEPTH_MODEL_NAME,
)

# =========================================================
# Logging 설정
# =========================================================
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(name)s | %(message)s"
)

logger = logging.getLogger(__name__)

# =========================================================
# FastAPI App
# =========================================================
app = FastAPI(title="AI Server")

app.include_router(health_router)
app.include_router(analyze_router)


# =========================================================
# Test Predict API
# =========================================================
class PredictRequest(BaseModel):
    text: str


class PredictResponse(BaseModel):
    result: str


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    out = f"you said: {req.text}"
    return PredictResponse(result=out)


# =========================================================
# Startup: 모델 로드
# =========================================================
@app.on_event("startup")
async def startup_event():
    logger.info("AI 모델 로드 시작")

    load_models(
        yolo_det_weights=str(YOLO_DET_WEIGHTS),
        yolo_seg_weights=str(YOLO_SEG_WEIGHTS),
        depth_model_name=DEPTH_MODEL_NAME,
    )

    logger.info("AI 모델 로드 완료")