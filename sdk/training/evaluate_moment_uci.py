from __future__ import annotations

import argparse
import hashlib
import json
import random
import time
from importlib.metadata import version
from pathlib import Path

import torch
from torch import nn
from torch.utils.data import DataLoader, TensorDataset
from momentfm import MOMENTPipeline

from train_uci_vibration_baseline import LABELS, TEST_SPEEDS, VAL_SPEEDS, WINDOW_SIZE, load_dataset, score


def split_masks(speeds: torch.Tensor) -> tuple[torch.Tensor, torch.Tensor, torch.Tensor]:
    excluded = VAL_SPEEDS | TEST_SPEEDS
    train = torch.tensor([int(speed) not in excluded for speed in speeds])
    val = torch.tensor([int(speed) in VAL_SPEEDS for speed in speeds])
    test = torch.tensor([int(speed) in TEST_SPEEDS for speed in speeds])
    if not torch.all(train.to(torch.int8) + val.to(torch.int8) + test.to(torch.int8) == 1):
        raise ValueError("Every window must belong to exactly one speed split")
    return train, val, test


def pad_windows(signals: torch.Tensor, seq_len: int) -> tuple[torch.Tensor, torch.Tensor]:
    if signals.ndim != 3 or signals.shape[2] != WINDOW_SIZE or seq_len < WINDOW_SIZE:
        raise ValueError("Expected [windows, 3, 64] signals and a context of at least 64")
    padded = signals.new_zeros((len(signals), signals.shape[1], seq_len))
    padded[:, :, :WINDOW_SIZE] = signals
    mask = signals.new_zeros((len(signals), seq_len))
    mask[:, :WINDOW_SIZE] = 1
    return padded, mask


def extract_embeddings(model: MOMENTPipeline, signals: torch.Tensor, batch_size: int) -> tuple[torch.Tensor, float]:
    embeddings = []
    started = time.perf_counter()
    with torch.inference_mode():
        for start in range(0, len(signals), batch_size):
            x, mask = pad_windows(signals[start : start + batch_size], model.seq_len)
            output = model(x_enc=x, input_mask=mask).embeddings
            if output.ndim != 2 or not torch.isfinite(output).all():
                raise RuntimeError("MOMENT returned invalid embedding features")
            embeddings.append(output.cpu())
            completed = min(start + batch_size, len(signals))
            if completed % (batch_size * 10) == 0 or completed == len(signals):
                print(f"Embedded {completed}/{len(signals)} windows", flush=True)
    return torch.cat(embeddings), time.perf_counter() - started


def fit_probe(train_x: torch.Tensor, train_y: torch.Tensor, val_x: torch.Tensor, val_y: torch.Tensor,
              epochs: int, patience: int, seed: int) -> tuple[nn.Module, int, torch.Tensor, torch.Tensor]:
    mean = train_x.mean(0, keepdim=True)
    std = train_x.std(0, unbiased=False, keepdim=True).clamp_min(1e-6)
    train_x, val_x = (train_x - mean) / std, (val_x - mean) / std
    generator = torch.Generator().manual_seed(seed)
    loader = DataLoader(TensorDataset(train_x, train_y), batch_size=128, shuffle=True,
                        generator=generator, num_workers=0)
    probe = nn.Linear(train_x.shape[1], len(LABELS))
    optimizer = torch.optim.Adam(probe.parameters(), lr=1e-3)
    best_state, best_key, best_epoch, stale = None, (-1.0, float("inf")), 0, 0
    for epoch in range(1, epochs + 1):
        probe.train()
        for x, y in loader:
            optimizer.zero_grad(set_to_none=True)
            loss = nn.functional.cross_entropy(probe(x), y)
            loss.backward()
            optimizer.step()
        validation = score(probe, val_x, val_y)
        key = (validation["accuracy"], -validation["crossEntropy"])
        if key > best_key:
            best_state = {name: value.detach().clone() for name, value in probe.state_dict().items()}
            best_key, best_epoch, stale = key, epoch, 0
        else:
            stale += 1
            if stale >= patience:
                break
    if best_state is None:
        raise RuntimeError("Probe training produced no checkpoint")
    probe.load_state_dict(best_state)
    probe.eval()
    return probe, best_epoch, mean, std


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def run(args: argparse.Namespace) -> dict:
    if args.batch_size < 1 or args.epochs < 1 or args.patience < 1:
        raise ValueError("Batch size, epochs, and patience must be positive")
    random.seed(args.seed)
    torch.manual_seed(args.seed)
    torch.use_deterministic_algorithms(True)
    torch.set_num_threads(1)
    if torch.cuda.is_available():
        raise RuntimeError("This comparison must run on CPU")

    signals, labels, speeds = load_dataset(args.dataset)
    train_mask, val_mask, test_mask = split_masks(speeds)
    model = MOMENTPipeline.from_pretrained(str(args.model_dir), local_files_only=True,
                                           model_kwargs={"task_name": "embedding"})
    model.init()
    model.eval()
    model.requires_grad_(False)
    embeddings, extraction_seconds = extract_embeddings(model, signals, args.batch_size)
    train_x, val_x, test_x = embeddings[train_mask], embeddings[val_mask], embeddings[test_mask]
    train_y, val_y, test_y = labels[train_mask], labels[val_mask], labels[test_mask]

    train_started = time.perf_counter()
    probe, best_epoch, mean, std = fit_probe(train_x, train_y, val_x, val_y,
                                             args.epochs, args.patience, args.seed)
    training_seconds = time.perf_counter() - train_started

    # score() takes a module over its input; this adapter keeps the report identical to the baseline.
    normalized_test_x = (test_x - mean) / std
    test = score(probe, normalized_test_x, test_y)
    model_file = args.model_dir / "model.safetensors"
    report = {
        "purpose": "frozen pretrained MOMENT embedding linear probe on UCI fan vibration; not a Capybara integrity model",
        "source": {"dataset": "UCI Accelerometer 846", "doi": "10.24432/C5Q61V",
                   "license": "CC BY 4.0", "csvSha256": sha256(args.dataset)},
        "pretrained": {"repo": "AutonLab/MOMENT-1-small", "revision": "411e288267f82cce86296dbe4d6c8bc533cc162f",
                       "license": "MIT", "checkpointSha256": sha256(model_file),
                       "parameters": sum(parameter.numel() for parameter in model.parameters()),
                       "torchVersion": torch.__version__, "momentfmVersion": version("momentfm"), "device": "cpu"},
        "task": "three-way cooler-fan blade-weight configuration classification",
        "input": {"channels": ["x", "y", "z"], "sampleRateHz": 50, "windowSamples": WINDOW_SIZE,
                  "contextSamples": model.seq_len, "padding": "zero padding masked as invalid; no resampling"},
        "split": {"unit": "entire fan-speed setting; non-overlapping windows",
                  "trainSpeeds": sorted(set(int(speed) for speed in speeds[train_mask])),
                  "validationSpeeds": sorted(VAL_SPEEDS), "testSpeeds": sorted(TEST_SPEEDS)},
        "probe": {"type": "frozen MOMENT embedding plus linear classifier", "maxEpochs": args.epochs,
                  "bestEpoch": best_epoch, "patience": args.patience, "optimizer": "Adam",
                  "learningRate": 0.001, "batchSize": 128, "seed": args.seed,
                  "normalizationFitOn": "training embeddings only", "extractionSeconds": extraction_seconds,
                  "trainingSeconds": training_seconds},
        "heldOutTest": test,
        "chanceAccuracy": 1 / len(LABELS),
    }
    if args.baseline_report and args.baseline_report.exists():
        baseline = json.loads(args.baseline_report.read_text(encoding="utf-8"))
        report["scratchBaselineHeldOutTest"] = baseline["heldOutTest"]
    args.out.mkdir(parents=True, exist_ok=True)
    (args.out / "training-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    torch.save({"state_dict": probe.state_dict(), "embedding_mean": mean, "embedding_std": std,
                "windowSize": WINDOW_SIZE,
                "contextSamples": model.seq_len, "seed": args.seed}, args.out / "moment-uci-linear-probe.pt")
    print(json.dumps(report, indent=2))
    return report


def main() -> None:
    parser = argparse.ArgumentParser(description="Evaluate a frozen licensed MOMENT encoder on UCI fan vibration.")
    parser.add_argument("--dataset", type=Path, default=Path("sdk/training/artifacts/uci-accelerometer.csv"))
    parser.add_argument("--model-dir", type=Path, default=Path("sdk/training/artifacts/moment-1-small"))
    parser.add_argument("--baseline-report", type=Path,
                        default=Path("sdk/training/artifacts/uci-fan-baseline/training-report.json"))
    parser.add_argument("--out", type=Path, default=Path("sdk/training/artifacts/moment-uci-linear-probe"))
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--epochs", type=int, default=100)
    parser.add_argument("--patience", type=int, default=12)
    parser.add_argument("--seed", type=int, default=20261009)
    run(parser.parse_args())


if __name__ == "__main__":
    main()
