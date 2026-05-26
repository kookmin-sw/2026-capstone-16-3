# YOLOv26 Segmentation — Surface Classification

시각장애인 보행 보조를 위한 노면 영역 분할 모델.  
AIHub 보행자 안전을 위한 노면 분류 데이터셋을 기반으로 YOLOv26n-seg를 파인튜닝했다.

---

## 클래스 정의

| ID | 클래스명 | 설명 |
|----|----------|------|
| 0 | `walkable_surface` | 보행 가능 구역 (인도, 골목) |
| 1 | `braille_guide_blocks` | 점자블록 (정상/손상 포함) |
| 2 | `caution_surface` | 주의 구역 (자전거도로, 위험구역) |
| 3 | `roadway` | 차도 |
| 4 | `crosswalk` | 횡단보도 |

---

## 데이터셋

- **출처**: AIHub — 보행자 안전을 위한 노면 영역 분류 데이터  
- **어노테이션 형식**: CVAT XML (polygon)  
- **분할 비율**: train 80% / val 10% / test 10%  
- **이미지 크기**: 480×480 (학습 기준)

### 원본 폴더 구조

```
capstone dataset/
├── Surface_001_unzipped/
│   └── Surface_001/
│       ├── *.jpg
│       └── *.xml       ← CVAT polygon 어노테이션
├── Surface_002_unzipped/
│   └── Surface_002/
│       ├── *.jpg
│       └── *.xml
└── ...
```

### YOLO 변환 후 구조

```
aihub_surface_yolo_5cls_full/
├── images/
│   ├── train/
│   ├── val/
│   └── test/
├── labels/
│   ├── train/
│   ├── val/
│   └── test/
└── data.yaml
```

---

## 학습 설정

| 항목 | 값 |
|------|-----|
| 베이스 모델 | `yolo26n-seg.pt` |
| Epochs | 80 |
| Image size | 480 |
| Batch size | 8 |
| Device | GPU (CUDA 0) |
| Early stopping patience | 15 |
| `retina_masks` | True |
| `overlap_mask` | True |
| `mask_ratio` | 4 |
| `cls` (분류 손실 가중치) | 0.5 |
| Augmentation — fliplr | 0.5 |
| Augmentation — degrees | 5.0 |
| Augmentation — perspective | 0.0003 |
| `close_mosaic` | 10 |

---

## 파일 구성

```
models/segmentation/
├── train.py                              ← 훈련 스크립트 (이 파일)
├── yolov26_segmentation_model_surface.ipynb  ← 탐색/실험 노트북
├── README.md                             ← 이 문서
└── best_yoloseg.pt                       ← 학습된 모델 가중치 (git 미포함)
```

> `best_yoloseg.pt`는 용량이 크므로 git에 포함하지 않는다.  
> 모델 파일은 아래 링크에서 다운로드한다. (링크 추가 예정)

---

## 실행 방법

### 1. 의존성 설치

```bash
pip install ultralytics tqdm
```

### 2. 데이터 준비 + 훈련 (처음 실행)

```bash
python train.py \
  --src_root "/content/drive/MyDrive/capstone dataset" \
  --out_root "/content/drive/MyDrive/aihub_surface_yolo_5cls_full" \
  --project  "/content/drive/MyDrive/yolo_surface_runs"
```

### 3. 데이터가 이미 준비된 경우 (훈련만)

```bash
python train.py \
  --skip_prepare \
  --out_root "/content/drive/MyDrive/aihub_surface_yolo_5cls_full" \
  --project  "/content/drive/MyDrive/yolo_surface_runs"
```

### 주요 인자

| 인자 | 기본값 | 설명 |
|------|--------|------|
| `--src_root` | (Colab 경로) | 원본 AIHub 데이터 루트 |
| `--out_root` | (Colab 경로) | YOLO 변환 데이터 출력 경로 |
| `--skip_prepare` | False | 데이터 변환 생략 (이미 준비된 경우) |
| `--weights` | `yolo26n-seg.pt` | 시작 가중치 |
| `--epochs` | 80 | 학습 에포크 수 |
| `--imgsz` | 480 | 입력 이미지 크기 |
| `--batch` | 8 | 배치 크기 |
| `--device` | `0` | CUDA 디바이스 번호 또는 `cpu` |
| `--project` | (Colab 경로) | 훈련 결과 저장 디렉토리 |
| `--name` | `yolo26n_seg_surface_5cls_full` | 실험 이름 |

---

## 서버 연동

학습된 모델은 `config.py`의 `YOLO_SEG_WEIGHTS` 경로에 배치한다.

```python
# AI/app/core/config.py
YOLO_SEG_WEIGHTS = BASE_DIR / "models" / "segmentation" / "best_yoloseg.pt"
```

서버 추론 파이프라인에서 노면 분할 결과는 점자블록 상태 판단 및 횡단보도 감지에 사용된다.
