from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import random
from collections import defaultdict
from pathlib import Path

import torch
from torch import nn
from torch.utils.data import DataLoader, TensorDataset

WINDOW_SIZE = 64
SAMPLE_RATE_HZ = 50
VAL_SPEEDS = {30, 60, 90}
TEST_SPEEDS = {40, 70, 100}
LABELS = {
    1: "normal: weights on neighboring blades",
    2: "perpendicular: weights 90 degrees apart",
    3: "opposite: weights on opposite blades",
}


class FanConfigurationClassifier(nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.features = nn.Sequential(
            nn.Conv1d(3, 16, kernel_size=9, stride=2, padding=4),
            nn.GELU(),
            nn.Conv1d(16, 32, kernel_size=9, stride=2, padding=4),
            nn.GELU(),
            nn.AdaptiveAvgPool1d(1),
            nn.Flatten(),
        )
        self.classifier = nn.Linear(32, len(LABELS))

    def forward(self, signal: torch.Tensor) -> torch.Tensor:
        return self.classifier(self.features(signal))


def load_dataset(path: Path) -> tuple[torch.Tensor, torch.Tensor, torch.Tensor]:
    groups: dict[tuple[int, int], list[list[float]]] = defaultdict(list)
    with path.open(newline="", encoding="utf-8-sig") as source:
        reader = csv.DictReader(source)
        if reader.fieldnames != ["wconfid", "pctid", "x", "y", "z"]:
            raise ValueError(f"Unexpected UCI CSV columns: {reader.fieldnames}")
        for line, row in enumerate(reader, 2):
            label, speed = int(row["wconfid"]), int(row["pctid"])
            values = [float(row[axis]) for axis in ("x", "y", "z")]
            if label not in LABELS or not all(math.isfinite(value) for value in values):
                raise ValueError(f"Invalid sensor row {line}")
            groups[label, speed].append(values)

    speeds = set(range(20, 101, 5))
    expected_groups = {(label, speed) for label in LABELS for speed in speeds}
    if set(groups) != expected_groups:
        raise ValueError("Dataset must contain all 17 speeds and all 3 configurations")
    windows: list[list[list[float]]] = []
    labels: list[int] = []
    window_speeds: list[int] = []
    for (label, speed), rows in sorted(groups.items()):
        if len(rows) != 3000:
            raise ValueError(f"Expected 3000 rows for configuration={label}, speed={speed}; got {len(rows)}")
        for start in range(0, len(rows) - WINDOW_SIZE + 1, WINDOW_SIZE):
            windows.append(rows[start : start + WINDOW_SIZE])
            labels.append(label - 1)
            window_speeds.append(speed)

    signals = torch.tensor(windows, dtype=torch.float32).permute(0, 2, 1)
    return signals, torch.tensor(labels), torch.tensor(window_speeds)


def score(model: nn.Module, signals: torch.Tensor, labels: torch.Tensor) -> dict:
    model.eval()
    with torch.inference_mode():
        logits = model(signals)
        loss = nn.functional.cross_entropy(logits, labels).item()
        predicted = logits.argmax(dim=1)
    confusion = [[0] * len(LABELS) for _ in LABELS]
    for expected, actual in zip(labels.tolist(), predicted.tolist()):
        confusion[expected][actual] += 1
    total = len(labels)
    accuracy = sum(confusion[i][i] for i in range(len(LABELS))) / total
    f1 = []
    for i in range(len(LABELS)):
        tp = confusion[i][i]
        fp = sum(confusion[row][i] for row in range(len(LABELS)) if row != i)
        fn = sum(confusion[i][col] for col in range(len(LABELS)) if col != i)
        f1.append(2 * tp / (2 * tp + fp + fn) if 2 * tp + fp + fn else 0.0)
    return {"windows": total, "accuracy": accuracy, "macroF1": sum(f1) / len(f1), "crossEntropy": loss, "confusionMatrix": confusion}


def train(dataset: Path, out: Path, epochs: int, patience: int, seed: int) -> None:
    random.seed(seed)
    torch.manual_seed(seed)
    torch.use_deterministic_algorithms(True)
    torch.set_num_threads(1)

    signals, labels, speeds = load_dataset(dataset)
    excluded_speeds = VAL_SPEEDS | TEST_SPEEDS
    train_mask = torch.tensor([int(speed) not in excluded_speeds for speed in speeds])
    val_mask = torch.tensor([int(speed) in VAL_SPEEDS for speed in speeds])
    test_mask = torch.tensor([int(speed) in TEST_SPEEDS for speed in speeds])
    train_x, train_y = signals[train_mask], labels[train_mask]
    val_x, val_y = signals[val_mask], labels[val_mask]
    test_x, test_y = signals[test_mask], labels[test_mask]

    mean = train_x.mean(dim=(0, 2), keepdim=True)
    std = train_x.std(dim=(0, 2), unbiased=False, keepdim=True).clamp_min(1e-6)
    train_x, val_x, test_x = ((x - mean) / std for x in (train_x, val_x, test_x))
    loader = DataLoader(
        TensorDataset(train_x, train_y), batch_size=128, shuffle=True,
        generator=torch.Generator().manual_seed(seed), num_workers=0,
    )

    model = FanConfigurationClassifier()
    optimizer = torch.optim.Adam(model.parameters(), lr=1e-3)
    best_state = None
    best_key = (-1.0, float("inf"))
    best_epoch = 0
    stale_epochs = 0
    for epoch in range(1, epochs + 1):
        model.train()
        for batch_x, batch_y in loader:
            optimizer.zero_grad(set_to_none=True)
            loss = nn.functional.cross_entropy(model(batch_x), batch_y)
            loss.backward()
            optimizer.step()
        validation = score(model, val_x, val_y)
        key = (validation["accuracy"], -validation["crossEntropy"])
        if key > best_key:
            best_key = key
            best_state = {name: value.detach().clone() for name, value in model.state_dict().items()}
            best_epoch = epoch
            stale_epochs = 0
        else:
            stale_epochs += 1
            if stale_epochs >= patience:
                break
    if best_state is None:
        raise RuntimeError("Training produced no model checkpoint")

    model.load_state_dict(best_state)
    result = score(model, test_x, test_y)
    out.mkdir(parents=True, exist_ok=True)
    model_path = out / "uci-fan-configuration-baseline.pt"
    torch.save({
        "state_dict": best_state,
        "normalization_mean": mean,
        "normalization_std": std,
        "windowSize": WINDOW_SIZE,
        "sampleRateHz": SAMPLE_RATE_HZ,
        "labels": LABELS,
        "architecture": "Conv1d(3,16,9,stride=2)-GELU-Conv1d(16,32,9,stride=2)-GELU-global-average-pool-Linear(32,3)",
    }, model_path)
    digest = hashlib.sha256(dataset.read_bytes()).hexdigest()
    report = {
        "purpose": "UCI fan-vibration classifier baseline; not a Capybara integrity model",
        "source": {
            "title": "Accelerometer",
            "doi": "10.24432/C5Q61V",
            "url": "https://archive.ics.uci.edu/dataset/846/accelerometer",
            "license": "CC BY 4.0",
            "citation": "Scalabrini Sampaio et al., UCI Machine Learning Repository, https://doi.org/10.24432/C5Q61V",
            "csvSha256": digest,
        },
        "task": "three-way classification of cooler-fan blade weight configuration",
        "input": {"channels": ["x", "y", "z"], "sampleRateHz": SAMPLE_RATE_HZ, "windowSamples": WINDOW_SIZE, "windowSeconds": WINDOW_SIZE / SAMPLE_RATE_HZ},
        "labels": LABELS,
        "split": {"unit": "entire fan-speed setting; non-overlapping windows", "trainSpeeds": sorted(set(range(20, 101, 5)) - VAL_SPEEDS - TEST_SPEEDS), "validationSpeeds": sorted(VAL_SPEEDS), "testSpeeds": sorted(TEST_SPEEDS)},
        "training": {"seed": seed, "maxEpochs": epochs, "bestEpoch": best_epoch, "patience": patience, "optimizer": "Adam", "learningRate": 0.001, "batchSize": 128, "torchVersion": torch.__version__, "device": "cpu"},
        "normalization": {"mean": mean.flatten().tolist(), "std": std.flatten().tolist(), "fitOn": "training split only"},
        "validation": score(model, val_x, val_y),
        "heldOutTest": result,
        "chanceAccuracy": 1 / len(LABELS),
        "modelSha256": hashlib.sha256(model_path.read_bytes()).hexdigest(),
    }
    (out / "training-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


def main() -> None:
    parser = argparse.ArgumentParser(description="Train a CPU-only UCI fan-configuration baseline.")
    parser.add_argument("--dataset", type=Path, required=True, help="Extracted UCI accelerometer CSV")
    parser.add_argument("--out", type=Path, required=True, help="Ignored output directory for weights and report")
    parser.add_argument("--epochs", type=int, default=60)
    parser.add_argument("--patience", type=int, default=10)
    parser.add_argument("--seed", type=int, default=20261009)
    args = parser.parse_args()
    if args.epochs < 1 or args.patience < 1:
        parser.error("--epochs and --patience must be positive")
    train(args.dataset, args.out, args.epochs, args.patience, args.seed)


if __name__ == "__main__":
    main()
