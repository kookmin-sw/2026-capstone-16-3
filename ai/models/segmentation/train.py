"""
YOLOv8/YOLO26 segmentation training script.
AIHub Surface dataset (5 classes): walkable_surface, braille_guide_blocks,
caution_surface, roadway, crosswalk.

Usage (Google Colab):
    python train.py --src_root "/content/drive/MyDrive/capstone dataset" \
                    --out_root "/content/drive/MyDrive/aihub_surface_yolo_5cls_full" \
                    --project  "/content/drive/MyDrive/yolo_surface_runs"

Usage (local, data already prepared):
    python train.py --skip_prepare \
                    --out_root /path/to/dataset \
                    --project   /path/to/runs
"""

from __future__ import annotations

import argparse
import collections
import glob
import os
import random
import shutil
import xml.etree.ElementTree as ET

from tqdm import tqdm
from ultralytics import YOLO

# =========================================================
# Constants
# =========================================================
SEED = 42
TRAIN_RATIO = 0.8
VAL_RATIO = 0.1
TEST_RATIO = 0.1

CLASS_NAMES = {
    0: "walkable_surface",
    1: "braille_guide_blocks",
    2: "caution_surface",
    3: "roadway",
    4: "crosswalk",
}


# =========================================================
# Label mapping
# =========================================================
def canonical_raw_label(label: str, attr: str | None) -> str:
    label = label.strip().lower()
    attr = (attr or "").strip().lower()

    if label == "sidewalk":
        return f"sidewalk_{attr}" if attr else "sidewalk"

    if label == "braille_guide_blocks":
        if attr in ("normal", "damaged"):
            return f"braille_guide_blocks_{attr}"
        return "braille_guide_blocks"

    if label == "roadway":
        if attr in ("normal", "crosswalk"):
            return f"roadway_{attr}"
        return "roadway"

    if label == "alley":
        if attr in ("normal", "crosswalk", "speed_bump", "damaged"):
            return f"alley_{attr}"
        return "alley"

    if label == "caution_zone":
        if attr in ("stairs", "manhole", "tree_zone", "grating", "repair_zone"):
            return f"caution_zone_{attr}"
        return "caution_zone"

    if label == "bike_lane":
        return "bike_lane"

    return label


def map_raw_to_5class(raw: str) -> int | None:
    raw = raw.lower()

    if raw.startswith("braille_guide_blocks"):
        return 1

    if raw in ("roadway_crosswalk", "alley_crosswalk"):
        return 4

    if raw.startswith("roadway"):
        return 3

    if raw.startswith("bike_lane") or raw.startswith("caution_zone"):
        return 2

    if raw.startswith("sidewalk") or raw.startswith("alley"):
        return 0

    return None


# =========================================================
# XML → YOLO label conversion
# =========================================================
def _get_polygon_attr(poly: ET.Element) -> str | None:
    for child in poly:
        if child.tag == "attribute":
            return (child.text or "").strip()
    return None


def _parse_points(points_str: str) -> list[tuple[float, float]]:
    pts = []
    for p in points_str.split(";"):
        x, y = p.split(",")
        pts.append((float(x), float(y)))
    return pts


def _polygon_to_yolo_line(class_id: int, pts: list[tuple[float, float]], w: int, h: int) -> str:
    vals = [f"{x / w:.6f} {y / h:.6f}" for x, y in pts]
    return f"{class_id} " + " ".join(vals)


def collect_annotations_from_xml(xml_path: str) -> dict[str, list[str]]:
    """Parse a CVAT XML file and return {image_name: [yolo_label_lines]}."""
    tree = ET.parse(xml_path)
    root = tree.getroot()
    result: dict[str, list[str]] = {}

    for image_tag in root.findall("image"):
        img_name = image_tag.attrib["name"]
        w = int(float(image_tag.attrib["width"]))
        h = int(float(image_tag.attrib["height"]))

        lines: list[str] = []
        for poly in image_tag.findall("polygon"):
            label = poly.attrib["label"]
            points_str = poly.attrib["points"]
            attr = _get_polygon_attr(poly)

            raw = canonical_raw_label(label, attr)
            class_id = map_raw_to_5class(raw)

            if class_id is None:
                continue

            pts = _parse_points(points_str)
            if len(pts) < 3:
                continue

            lines.append(_polygon_to_yolo_line(class_id, pts, w, h))

        result[img_name] = lines

    return result


# =========================================================
# Dataset preparation
# =========================================================
def prepare_dataset(src_root: str, out_root: str) -> str:
    """Convert raw Surface dataset to YOLO format and create data.yaml.

    Returns the path to data.yaml.
    """
    for split in ("train", "val", "test"):
        os.makedirs(f"{out_root}/images/{split}", exist_ok=True)
        os.makedirs(f"{out_root}/labels/{split}", exist_ok=True)

    surface_dirs = sorted(glob.glob(os.path.join(src_root, "Surface_*_unzipped", "Surface_*")))
    print(f"Found {len(surface_dirs)} surface directories")

    random.seed(SEED)
    random.shuffle(surface_dirs)

    n_total = len(surface_dirs)
    n_train = int(n_total * TRAIN_RATIO)
    n_val = int(n_total * VAL_RATIO)

    splits = {
        "train": surface_dirs[:n_train],
        "val": surface_dirs[n_train : n_train + n_val],
        "test": surface_dirs[n_train + n_val :],
    }

    for split_name, dirs in splits.items():
        print(f"{split_name}: {len(dirs)} dirs")

    for split_name, dirs in splits.items():
        _process_split(dirs, split_name, out_root)

    yaml_path = _write_yaml(out_root)
    _print_stats(out_root)

    return yaml_path


def _process_split(surface_dir_list: list[str], split_name: str, out_root: str) -> None:
    img_out = f"{out_root}/images/{split_name}"
    lab_out = f"{out_root}/labels/{split_name}"
    xml_cache: dict[str, dict[str, list[str]]] = {}
    total_imgs = 0

    for sdir in tqdm(surface_dir_list, desc=f"Processing {split_name}"):
        xmls = glob.glob(os.path.join(sdir, "*.xml"))
        if len(xmls) != 1:
            print(f"[SKIP] XML count unexpected: {sdir} -> {xmls}")
            continue

        xml_path = xmls[0]
        if xml_path not in xml_cache:
            xml_cache[xml_path] = collect_annotations_from_xml(xml_path)
        ann_dict = xml_cache[xml_path]

        jpgs = sorted(glob.glob(os.path.join(sdir, "*.jpg"))) or sorted(
            glob.glob(os.path.join(sdir, "*.png"))
        )

        for img_path in jpgs:
            img_name = os.path.basename(img_path)
            txt_name = os.path.splitext(img_name)[0] + ".txt"
            lines = ann_dict.get(img_name, [])

            shutil.copy(img_path, os.path.join(img_out, img_name))
            with open(os.path.join(lab_out, txt_name), "w", encoding="utf-8") as f:
                f.write("\n".join(lines))

            total_imgs += 1

    print(f"  {split_name}: {total_imgs} images copied")


def _write_yaml(out_root: str) -> str:
    names_block = "\n".join(f"  {k}: {v}" for k, v in CLASS_NAMES.items())
    yaml_text = (
        f"path: {out_root}\n"
        "train: images/train\n"
        "val: images/val\n"
        "test: images/test\n"
        "\n"
        "names:\n"
        f"{names_block}\n"
    )
    yaml_path = os.path.join(out_root, "data.yaml")
    with open(yaml_path, "w", encoding="utf-8") as f:
        f.write(yaml_text)
    print(f"Saved data.yaml → {yaml_path}")
    return yaml_path


def _print_stats(out_root: str) -> None:
    for split in ("train", "val", "test"):
        n_img = len(glob.glob(f"{out_root}/images/{split}/*"))
        n_lab = len(glob.glob(f"{out_root}/labels/{split}/*"))
        print(f"  {split:5s}: {n_img} images, {n_lab} labels")

    label_files = glob.glob(f"{out_root}/labels/train/*.txt")
    cls_count: collections.Counter[int] = collections.Counter()
    for path in label_files:
        for line in open(path).readlines():
            if line.strip():
                cls_count[int(line.split()[0])] += 1
    print("Class distribution (train):", dict(sorted(cls_count.items())))


# =========================================================
# Training
# =========================================================
def train(yaml_path: str, args: argparse.Namespace) -> None:
    model = YOLO(args.weights)
    model.train(
        data=yaml_path,
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        device=args.device,
        cache=True,
        patience=15,
        workers=2,
        retina_masks=True,
        overlap_mask=True,
        mask_ratio=4,
        cls=0.5,
        fliplr=0.5,
        degrees=5.0,
        perspective=0.0003,
        close_mosaic=10,
        project=args.project,
        name=args.name,
    )


# =========================================================
# CLI
# =========================================================
def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="YOLOv8/YOLO26 segmentation trainer")

    parser.add_argument(
        "--src_root",
        default="/content/drive/MyDrive/capstone dataset",
        help="Root directory containing Surface_*_unzipped folders",
    )
    parser.add_argument(
        "--out_root",
        default="/content/drive/MyDrive/aihub_surface_yolo_5cls_full",
        help="Output directory for prepared YOLO dataset",
    )
    parser.add_argument(
        "--skip_prepare",
        action="store_true",
        help="Skip data preparation (dataset already exists at --out_root)",
    )
    parser.add_argument(
        "--weights",
        default="yolo26n-seg.pt",
        help="Pretrained weights to start from",
    )
    parser.add_argument("--epochs", type=int, default=80)
    parser.add_argument("--imgsz", type=int, default=480)
    parser.add_argument("--batch", type=int, default=8)
    parser.add_argument("--device", default="0", help="CUDA device id or 'cpu'")
    parser.add_argument(
        "--project",
        default="/content/drive/MyDrive/yolo_surface_runs",
        help="Directory to save training runs",
    )
    parser.add_argument(
        "--name",
        default="yolo26n_seg_surface_5cls_full",
        help="Run name (sub-directory inside --project)",
    )

    return parser.parse_args()


def main() -> None:
    args = parse_args()

    if args.skip_prepare:
        yaml_path = os.path.join(args.out_root, "data.yaml")
        if not os.path.exists(yaml_path):
            raise FileNotFoundError(
                f"data.yaml not found at {yaml_path}. "
                "Run without --skip_prepare to generate the dataset first."
            )
        print(f"Skipping data preparation, using existing dataset at {args.out_root}")
    else:
        print("=== Step 1: Preparing dataset ===")
        yaml_path = prepare_dataset(args.src_root, args.out_root)

    print("\n=== Step 2: Training ===")
    train(yaml_path, args)


if __name__ == "__main__":
    main()
