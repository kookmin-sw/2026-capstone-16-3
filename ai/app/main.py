from fastapi import FastAPI
from pydantic import BaseModel
from app.api.health import router as health_router
from app.api.analyze import router as analyze_router
from app.services.inference import load_models
from app.core.config import YOLO_MODEL_PATH, SEG_MODEL_DIR
import logging


app = FastAPI(title="AI Server")
app.include_router(health_router)
app.include_router(analyze_router)


class PredictRequest(BaseModel):
    text: str


class PredictResponse(BaseModel):
    result: str


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    out = f"you said: {req.text}"
    return PredictResponse(result=out)


@app.on_event("startup")
async def startup_event():
    load_models(
        yolo_pt=YOLO_MODEL_PATH,
        seg_model_dir=SEG_MODEL_DIR
    )

import logging
from fastapi import FastAPI

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(name)s | %(message)s"
)