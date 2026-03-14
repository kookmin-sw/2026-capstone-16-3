from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="AI Server")

class PredictRequest(BaseModel):
    text: str

class PredictResponse(BaseModel):
    result: str

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    # TODO: 여기서 모델 추론 연결 (예: torch / transformers / sklearn)
    out = f"you said: {req.text}"
    return PredictResponse(result=out)