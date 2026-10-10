from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
import random
import subprocess
import sys
import time
import urllib.request
import zipfile
from dataclasses import dataclass
from io import BytesIO
from pathlib import Path

import numpy as np
import torch
from torch import nn
from torch.nn import functional as F
from torch.utils.data import DataLoader, TensorDataset

DATASET_URL = "https://zenodo.org/api/records/6488321/files/ReaLISED_Dataset.zip/content"
DATASET_SIZE = 280_551_607
DATASET_MD5 = "fd75c906a022a2c82890547152867504"
DATASET_DOI = "10.5281/zenodo.6488321"
SOURCE_NAMES = {
    "bea": "beater", "coo": "cooking", "cup": "cupboard_wardrobe",
    "dis": "dishwasher", "doo": "door", "dra": "drawer",
    "fur": "furniture_movement", "mic": "microwave", "obj": "object_falling",
    "smo": "smoke_extractor", "spe": "speech", "swi": "switch",
    "tel": "television", "vac": "vacuum_cleaner", "wal": "walking",
    "was": "washing_machine", "wat": "water_tap", "win": "window",
}
CLASS_NAMES = list(SOURCE_NAMES.values())
SAMPLE_RATE = 44_100
CLIP_SECONDS = 2
N_FFT = 2_048
HOP_LENGTH = 512
FREQUENCY_BANDS = 64


@dataclass(frozen=True)
class Record:
    path: str
    label: int
    group: str


def parse_record(path: str) -> Record:
    name = Path(path).name
    if not name.lower().endswith(".flac"):
        raise ValueError(f"not a FLAC recording: {name}")
    parts = name[:-5].split("_")
    if len(parts) != 5:
        raise ValueError(f"unexpected ReaLISED filename: {name}")
    source, event_id, action, material, intensity = parts
    if source not in SOURCE_NAMES:
        raise ValueError(f"unknown ReaLISED event class in {name}")
    if (len(event_id) != 3 or not event_id.isdigit() or
            not 1 <= int(event_id) <= 190):
        raise ValueError(f"invalid ReaLISED event identifier in {name}")
    if (len(action) != 2 or not action.isdigit() or int(action) > 10 or
            len(material) != 2 or not material.isdigit() or int(material) > 13 or
            len(intensity) != 1 or not intensity.isdigit() or int(intensity) > 3):
        raise ValueError(f"invalid ReaLISED annotation fields in {name}")
    return Record(path=path, label=list(SOURCE_NAMES).index(source),
                  group=f"{source}_{event_id}")


def split_records(records: list[Record], seed: int) -> dict[str, list[Record]]:
    by_class: dict[int, dict[str, list[Record]]] = {}
    for record in records:
        by_class.setdefault(record.label, {}).setdefault(record.group, []).append(record)

    splits = {"train": [], "validation": [], "test": []}
    for label, groups in sorted(by_class.items()):
        group_ids = sorted(groups)
        random.Random(seed + label).shuffle(group_ids)
        if len(group_ids) < 3:
            raise ValueError(f"class {CLASS_NAMES[label]} has fewer than three event groups")
        test_count = max(1, round(len(group_ids) * 0.15))
        validation_count = max(1, round(len(group_ids) * 0.15))
        if test_count + validation_count >= len(group_ids):
            raise ValueError(f"class {CLASS_NAMES[label]} cannot populate all splits")
        test_ids = set(group_ids[:test_count])
        validation_ids = set(group_ids[test_count:test_count + validation_count])
        for group_id, members in groups.items():
            destination = ("test" if group_id in test_ids else
                           "validation" if group_id in validation_ids else "train")
            splits[destination].extend(members)

    all_records = [record for records_in_split in splits.values()
                   for record in records_in_split]
    if sorted(record.path for record in all_records) != sorted(record.path for record in records):
        raise AssertionError("split lost or duplicated recordings")
    group_sets = {key: {record.group for record in values} for key, values in splits.items()}
    if (group_sets["train"] & group_sets["validation"] or
            group_sets["train"] & group_sets["test"] or
            group_sets["validation"] & group_sets["test"]):
        raise AssertionError("event group crosses dataset splits")
    return splits


def verify_archive(path: Path) -> tuple[str, str]:
    if path.stat().st_size != DATASET_SIZE:
        raise ValueError(f"archive size mismatch: expected {DATASET_SIZE} bytes")
    md5 = hashlib.md5()
    sha256 = hashlib.sha256()
    with path.open("rb") as archive:
        for block in iter(lambda: archive.read(1024 * 1024), b""):
            md5.update(block)
            sha256.update(block)
    if md5.hexdigest() != DATASET_MD5:
        raise ValueError("archive MD5 does not match the Zenodo record")
    return md5.hexdigest(), sha256.hexdigest()


def download_archive(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".part")
    digest = hashlib.md5()
    size = 0
    try:
        with urllib.request.urlopen(DATASET_URL, timeout=60) as response, temporary.open("wb") as out:
            while block := response.read(1024 * 1024):
                out.write(block)
                digest.update(block)
                size += len(block)
        if size != DATASET_SIZE or digest.hexdigest() != DATASET_MD5:
            raise ValueError("downloaded archive does not match Zenodo size and MD5")
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)


def read_records(archive: zipfile.ZipFile) -> list[Record]:
    flac_paths = sorted(name for name in archive.namelist()
                        if name.lower().endswith(".flac"))
    records = [parse_record(name) for name in flac_paths]
    if len(records) != 2_479:
        raise ValueError(f"expected 2,479 recordings, found {len(records)}")
    if len({record.path for record in records}) != len(records):
        raise ValueError("archive contains duplicate recording paths")
    if {record.label for record in records} != set(range(len(CLASS_NAMES))):
        raise ValueError("archive does not contain all 18 documented event classes")
    return records


def extract_features(archive: zipfile.ZipFile, records: list[Record]) -> tuple[torch.Tensor, dict[str, int]]:
    try:
        import soundfile as sf
    except ImportError as error:
        if not os.environ.get("KAGGLE_KERNEL_RUN_TYPE"):
            raise RuntimeError(
                "Install the pinned dependency: pip install soundfile==0.13.1"
            ) from error
        subprocess.check_call([sys.executable, "-m", "pip", "install",
                               "--quiet", "soundfile==0.13.1"])
        import soundfile as sf

    target_length = SAMPLE_RATE * CLIP_SECONDS
    window = torch.hann_window(N_FFT)
    features = []
    channel_counts: dict[str, int] = {}
    for record in records:
        data, sample_rate = sf.read(BytesIO(archive.read(record.path)),
                                    dtype="float32", always_2d=True)
        if (sample_rate != SAMPLE_RATE or data.shape[1] not in (1, 2) or
                data.shape[0] == 0):
            raise ValueError(f"unexpected audio format in {record.path}")
        channels = str(data.shape[1])
        channel_counts[channels] = channel_counts.get(channels, 0) + 1
        mono = data.mean(axis=1)
        if mono.size > target_length:
            start = (mono.size - target_length) // 2
            mono = mono[start:start + target_length]
        elif mono.size < target_length:
            mono = np.pad(mono, (0, target_length - mono.size))
        waveform = torch.from_numpy(np.asarray(mono, dtype=np.float32).copy())
        waveform = waveform - waveform.mean()
        waveform = waveform / waveform.square().mean().sqrt().clamp_min(1e-6)
        spectrum = torch.stft(waveform, n_fft=N_FFT, hop_length=HOP_LENGTH,
                              window=window, return_complex=True).abs()
        spectrum = torch.log1p(spectrum)
        bands = F.adaptive_avg_pool2d(
            spectrum.unsqueeze(0).unsqueeze(0),
            (FREQUENCY_BANDS, spectrum.shape[1])).squeeze(0)
        bands = (bands - bands.mean()) / bands.std(unbiased=False).clamp_min(1e-5)
        features.append(bands)
    return torch.stack(features), channel_counts


class EventClassifier(nn.Module):
    def __init__(self, class_count: int):
        super().__init__()
        self.network = nn.Sequential(
            nn.Conv2d(1, 16, 3, padding=1), nn.BatchNorm2d(16), nn.GELU(),
            nn.MaxPool2d(2),
            nn.Conv2d(16, 32, 3, padding=1), nn.BatchNorm2d(32), nn.GELU(),
            nn.MaxPool2d(2),
            nn.Conv2d(32, 64, 3, padding=1), nn.GELU(),
            nn.AdaptiveAvgPool2d(1), nn.Flatten(), nn.Linear(64, class_count),
        )

    def forward(self, inputs: torch.Tensor) -> torch.Tensor:
        return self.network(inputs)


def classification_metrics(truth: list[int], predicted: list[int], class_count: int) -> dict:
    confusion = [[0 for _ in range(class_count)] for _ in range(class_count)]
    for actual, guess in zip(truth, predicted, strict=True):
        confusion[actual][guess] += 1
    per_class_f1 = []
    supports = []
    for label in range(class_count):
        tp = confusion[label][label]
        fp = sum(confusion[actual][label] for actual in range(class_count)) - tp
        fn = sum(confusion[label]) - tp
        supports.append(sum(confusion[label]))
        per_class_f1.append(2 * tp / max(1, 2 * tp + fp + fn))
    total = sum(supports)
    accuracy = sum(confusion[label][label] for label in range(class_count)) / max(1, total)
    return {"accuracy": accuracy, "macro_f1": sum(per_class_f1) / class_count,
            "support_by_class": dict(zip(CLASS_NAMES, supports, strict=True)),
            "confusion_matrix": confusion}


def evaluate(model: nn.Module, features: torch.Tensor, labels: torch.Tensor,
             indices: list[int], device: torch.device) -> dict:
    model.eval()
    predicted = []
    truth = []
    with torch.inference_mode():
        for batch_start in range(0, len(indices), 64):
            batch_indices = indices[batch_start:batch_start + 64]
            logits = model(features[batch_indices].to(device))
            predicted.extend(logits.argmax(1).cpu().tolist())
            truth.extend(labels[batch_indices].tolist())
    return classification_metrics(truth, predicted, len(CLASS_NAMES))


def default_artifacts_path(script_file: Path, on_kaggle: bool) -> Path:
    if on_kaggle:
        return Path("/kaggle/working")
    return script_file.resolve().parents[3] / "sdk/training/artifacts"


def main() -> None:
    on_kaggle = (bool(os.environ.get("KAGGLE_KERNEL_RUN_TYPE")) or
                 Path("/kaggle/working").is_dir())
    default_artifacts = default_artifacts_path(Path(__file__), on_kaggle)
    parser = argparse.ArgumentParser(description="Train an auxiliary ReaLISED event classifier")
    parser.add_argument("--archive", type=Path,
                        default=default_artifacts / "ReaLISED_Dataset.zip")
    parser.add_argument("--out", type=Path,
                        default=default_artifacts / "realised-event-classifier")
    parser.add_argument("--epochs", type=int, default=25)
    parser.add_argument("--seed", type=int, default=20261010)
    args = parser.parse_args()
    if args.epochs < 1:
        parser.error("--epochs must be positive")
    if on_kaggle and not torch.cuda.is_available():
        raise RuntimeError("Kaggle allocated no GPU despite GPU-enabled metadata")

    random.seed(args.seed)
    np.random.seed(args.seed)
    torch.manual_seed(args.seed)
    torch.set_num_threads(min(8, os.cpu_count() or 1))
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(args.seed)
    started = time.monotonic()
    if not args.archive.exists():
        download_archive(args.archive)
    archive_md5, archive_sha256 = verify_archive(args.archive)
    with zipfile.ZipFile(args.archive) as archive:
        records = read_records(archive)
        splits = split_records(records, args.seed)
        features, channel_counts = extract_features(archive, records)

    labels = torch.tensor([record.label for record in records], dtype=torch.long)
    record_indices = {record.path: index for index, record in enumerate(records)}
    split_indices = {name: [record_indices[record.path] for record in values]
                     for name, values in splits.items()}
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = EventClassifier(len(CLASS_NAMES)).to(device)
    train_labels = labels[split_indices["train"]]
    class_counts = torch.bincount(train_labels, minlength=len(CLASS_NAMES)).float()
    class_weights = (len(train_labels) / (len(CLASS_NAMES) * class_counts)).to(device)
    criterion = nn.CrossEntropyLoss(weight=class_weights)
    optimizer = torch.optim.AdamW(model.parameters(), lr=1e-3, weight_decay=1e-4)
    train_dataset = TensorDataset(features[split_indices["train"]], train_labels)
    train_loader = DataLoader(train_dataset, batch_size=32, shuffle=True,
                              generator=torch.Generator().manual_seed(args.seed))

    best_state = None
    best_validation_f1 = -1.0
    best_epoch = 0
    stale_epochs = 0
    for epoch in range(1, args.epochs + 1):
        model.train()
        for batch_features, batch_labels in train_loader:
            batch_features = batch_features.to(device)
            batch_labels = batch_labels.to(device)
            optimizer.zero_grad(set_to_none=True)
            loss = criterion(model(batch_features), batch_labels)
            loss.backward()
            optimizer.step()
        validation = evaluate(model, features, labels, split_indices["validation"], device)
        print(json.dumps({"epoch": epoch, "validation_accuracy": validation["accuracy"],
                          "validation_macro_f1": validation["macro_f1"]}))
        if validation["macro_f1"] > best_validation_f1:
            best_validation_f1 = validation["macro_f1"]
            best_epoch = epoch
            best_state = copy.deepcopy(model.state_dict())
            stale_epochs = 0
        else:
            stale_epochs += 1
            if stale_epochs >= 5:
                break

    if best_state is None:
        raise RuntimeError("training produced no selectable checkpoint")
    model.load_state_dict(best_state)
    test_metrics = evaluate(model, features, labels, split_indices["test"], device)
    validation_metrics = evaluate(model, features, labels,
                                  split_indices["validation"], device)

    args.out.mkdir(parents=True, exist_ok=True)
    model_path = args.out / "realised-event-classifier.pt"
    torch.save({"state_dict": model.state_dict(), "class_names": CLASS_NAMES,
                "sample_rate": SAMPLE_RATE, "clip_seconds": CLIP_SECONDS,
                "n_fft": N_FFT, "hop_length": HOP_LENGTH,
                "frequency_bands": FREQUENCY_BANDS}, model_path)
    model_sha256 = hashlib.sha256(model_path.read_bytes()).hexdigest()
    report = {
        "purpose": "auxiliary indoor sound-event classification research only",
        "dataset": {"name": "ReaLISED",
                    "title": "Real-Life Indoor Sound Event Dataset for Sound Event Classification",
                    "doi": DATASET_DOI, "attribution": "Mohino-Herranz et al.",
                    "license": "CC BY 4.0",
                    "license_source": "https://zenodo.org/api/records/6488321",
                    "license_text": "https://creativecommons.org/licenses/by/4.0/",
                    "archive_md5": archive_md5, "archive_sha256": archive_sha256,
                    "recordings": len(records), "classes": CLASS_NAMES,
                    "sample_rate": SAMPLE_RATE, "channels_by_recording": channel_counts},
        "split": {"seed": args.seed, "group": "event identifier within source class",
                  "counts": {name: len(values) for name, values in splits.items()},
                  "object_or_session_separated": False,
                  "limitation": "source object/session identifiers are not provided"},
        "model": {"architecture": "three-block 2D CNN on per-clip normalized log-magnitude spectrograms",
                  "sha256": model_sha256, "best_epoch": best_epoch,
                  "device": str(device), "epochs_requested": args.epochs},
        "validation": validation_metrics,
        "test": test_metrics,
        "training_seconds": round(time.monotonic() - started, 3),
        "capybara_integrity_evidence": False,
        "android_deployment_candidate": False,
    }
    (args.out / "training-report.json").write_text(
        json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"test_accuracy": test_metrics["accuracy"],
                      "test_macro_f1": test_metrics["macro_f1"],
                      "model_sha256": model_sha256, "output": str(args.out)}))


if __name__ == "__main__":
    main()
