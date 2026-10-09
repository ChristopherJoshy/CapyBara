"""Train the actual pinned Laya model, or export it with held-out Android-CPU parity checks."""
import argparse
import hashlib
import importlib.metadata
import importlib.util
import json
from dataclasses import asdict
from pathlib import Path

from prepare import LABELS, digest, load_prepared, read_json

LAYA_COMMIT = "1adc59f7e371deb601fcfa18a14e25db238addcc"
BASE_REVISION = "7b928d828b7b0e022f929d9bd2e44165aa270148"
BASE_WEIGHTS_SHA256 = "891102d372688fc2a094dac56a384bc537b87c63f21f9f3dac0be2b7cbc8d86c"
EXPORT_SCRIPT_SHA256 = "3dc6eac8971358ad5531c7124f2e631ddd4da6890a8df8a6237051e56b4594e2"
SPECIALISTS = {"EchoNet", "VibraNet", "PhysFusion", "StateNet", "SpatialNet"}


def validate_training(parts):
    identities = set()
    for split, rows in parts.items():
        if set(row["expected"]["integrity"] for row in rows) != set(LABELS):
            raise ValueError(split + " must represent all three physical decision labels")
        for row in rows:
            prov = row["provenance"]
            models = prov["modelVersions"]
            if not SPECIALISTS.issubset(models) or not prov.get("calibrationVersion"):
                raise ValueError("Final Laya training requires all five trained specialists and physical calibration; primitive physics is insufficient")
            if not isinstance(prov["calibrationVersion"], str):
                raise ValueError("calibrationVersion must identify the actual calibration")
            for name in SPECIALISTS:
                value = models[name]
                if not isinstance(value, str) or len(value) != 64 or any(c not in "0123456789abcdef" for c in value):
                    raise ValueError(name + " must identify its actual model SHA256")
            identities.add((tuple(sorted(models.items())), prov["calibrationVersion"]))
    if len(identities) != 1:
        raise ValueError("Mixed specialist or calibration versions are incompatible training evidence")
    if len(parts["calibration"]) < 10:
        raise ValueError("Laya upstream needs at least 10 independent calibration items to fit its type temperature")


def checked_upstream():
    dist = importlib.metadata.distribution("laya")
    source = json.loads(dist.read_text("direct_url.json") or "{}")
    if source.get("vcs_info", {}).get("commit_id") != LAYA_COMMIT:
        raise ValueError("Install sdk/training/requirements.txt to use the exact verified Laya source")


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, allow_nan=False), encoding="utf-8")


def train(args, manifest, parts):
    if digest(args.base / "model.safetensors") != BASE_WEIGHTS_SHA256:
        raise ValueError("Base weights differ from the verified official English Laya checkpoint")
    from laya.train import (TrainConfig, load_checkpoint, items_from_rows, train_model,
                            calibration_records, calibration_report, save_checkpoint,
                            evaluate_records, update_budget_message, prior_collapse_message)
    from laya.calibrate import fit_temperature_map, fit_abstention_thresholds
    import torch

    model, tok, cfg = load_checkpoint(str(args.base))
    if cfg.get("option_layout", "sequential") != "sequential":
        raise ValueError("Android ONNX requires sequential option layout")
    device = torch.device(args.device)
    if device.type == "cuda" and not torch.cuda.is_available():
        raise ValueError("Requested CUDA training but no GPU is allocated")
    model.to(device)
    config = TrainConfig(epochs=args.epochs, seed=20261009, option_layout="sequential",
                         shuffle_options=("choice",), calib_frac=0.0)
    config.validate()
    items = {}
    for split, rows in parts.items():
        items[split], skipped = items_from_rows(tok, rows, cfg["max_len"], cfg["head_max_len"])
        if skipped or len(items[split]) != len(rows):
            raise ValueError("Tokenizer refused " + split + ": " + str(skipped))
    print(update_budget_message(len(items["train"]), config), flush=True)
    before = calibration_records(model, tok, items["test"], device, cfg["max_len"], cfg["head_max_len"], batch_size=1)
    baseline = evaluate_records(before, cfg.get("temperature"), cfg.get("temperature_by_options"))
    args.out.mkdir(parents=True)

    def checkpoint(epoch, loss):
        save_checkpoint(model, tok, cfg, str(args.out / "checkpoint_latest"))

    losses = train_model(model, tok, items["train"], config, device,
                         cfg["max_len"], cfg["head_max_len"], on_epoch_end=checkpoint)
    save_checkpoint(model, tok, cfg, str(args.out))
    del model
    if device.type == "cuda":
        torch.cuda.empty_cache()
    # Fit calibration on reloaded persisted weights, so fp16 checkpoint storage cannot silently change it.
    model, tok, _ = load_checkpoint(str(args.out))
    model.to(device)
    # Separate connected groups are essential: upstream finetune's item-random calibration split leaks physical sessions.
    records = calibration_records(model, tok, items["calibration"], device, cfg["max_len"], cfg["head_max_len"], batch_size=1)
    collapse = prior_collapse_message(records)
    if collapse:
        raise ValueError("Checkpoint withheld: " + collapse)
    fitted = fit_temperature_map(records)
    calibration = calibration_report(records, fitted["temperature"])
    thresholds = fit_abstention_thresholds(records, fitted["temperature"], fitted["temperature_by_options"])
    trained_cfg = dict(cfg, fine_tuned=True, option_layout="sequential", temperature=fitted["temperature"],
                       temperature_by_options=fitted["temperature_by_options"])
    trained_cfg["training"] = {"capybara": {"sourceCommit": LAYA_COMMIT, "baseRevision": BASE_REVISION,
        "dataset": manifest, "config": asdict(config), "calibration": calibration,
        "abstentionThresholds": thresholds}}
    save_checkpoint(model, tok, trained_cfg, str(args.out))
    test = calibration_records(model, tok, items["test"], device,
                               cfg["max_len"], cfg["head_max_len"], batch_size=1)
    after = evaluate_records(test, trained_cfg["temperature"], trained_cfg["temperature_by_options"])
    report = {"stage": "Laya model evaluation; full-system TrustGate metrics remain separate",
              "base": baseline, "fineTuned": after, "calibration": calibration,
              "epochLosses": losses, "weightsSha256": digest(args.out / "model.safetensors"),
              "dependencies": {d.metadata["Name"]: d.version for d in importlib.metadata.distributions()},
              "device": str(device), "deploymentValidated": False}
    write_json(args.out / "training-report.json", report)
    print(json.dumps(report, indent=2))


def export(args, manifest, parts):
    import numpy as np
    import onnxruntime as ort
    import torch
    from laya.train import load_checkpoint, items_from_rows, encode_item
    from laya.common import collate_items

    cfg = read_json(args.checkpoint / "rl_agent_config.json")
    training_report = read_json(args.checkpoint / "training-report.json")
    if digest(args.checkpoint / "model.safetensors") != training_report.get("weightsSha256"):
        raise ValueError("Fine-tuned weights no longer match their training report")
    recorded = cfg.get("training", {}).get("capybara", {})
    if recorded.get("dataset") != manifest or not cfg.get("fine_tuned"):
        raise ValueError("Checkpoint is not a fine-tune of this verified dataset")
    if cfg.get("option_layout", "sequential") != "sequential":
        raise ValueError("ONNX refuses parallel option-layout weights")
    exporter = Path(__file__).parent / "upstream" / "laya" / "scripts" / "export_onnx.py"
    source_digest = hashlib.sha256(exporter.read_bytes().replace(b"\r\n", b"\n")).hexdigest()
    if source_digest != EXPORT_SCRIPT_SHA256:
        raise ValueError("Clone the pinned upstream export source; its digest does not match")
    spec = importlib.util.spec_from_file_location("laya_upstream_export", exporter)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    args.out.mkdir(parents=True)
    graph = args.out / "laya.onnx"
    module.export_to_onnx(str(args.checkpoint), str(graph))
    session = ort.InferenceSession(str(graph), providers=["CPUExecutionProvider"])
    if session.get_providers() != ["CPUExecutionProvider"]:
        raise ValueError("Export validation must run exclusively on CPU")
    model, tok, _ = load_checkpoint(str(args.checkpoint))
    model.eval()
    items, skipped = items_from_rows(tok, parts["test"], cfg["max_len"], cfg["head_max_len"])
    if skipped or len(items) != len(parts["test"]):
        raise ValueError("Held-out export items were skipped")
    max_drift = 0.0
    with torch.no_grad():
        chunks = [items[:1]] + [items[start:start + 2] for start in range(0, len(items), 2)]
        for chunk in chunks:
            batch = collate_items([[encode_item(tok, item, cfg["max_len"], cfg["head_max_len"])
                                    for item in chunk]], tok.pad_token_id)
            inputs = {name: batch[name] for name in ("input_ids", "attention_mask", "marker_pos", "marker_mask", "qtype")}
            eager = model(*(inputs[name] for name in inputs))
            actual = session.run(["logits", "act_logits"], {name: tensor.numpy() for name, tensor in inputs.items()})
            for expected, result in zip(eager, actual):
                expected = expected.numpy()
                np.testing.assert_allclose(result, expected, atol=1e-4, rtol=1e-4)
                max_drift = max(max_drift, float(np.max(np.abs(result - expected))))
            if not np.array_equal(actual[0].argmax(axis=1), eager[0].numpy().argmax(axis=1)):
                raise ValueError("Export changed a held-out decision")
    artifacts = {str(path.relative_to(args.out)): digest(path) for path in args.out.rglob("*") if path.is_file()}
    report = {"heldOutCases": len(items), "maxAbsoluteLogitDrift": max_drift,
              "provider": session.get_providers(), "quantized": False,
              "onnxSha256": digest(graph), "weightsSha256": digest(args.checkpoint / "model.safetensors"),
              "artifactSha256": artifacts, "dataset": manifest, "androidValidated": False,
              "modelSizeBytes": sum(path.stat().st_size for path in args.out.rglob("*") if path.is_file())}
    write_json(args.out / "export-report.json", report)
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    training = commands.add_parser("train")
    training.add_argument("--base", required=True, type=Path)
    training.add_argument("--device", choices=("cpu", "cuda"), default="cuda")
    training.add_argument("--epochs", type=int, default=4)
    exporting = commands.add_parser("export")
    exporting.add_argument("--checkpoint", required=True, type=Path)
    for command in (training, exporting):
        command.add_argument("dataset", type=Path)
        command.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    try:
        manifest, parts = load_prepared(args.dataset)
        validate_training(parts)
        if args.out.exists():
            raise ValueError("Use a new output directory to preserve existing artifacts")
        checked_upstream()
        (train if args.command == "train" else export)(args, manifest, parts)
    except (ValueError, OSError, KeyError, TypeError, importlib.metadata.PackageNotFoundError) as exc:
        parser.exit(2, "Laya operation refused: " + str(exc) + "\n")
