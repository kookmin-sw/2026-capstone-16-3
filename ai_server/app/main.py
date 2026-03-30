from fastapi import FastAPI
from pydantic import BaseModel
from app.api.health import router as health_router
from app.api.analyze import router as analyze_router

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