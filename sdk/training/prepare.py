"""Validate genuine capture annotations and prepare object/session-disjoint Laya rows."""
import re
import hashlib
import json
import math
import random
from collections import Counter
from datetime import datetime
from pathlib import Path

LABELS = ("MATCH", "CHANGED", "INCONCLUSIVE")
QUESTIONS = {"integrity": {"type": "choice", "instructions":
    "Evaluate the physical evidence against the enrolled reference under its integrity profile.",
    "criteria": {"MATCH": "Consistent physical response under validated measurement conditions.",
                 "CHANGED": "Evidence supports a meaningful change in physical state.",
                 "INCONCLUSIVE": "Evidence or measurement quality is insufficient for a reliable decision."}}}
FORBIDDEN_TOKENS = {"label", "expected", "gold", "condition", "annotator", "annotation",
                    "decision", "result", "target", "truth", "answer"}
FORBIDDEN_IDENTIFIERS = ("objectid", "captureid", "referenceid", "sessionid", "runid")
FORBIDDEN = {"label", "expected", "gold", "condition", "objectid", "sessionid", "runid",
             "captureid", "referencecaptureid", "annotator", "annotation", "decision",
             "result", "target", "truth", "answer"}
FORBIDDEN_ID_SEQUENCES = (("object", "id"), ("object", "identifier"),
                          ("object", "name"), ("capture", "id"),
                          ("session", "id"), ("run", "id"),
                          ("reference", "id"), ("reference", "capture", "id"))


def forbidden_key(key):
    separated = re.sub(r"([A-Z]+)([A-Z][a-z])", r"\1_\2", key)
    separated = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", separated)
    tokens = re.findall(r"[a-z0-9]+", separated.lower())
    normalized = "".join(tokens)
    return (normalized in FORBIDDEN or any(identifier in normalized for identifier in FORBIDDEN_IDENTIFIERS)
            or any(token.startswith("label") or token in FORBIDDEN_TOKENS for token in tokens)
            or any(tuple(tokens[index:index + len(sequence)]) == sequence
                   for sequence in FORBIDDEN_ID_SEQUENCES
                   for index in range(len(tokens) - len(sequence) + 1)))




def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8-sig"),
                      parse_constant=lambda value: (_ for _ in ()).throw(ValueError("Nonfinite JSON: " + value)))


def required_text(value, name):
    if not isinstance(value, str) or not value.strip():
        raise ValueError(name + " must be a nonempty string")
    return value


def safe_path(root, value):
    path = (root / required_text(value, "artifact path")).resolve()
    if not path.is_relative_to(root.resolve()) or not path.is_file():
        raise ValueError("Artifact must be an existing file under the capture root: " + str(path))
    return path


def check_values(value):
    if isinstance(value, dict):
        for key, item in value.items():
            if forbidden_key(key):
                raise ValueError("Annotation/identity cannot be input evidence: " + key)
            check_values(item)
    elif isinstance(value, list):
        for item in value:
            check_values(item)
    elif isinstance(value, float) and not math.isfinite(value):
        raise ValueError("Evidence contains a nonfinite number")


def capture(root, relative):
    path = safe_path(root, relative)
    meta = read_json(path)
    if meta.get("schemaVersion") != 1:
        raise ValueError("Unsupported capture schema")
    label = meta.get("label", {})
    for name in ("objectId", "sessionId", "condition", "position", "profile"):
        required_text(label.get(name), "capture label." + name)
    if any("diagnostic" in str(value).lower() for value in label.values()):
        raise ValueError("Diagnostic captures are not labeled physical experiments")
    required_text(meta.get("captureId"), "captureId")
    for name in ("sampleRate", "sampleCount", "startedBootNanos", "endedBootNanos"):
        if type(meta.get(name)) is not int or meta[name] <= 0:
            raise ValueError(name + " must be a positive measured integer")
    if meta["endedBootNanos"] <= meta["startedBootNanos"]:
        raise ValueError("Invalid capture interval")
    pcm = safe_path(root, str(path.parent.relative_to(root) / "audio.pcm"))
    imu = safe_path(root, str(path.parent.relative_to(root) / "imu.csv"))
    if pcm.stat().st_size != 2 * meta["sampleCount"]:
        raise ValueError("PCM16 byte count does not match metadata")
    return meta, {str(p.relative_to(root)): digest(p) for p in (path, pcm, imu)}


def validate_row(row, root):
    if row.get("label") not in LABELS:
        raise ValueError("Label must be MATCH, CHANGED, or INCONCLUSIVE")
    required_text(row.get("runId"), "runId")
    required_text(row.get("annotator"), "annotator")
    stamp = datetime.fromisoformat(required_text(row.get("labelledAtUtc"), "labelledAtUtc").replace("Z", "+00:00"))
    if stamp.utcoffset() is None:
        raise ValueError("Label timestamp must include a timezone")
    current, hashes = capture(root, row.get("capture"))
    reference, ref_hashes = capture(root, row.get("reference"))
    hashes.update(ref_hashes)
    if current["captureId"] == reference["captureId"]:
        raise ValueError("Verification must use a separate capture from enrollment")
    for key in ("objectId", "profile", "position"):
        if current["label"][key] != reference["label"][key]:
            raise ValueError("Capture and reference have different " + key)
    path = safe_path(root, row.get("evidence"))
    evidence = read_json(path)
    if evidence.get("captureId") != current["captureId"] or evidence.get("referenceCaptureId") != reference["captureId"]:
        raise ValueError("Evidence provenance does not match its captures")
    required_text(evidence.get("pipelineVersion"), "pipelineVersion")
    values = evidence.get("values")
    if not isinstance(values, dict) or not values:
        raise ValueError("Evidence must contain actual derived values")
    check_values(values)
    hashes[str(path.relative_to(root))] = digest(path)
    keys = {("run", row["runId"])}
    for meta in (current, reference):
        keys.update({("object", meta["label"]["objectId"]),
                     ("session", meta["label"]["sessionId"]), ("capture", meta["captureId"])})
    provenance = {"captureId": current["captureId"], "referenceCaptureId": reference["captureId"],
                  "groupKeys": sorted(keys), "files": hashes, "condition": current["label"]["condition"],
                  "profile": current["label"]["profile"], "position": current["label"]["position"],
                  "pipelineVersion": evidence["pipelineVersion"], "runId": row["runId"],
                  "annotator": row["annotator"], "labelledAtUtc": row["labelledAtUtc"],
                  "modelVersions": evidence.get("modelVersions", {}),
                  "calibrationVersion": evidence.get("calibrationVersion")}
    state = json.dumps({"profile": current["label"]["profile"], "evidence": values},
                       ensure_ascii=False, sort_keys=True, allow_nan=False)
    return {"state": state, "questions": QUESTIONS, "expected": {"integrity": row["label"]},
            "provenance": provenance}


def connected_groups(rows):
    parents = list(range(len(rows)))

    def find(index):
        while parents[index] != index:
            parents[index] = parents[parents[index]]
            index = parents[index]
        return index

    owners = {}
    for index, row in enumerate(rows):
        for key in row["provenance"]["groupKeys"]:
            key = tuple(key)
            if key in owners:
                parents[find(index)] = find(owners[key])
            owners[key] = index
    groups = {}
    for index, row in enumerate(rows):
        groups.setdefault(find(index), []).append(row)
    return list(groups.values())


def check_split(parts):
    owners = {}
    captures = set()
    versions = set()
    states = {}
    for split, rows in parts.items():
        if not rows:
            raise ValueError("Empty " + split + " split")
        for row in rows:
            prov = row["provenance"]
            cid = prov["captureId"]
            if cid in captures:
                raise ValueError("Duplicate capture annotation: " + cid)
            captures.add(cid)
            if row.get("questions") != QUESTIONS or row.get("expected", {}).get("integrity") not in LABELS:
                raise ValueError("Unsupported question or label")
            versions.add(prov["pipelineVersion"])
            for key in prov["groupKeys"]:
                key = tuple(key)
                if key in owners and owners[key] != split:
                    raise ValueError("Object/session/reference/run leakage across splits: " + str(key))
                owners[key] = split
            if row["state"] in states and states[row["state"]] != split:
                raise ValueError("Identical evidence in different splits")
            states[row["state"]] = split
    if len(versions) != 1:
        raise ValueError("Mixed evidence pipeline versions")


def prepare(index, root, out, seed=20261009):
    rows = [validate_row(json.loads(line), root.resolve()) for line in index.read_text(encoding="utf-8-sig").splitlines()
            if line.strip()]
    if not rows:
        raise ValueError("No genuine labeled captures; physical experiments are required")
    rows.sort(key=lambda row: row["provenance"]["captureId"])
    groups = connected_groups(rows)
    if len(groups) < 3:
        raise ValueError("Need at least three independent object/session/reference/run components")
    random.Random(seed).shuffle(groups)
    n_test = max(1, round(len(groups) * 0.2))
    n_calib = min(max(1, round(len(groups) * 0.2)), len(groups) - n_test - 1)
    selections = {"test": groups[:n_test], "calibration": groups[n_test:n_test + n_calib],
                  "train": groups[n_test + n_calib:]}
    parts = {name: [row for group in subset for row in group] for name, subset in selections.items()}
    check_split(parts)
    if out.exists():
        raise ValueError("Use a new output directory; datasets are immutable: " + str(out))
    out.mkdir(parents=True)
    manifests = {}
    for split, items in parts.items():
        path = out / (split + ".jsonl")
        path.write_text("".join(json.dumps(row, ensure_ascii=False, allow_nan=False) + "\n" for row in items), encoding="utf-8")
        manifests[split] = {"file": path.name, "sha256": digest(path), "count": len(items),
                            "labels": dict(Counter(row["expected"]["integrity"] for row in items))}
    report = {"schemaVersion": 1, "annotationSha256": digest(index), "seed": seed,
              "splitUnit": "connected object/session/run/shared reference", "components": len(groups),
              "pipelineVersion": rows[0]["provenance"]["pipelineVersion"], "splits": manifests,
              "source": "human-labeled physical captures; authenticity requires experimental supervision"}
    (out / "manifest.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    return report


def load_prepared(directory):
    manifest = read_json(directory / "manifest.json")
    if manifest.get("schemaVersion") != 1:
        raise ValueError("Unsupported prepared schema")
    parts = {}
    for split in ("train", "calibration", "test"):
        item = manifest["splits"][split]
        path = directory / (split + ".jsonl")
        if item["file"] != path.name or digest(path) != item["sha256"]:
            raise ValueError("Prepared split was changed: " + split)
        parts[split] = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line]
        if len(parts[split]) != item["count"]:
            raise ValueError("Prepared split count does not match")
    check_split(parts)
    return manifest, parts


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("index", type=Path)
    parser.add_argument("--capture-root", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--seed", type=int, default=20261009)
    args = parser.parse_args()
    try:
        print(json.dumps(prepare(args.index, args.capture_root, args.out, args.seed), indent=2))
    except (ValueError, OSError, KeyError, TypeError) as exc:
        parser.exit(2, "Dataset refused: " + str(exc) + "\n")
