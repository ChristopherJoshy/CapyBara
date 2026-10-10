from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
import random
import shutil
import tempfile
import time
import urllib.request
import zipfile
from dataclasses import dataclass
from io import BytesIO
from pathlib import Path, PurePosixPath

import numpy as np
import soundfile as sf
import torch
from rosbags.highlevel import AnyReader
from torch import nn
from torch.nn import functional as F
from torch.utils.data import DataLoader, TensorDataset

# rosbags 0.11.6 uses PosixPath for ROS message-type names. On Windows this
# concrete path class cannot be instantiated; PurePosixPath has the needed API.
if os.name == "nt":
    import rosbags.typesys.msg as rosbags_msg

    rosbags_msg.PosixPath = PurePosixPath

DATASET_URL = "https://zenodo.org/api/records/6372438/files/annotated_bags_mml.zip/content"
DATASET_DOI = "10.5281/zenodo.6372438"
DATASET_SIZE = 795_903_546
DATASET_MD5 = "60ac6f12924764dc813977c053934c98"
CLASSES = ("empty", "gummies", "rice")
CLASS_INDEX = {name: index for index, name in enumerate(CLASSES)}
AUDIO_TOPIC = "/audio/audio"
AUDIO_INFO_TOPIC = "/audio/audio_info"
TACTILE_TOPIC = "/tekscan/frame"
SAMPLE_RATE = 16_000
AUDIO_SECONDS = 8
N_FFT = 1_024
HOP_LENGTH = 256
AUDIO_FEATURE_SHAPE = (64, 128)
WINDOW = torch.hann_window(N_FFT)
TACTILE_SHAPE = (46, 64)


@dataclass(frozen=True)
class Record:
    path: str
    date: str
    motion: str
    label: int
    trial: int


def parse_record(path: str) -> Record | None:
    name = PurePosixPath(path).name
    if not name.endswith(".bag") or name.startswith("._"):
        return None
    parts = name.removesuffix(".bag").split("_")
    if len(parts) != 4 or parts[1] not in ("vertical", "rotation"):
        return None
    date, motion, content, trial_text = parts
    if content not in CLASS_INDEX:
        return None
    if not date.isdigit() or not trial_text.isdigit() or not 0 <= int(trial_text) < 30:
        raise ValueError(f"invalid EPFL trial filename: {name}")
    if (date, motion) not in (("20210825", "vertical"), ("20210826", "rotation")):
        return None
    return Record(path, date, motion, CLASS_INDEX[content], int(trial_text))


def read_records(archive: zipfile.ZipFile) -> list[Record]:
    records = [record for name in archive.namelist()
               if (record := parse_record(name)) is not None]
    keys = [(record.date, record.motion, record.label, record.trial) for record in records]
    expected = {(date, motion, label, trial)
                for date, motion in (("20210825", "vertical"), ("20210826", "rotation"))
                for label in range(len(CLASSES)) for trial in range(0, 30)}
    if len(keys) != len(expected) or set(keys) != expected:
        raise ValueError("EPFL archive does not contain exactly 30 trials per selected class and split")
    return sorted(records, key=lambda row: (row.date, row.label, row.trial))


def split_records(records: list[Record], seed: int) -> dict[str, list[Record]]:
    splits = {name: [] for name in ("train", "validation", "test")}
    for label in range(len(CLASSES)):
        training_day = sorted((row for row in records
                               if row.date == "20210825" and row.label == label),
                              key=lambda row: row.trial)
        test_day = [row for row in records
                    if row.date == "20210826" and row.label == label]
        if len(training_day) != 30 or len(test_day) != 30:
            raise ValueError(f"incomplete source trials for class {CLASSES[label]}")
        validation_ids = set(random.Random(seed + label).sample(
            [row.trial for row in training_day], 6))
        splits["validation"].extend(row for row in training_day
                                    if row.trial in validation_ids)
        splits["train"].extend(row for row in training_day
                                if row.trial not in validation_ids)
        splits["test"].extend(test_day)

    groups = {name: {(row.date, row.motion, row.label, row.trial) for row in rows}
              for name, rows in splits.items()}
    if (groups["train"] & groups["validation"] or groups["train"] & groups["test"] or
            groups["validation"] & groups["test"]):
        raise AssertionError("a source trial crosses splits")
    if any(len(splits[name]) != expected for name, expected in
           (("train", 72), ("validation", 18), ("test", 90))):
        raise ValueError("unexpected EPFL split size")
    if {row.date for row in splits["test"]} != {"20210826"}:
        raise AssertionError("test set must hold out the complete collection date")
    return splits


def verify_archive(path: Path) -> tuple[str, str]:
    if path.stat().st_size != DATASET_SIZE:
        raise ValueError(f"EPFL archive size mismatch: expected {DATASET_SIZE} bytes")
    md5 = hashlib.md5()
    sha256 = hashlib.sha256()
    with path.open("rb") as archive:
        for block in iter(lambda: archive.read(1024 * 1024), b""):
            md5.update(block)
            sha256.update(block)
    if md5.hexdigest() != DATASET_MD5:
        raise ValueError("EPFL archive MD5 does not match the Zenodo record")
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
            raise ValueError("downloaded EPFL archive does not match Zenodo size and MD5")
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)


def _audio_features(audio_bytes: bytes, sample_rate: int, channels: int) -> torch.Tensor:
    if sample_rate != SAMPLE_RATE or channels != 1 or not audio_bytes:
        raise ValueError("unexpected or missing EPFL audio metadata/data")
    waveform, decoded_rate = sf.read(BytesIO(audio_bytes), dtype="float32", always_2d=True)
    if decoded_rate != sample_rate or waveform.shape[1] != channels or waveform.shape[0] < sample_rate:
        raise ValueError("decoded EPFL audio is incomplete or has an unexpected format")
    mono = waveform[:, 0]
    target_length = SAMPLE_RATE * AUDIO_SECONDS
    if mono.size > target_length:
        start = (mono.size - target_length) // 2
        mono = mono[start:start + target_length]
    elif mono.size < target_length:
        mono = np.pad(mono, (0, target_length - mono.size))
    signal = torch.from_numpy(np.asarray(mono, dtype=np.float32).copy())
    signal = signal - signal.mean()
    rms = signal.square().mean().sqrt()
    if not torch.isfinite(rms) or rms < 1e-6:
        raise ValueError("EPFL audio is silent or nonfinite")
    signal = signal / rms
    spectrum = torch.stft(signal, n_fft=N_FFT, hop_length=HOP_LENGTH,
                          window=WINDOW, return_complex=True).abs()
    spectrum = torch.log1p(spectrum)
    features = F.adaptive_avg_pool2d(spectrum[None, None], AUDIO_FEATURE_SHAPE).squeeze(0)
    return (features - features.mean()) / features.std(unbiased=False).clamp_min(1e-5)


def _tactile_features(frames: list[np.ndarray]) -> torch.Tensor:
    if not frames:
        raise ValueError("EPFL tactile stream is missing")
    data = np.stack(frames)
    if data.shape[1] != TACTILE_SHAPE[0] * TACTILE_SHAPE[1]:
        raise ValueError(f"unexpected EPFL tactile frame length: {data.shape[1]}")
    data = data.reshape(-1, *TACTILE_SHAPE)
    if (not np.isfinite(data).all() or data.min() < 0 or data.max() > 255 or
            not np.count_nonzero(data)):
        raise ValueError("EPFL tactile stream is invalid or contains no measured pressure")
    summary = np.stack((data.mean(axis=0), np.quantile(data, 0.95, axis=0)))
    return torch.from_numpy((np.log1p(summary) / np.log1p(255)).astype(np.float32))


def extract_trial(archive: zipfile.ZipFile, record: Record, bag_path: Path) -> tuple[torch.Tensor, torch.Tensor]:
    with archive.open(record.path) as source, bag_path.open("wb") as target:
        shutil.copyfileobj(source, target)
    audio_chunks: list[bytes] = []
    tactile_frames: list[np.ndarray] = []
    audio_info = None
    with AnyReader([bag_path]) as reader:
        required = {AUDIO_TOPIC, AUDIO_INFO_TOPIC, TACTILE_TOPIC}
        connections = [connection for connection in reader.connections
                       if connection.topic in required]
        if {connection.topic for connection in connections} != required:
            raise ValueError(f"required EPFL streams are missing in {record.path}")
        for connection, _, raw in reader.messages(connections=connections):
            message = reader.deserialize(raw, connection.msgtype)
            if connection.topic == AUDIO_TOPIC:
                audio_chunks.append(message.data.tobytes())
            elif connection.topic == AUDIO_INFO_TOPIC:
                if audio_info is not None:
                    raise ValueError(f"multiple audio-info records in {record.path}")
                audio_info = message
            elif connection.topic == TACTILE_TOPIC:
                tactile_frames.append(np.asarray(message.data, dtype=np.float32).copy())
    if audio_info is None or audio_info.coding_format.lower() != "mp3" or audio_info.sample_format != "S16LE":
        raise ValueError(f"unexpected EPFL audio encoding in {record.path}")
    audio = _audio_features(b"".join(audio_chunks), audio_info.sample_rate,
                            audio_info.channels)
    tactile = _tactile_features(tactile_frames)
    return audio, tactile


class SourceContentClassifier(nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.audio = nn.Sequential(
            nn.Conv2d(1, 8, 3, padding=1), nn.GELU(), nn.MaxPool2d(2),
            nn.Conv2d(8, 16, 3, padding=1), nn.GELU(),
            nn.AdaptiveAvgPool2d(1), nn.Flatten(),
        )
        self.tactile = nn.Sequential(
            nn.Conv2d(2, 8, 3, padding=1), nn.GELU(), nn.MaxPool2d(2),
            nn.Conv2d(8, 16, 3, padding=1), nn.GELU(),
            nn.AdaptiveAvgPool2d(1), nn.Flatten(),
        )
        self.head = nn.Sequential(nn.Linear(32, 24), nn.GELU(), nn.Dropout(0.2),
                                  nn.Linear(24, len(CLASSES)))

    def forward(self, audio: torch.Tensor, tactile: torch.Tensor) -> torch.Tensor:
        return self.head(torch.cat((self.audio(audio), self.tactile(tactile)), dim=1))


def classification_metrics(truth: list[int], predicted: list[int]) -> dict:
    confusion = [[0] * len(CLASSES) for _ in CLASSES]
    for actual, guess in zip(truth, predicted, strict=True):
        confusion[actual][guess] += 1
    per_class_f1 = []
    supports = []
    for label in range(len(CLASSES)):
        true_positive = confusion[label][label]
        false_positive = sum(confusion[row][label] for row in range(len(CLASSES))) - true_positive
        false_negative = sum(confusion[label]) - true_positive
        supports.append(sum(confusion[label]))
        per_class_f1.append(2 * true_positive / max(1, 2 * true_positive + false_positive + false_negative))
    total = sum(supports)
    return {"accuracy": sum(confusion[i][i] for i in range(len(CLASSES))) / max(1, total),
            "macro_f1": sum(per_class_f1) / len(CLASSES),
            "support_by_class": dict(zip(CLASSES, supports, strict=True)),
            "confusion_matrix": confusion}


def evaluate(model: nn.Module, audio: torch.Tensor, tactile: torch.Tensor,
             labels: torch.Tensor, indices: list[int]) -> dict:
    model.eval()
    with torch.inference_mode():
        predicted = model(audio[indices], tactile[indices]).argmax(1).tolist()
    return classification_metrics(labels[indices].tolist(), predicted)


def main() -> None:
    root = Path(__file__).resolve().parent / "artifacts"
    parser = argparse.ArgumentParser(
        description="Train a separate EPFL robot-probe content classifier; not a Capybara model")
    parser.add_argument("--archive", type=Path, default=root / "epfl-mml" / "annotated_bags_mml.zip")
    parser.add_argument("--out", type=Path, default=root / "epfl-mml-source-classifier")
    parser.add_argument("--epochs", type=int, default=60)
    parser.add_argument("--seed", type=int, default=20261010)
    args = parser.parse_args()
    if args.epochs < 1:
        parser.error("--epochs must be positive")

    random.seed(args.seed)
    np.random.seed(args.seed)
    torch.manual_seed(args.seed)
    torch.set_num_threads(min(8, os.cpu_count() or 1))
    started = time.monotonic()
    if not args.archive.exists():
        download_archive(args.archive)
    archive_md5, archive_sha256 = verify_archive(args.archive)
    with zipfile.ZipFile(args.archive) as archive:
        records = read_records(archive)
        splits = split_records(records, args.seed)
        all_records = [record for name in ("train", "validation", "test")
                       for record in splits[name]]
        audio_features = []
        tactile_features = []
        args.out.parent.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=args.out.parent) as temporary:
            bag_path = Path(temporary) / "trial.bag"
            for index, record in enumerate(all_records, 1):
                audio, tactile = extract_trial(archive, record, bag_path)
                audio_features.append(audio)
                tactile_features.append(tactile)
                bag_path.unlink()
                if index % 30 == 0:
                    print(json.dumps({"extracted": index, "total": len(all_records)}))

    audio_tensor = torch.stack(audio_features)
    tactile_tensor = torch.stack(tactile_features)
    labels = torch.tensor([record.label for record in all_records], dtype=torch.long)
    record_indices = {record.path: index for index, record in enumerate(all_records)}
    split_indices = {name: [record_indices[row.path] for row in rows]
                     for name, rows in splits.items()}
    model = SourceContentClassifier()
    train_labels = labels[split_indices["train"]]
    counts = torch.bincount(train_labels, minlength=len(CLASSES)).float()
    criterion = nn.CrossEntropyLoss(weight=len(train_labels) / (len(CLASSES) * counts))
    loader = DataLoader(TensorDataset(audio_tensor[split_indices["train"]],
                                      tactile_tensor[split_indices["train"]], train_labels),
                        batch_size=16, shuffle=True,
                        generator=torch.Generator().manual_seed(args.seed))
    optimizer = torch.optim.AdamW(model.parameters(), lr=1e-3, weight_decay=1e-3)
    best_state = None
    best_f1 = -1.0
    best_epoch = 0
    stale_epochs = 0
    for epoch in range(1, args.epochs + 1):
        model.train()
        epoch_loss = 0.0
        epoch_samples = 0
        for batch_audio, batch_tactile, batch_labels in loader:
            optimizer.zero_grad(set_to_none=True)
            loss = criterion(model(batch_audio, batch_tactile), batch_labels)
            loss.backward()
            optimizer.step()
            epoch_loss += loss.detach().item() * len(batch_labels)
            epoch_samples += len(batch_labels)
        train_metrics = evaluate(model, audio_tensor, tactile_tensor, labels,
                                 split_indices["train"])
        validation = evaluate(model, audio_tensor, tactile_tensor, labels,
                              split_indices["validation"])
        print(json.dumps({"epoch": epoch, "train_loss": epoch_loss / epoch_samples,
                          "train_accuracy": train_metrics["accuracy"],
                          "validation_accuracy": validation["accuracy"],
                          "validation_macro_f1": validation["macro_f1"]}))
        if validation["macro_f1"] > best_f1:
            best_state = copy.deepcopy(model.state_dict())
            best_f1 = validation["macro_f1"]
            best_epoch = epoch
            stale_epochs = 0
        else:
            stale_epochs += 1
            if stale_epochs >= 8:
                break
    if best_state is None:
        raise RuntimeError("training produced no selectable checkpoint")
    model.load_state_dict(best_state)
    validation_metrics = evaluate(model, audio_tensor, tactile_tensor, labels,
                                  split_indices["validation"])
    test_metrics = evaluate(model, audio_tensor, tactile_tensor, labels,
                            split_indices["test"])

    args.out.mkdir(parents=True, exist_ok=True)
    model_path = args.out / "epfl-mml-content-classifier.pt"
    torch.save({"state_dict": model.state_dict(), "class_names": CLASSES,
                "sample_rate": SAMPLE_RATE, "audio_seconds": AUDIO_SECONDS,
                "n_fft": N_FFT, "hop_length": HOP_LENGTH,
                "audio_feature_shape": AUDIO_FEATURE_SHAPE,
                "tactile_shape": TACTILE_SHAPE}, model_path)
    model_sha256 = hashlib.sha256(model_path.read_bytes()).hexdigest()
    report = {
        "purpose": "source-domain container-content classification research only",
        "dataset": {"name": "EPFL Multimodal Sensory Learning for Object Manipulation",
                    "doi": DATASET_DOI, "url": "https://doi.org/" + DATASET_DOI,
                    "license": "CC BY 4.0", "license_source": "https://zenodo.org/api/records/6372438",
                    "archive_md5": archive_md5, "archive_sha256": archive_sha256,
                    "selected_trials": len(all_records), "classes": list(CLASSES),
                    "container_count": 1, "audio_rate_hz": SAMPLE_RATE,
                    "audio_sensor": "robot-probe microphone; MP3 decoded to mono",
                    "tactile_sensor": "robot-mounted Tekscan pressure array"},
        "split": {"seed": args.seed, "group": "individual recorded trial",
                  "counts": {name: len(rows) for name, rows in splits.items()},
                  "train_validation_collection_date": "20210825",
                  "test_held_out_collection_date": "20210826",
                  "test_motion": "rotation", "train_validation_motion": "vertical",
                  "date_and_motion_are_confounded": True,
                  "container_or_object_held_out": False},
        "model": {"architecture": "small two-branch CNN over log-magnitude audio and measured tactile pressure summaries",
                  "sha256": model_sha256, "best_epoch": best_epoch,
                  "device": "cpu", "epochs_requested": args.epochs,
                  "capybara_integrity_evidence": False,
                  "android_deployment_candidate": False},
        "uniform_chance_accuracy": 1 / len(CLASSES),
        "validation": validation_metrics,
        "train": evaluate(model, audio_tensor, tactile_tensor, labels,
                          split_indices["train"]),
        "test": test_metrics,
        "training_seconds": round(time.monotonic() - started, 3),
        "limitations": ["one container only", "content label, not integrity state",
                        "robot microphone and contact-pressure sensor are not phone speaker or vibrator feedback",
                        "host MP3 decoder emitted frame-length errors; decoded audio continuity is unverified",
                        "held-out date also changes motion; transfer to phones is untested"],
    }
    (args.out / "training-report.json").write_text(
        json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"test_accuracy": test_metrics["accuracy"],
                      "test_macro_f1": test_metrics["macro_f1"],
                      "model_sha256": model_sha256, "output": str(args.out)}))


if __name__ == "__main__":
    main()
