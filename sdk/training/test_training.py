"""Schema/leakage tests only. These fixtures are never physical training evidence."""
import copy
import json
import tempfile
import unittest
from pathlib import Path

from prepare import (QUESTIONS, capture, check_split, connected_groups, safe_path,
                     validate_row, check_values, prepare, load_prepared, digest)
from run_laya import (SPECIALISTS, validate_checkpoint_lineage, validate_training,
                      verified_specialists)


def row(cid, keys, label="MATCH"):
    return {"state": cid, "questions": QUESTIONS, "expected": {"integrity": label},
            "provenance": {"captureId": cid, "groupKeys": keys, "pipelineVersion": "physics-v1",
                           "modelVersions": {}, "calibrationVersion": None}}


class TrainingSafetyTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.artifacts = Path(__file__).parent / "artifacts"
        cls.artifacts.mkdir(exist_ok=True)

    def test_transitive_object_session_bridge_stays_together(self):
        rows = [row("a", [("object", "o1"), ("session", "s1")]),
                row("b", [("object", "o2"), ("session", "s1")]),
                row("c", [("object", "o2"), ("session", "s2")]),
                row("d", [("object", "o3"), ("session", "s3")])]
        self.assertEqual(sorted(len(group) for group in connected_groups(rows)), [1, 3])

    def test_shared_enrollment_reference_leaks(self):
        parts = {"train": [row("a", [("capture", "ref1")])],
                 "calibration": [row("b", [("capture", "ref1")])],
                 "test": [row("c", [("object", "other")])]}
        with self.assertRaisesRegex(ValueError, "leakage"):
            check_split(parts)

    def test_duplicate_annotation_and_identical_input_refused(self):
        parts = {name: [row(name, [("object", name)])] for name in ("train", "calibration", "test")}
        changed = copy.deepcopy(parts)
        changed["test"][0]["provenance"]["captureId"] = "train"
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            check_split(changed)
        parts["test"][0]["state"] = "train"
        with self.assertRaisesRegex(ValueError, "Identical evidence"):
            check_split(parts)

    def test_unknown_label_is_rejected_before_file_access(self):
        with self.assertRaisesRegex(ValueError, "Label"):
            validate_row({"label": "SUCCESS"}, Path("not-a-capture-root"))

    def test_labels_and_nonfinite_values_cannot_enter_evidence(self):
        for value in ({"nested": {"condition": "removed contents"}}, {"x": float("nan")}):
            with self.assertRaises(ValueError):
                check_values(value)
        for key in ("expectedLabel", "conditionName", "objectIdHash", "objectIDHash",
                    "gold_label", "referenceCaptureId", "labelledAtUtc",
                    "annotationNotes", "answer"):
            with self.subTest(key=key), self.assertRaisesRegex(ValueError, "Annotation/identity"):
                check_values({"nested": {key: "annotated"}})
        for value in ({"quality": "CHANGED"}, {"metric": True}):
            with self.subTest(value=value), self.assertRaisesRegex(ValueError, "numeric or null"):
                check_values(value)
        check_values({"missingFeature": None})
        check_values({"reference_reliability": 0.8, "acoustic_band_energy": [0.1, 0.2]})

    def test_primitive_physics_cannot_train_final_laya(self):
        parts = {name: [row(name + label, [], label) for label in ("MATCH", "CHANGED", "INCONCLUSIVE")]
                 for name in ("train", "calibration", "test")}
        with self.assertRaisesRegex(ValueError, "trained specialists"):
            validate_training(parts, {})

    def test_specialist_versions_must_match_artifacts_and_cannot_change(self):
        parts = {name: [row(name + str(index), [], ("MATCH", "CHANGED", "INCONCLUSIVE")[index % 3])
                       for index in range(12)] for name in ("train", "calibration", "test")}
        hashes = {name: "a" * 64 for name in SPECIALISTS}
        for rows in parts.values():
            for item in rows:
                item["provenance"].update(modelVersions=dict(hashes), calibrationVersion="schema-fixture")
        validate_training(parts, hashes)
        parts["test"][0]["provenance"]["modelVersions"]["EchoNet"] = "b" * 64
        with self.assertRaisesRegex(ValueError, "does not match the supplied specialist artifact"):
            validate_training(parts, hashes)

    def test_verified_specialist_hashes_come_from_local_artifact_bytes(self):
        parts = {name: [row(name + str(index), [], ("MATCH", "CHANGED", "INCONCLUSIVE")[index % 3])
                       for index in range(12)] for name in ("train", "calibration", "test")}
        with tempfile.TemporaryDirectory(dir=self.artifacts) as directory:
            root = Path(directory)
            artifacts = {}
            for name in SPECIALISTS:
                artifact = root / (name + ".onnx")
                artifact.write_bytes(name.encode("ascii"))
                artifacts[name] = artifact.name
            manifest = root / "specialists.json"
            manifest.write_text(json.dumps({"schemaVersion": 1, "artifacts": artifacts}))
            hashes = verified_specialists(manifest)
            for rows in parts.values():
                for item in rows:
                    item["provenance"].update(modelVersions=dict(hashes), calibrationVersion="schema-fixture")
            validate_training(parts, hashes)
            (root / artifacts["EchoNet"]).write_bytes(b"changed")
            changed_hashes = verified_specialists(manifest)
            with self.assertRaisesRegex(ValueError, "does not match the supplied specialist artifact"):
                validate_training(parts, changed_hashes)

    def test_checkpoint_keeps_specialist_lineage(self):
        hashes = {name: "a" * 64 for name in SPECIALISTS}
        dataset = {"schemaVersion": 1}
        config = {"fine_tuned": True, "training": {"capybara": {
            "dataset": dataset, "specialistArtifacts": dict(hashes)}}}
        self.assertEqual(validate_checkpoint_lineage(config, dataset, hashes),
                         config["training"]["capybara"])
        with self.assertRaisesRegex(ValueError, "missing Capybara lineage"):
            validate_checkpoint_lineage([], dataset, hashes)
        config["training"]["capybara"]["specialistArtifacts"]["EchoNet"] = "b" * 64
        with self.assertRaisesRegex(ValueError, "provenance"):
            validate_checkpoint_lineage(config, dataset, hashes)

    def test_paths_cannot_escape_capture_root(self):
        with tempfile.TemporaryDirectory(dir=self.artifacts) as directory:
            root = Path(directory) / "captures"
            root.mkdir()
            (Path(directory) / "outside.json").write_text("{}")
            with self.assertRaisesRegex(ValueError, "under the capture root"):
                safe_path(root, "../outside.json")

    def test_pcm_size_must_match_real_capture_metadata(self):
        with tempfile.TemporaryDirectory(dir=self.artifacts) as directory:
            root = Path(directory)
            meta = {"schemaVersion": 1, "captureId": "fixture", "sampleRate": 48000,
                    "sampleCount": 2, "pcmEncoding": "signed_pcm16_little_endian_mono",
                    "startedBootNanos": 1, "endedBootNanos": 2,
                    "label": {"objectId": "fixture", "sessionId": "fixture", "condition": "test",
                              "profile": "test", "position": "A"}}
            (root / "metadata.json").write_text(json.dumps(meta))
            (root / "audio.pcm").write_bytes(b"\0\0")
            (root / "imu.csv").write_text("type,timestampNanos,x,y,z\n")
            with self.assertRaisesRegex(ValueError, "PCM16 byte count"):
                capture(root, "metadata.json")

    def test_prepare_round_trip_and_tamper_detection(self):
        with tempfile.TemporaryDirectory(dir=self.artifacts) as directory:
            root = Path(directory)
            annotations = []
            for group in range(3):
                for role in ("reference", "current"):
                    cid = str(group) + role
                    folder = root / cid
                    folder.mkdir()
                    meta = {"schemaVersion": 1, "captureId": cid, "sampleRate": 48000,
                            "sampleCount": 1, "pcmEncoding": "signed_pcm16_little_endian_mono",
                            "startedBootNanos": 1, "endedBootNanos": 2,
                            "metadata": {"audioSampleCount": "1", "audioExpectedSamples": "1"},
                            "label": {"objectId": str(group), "sessionId": str(group),
                                      "condition": "schema-fixture", "profile": "test", "position": "A"}}
                    audio_path = folder / "audio.pcm"
                    audio_path.write_bytes(b"\0\0")
                    imu_path = folder / "imu.csv"
                    imu_path.write_text("type,timestampNanos,x,y,z\n")
                    meta["audioSha256"] = digest(audio_path)
                    meta["imuSha256"] = digest(imu_path)
                    (folder / "metadata.json").write_text(json.dumps(meta))
                evidence = {"captureId": str(group) + "current", "referenceCaptureId": str(group) + "reference",
                            "pipelineVersion": "physics-v1", "values": {"schemaFixture": group}}
                (root / (str(group) + "current") / "evidence.json").write_text(json.dumps(evidence))
                annotations.append({"capture": str(group) + "current/metadata.json",
                                    "reference": str(group) + "reference/metadata.json",
                                    "evidence": str(group) + "current/evidence.json", "runId": str(group),
                                    "label": "MATCH", "annotator": "schema-test", "labelledAtUtc": "2026-10-09T00:00:00Z"})
            evidence_path = root / "0current" / "evidence.json"
            original_evidence = evidence_path.read_text()
            evidence = json.loads(original_evidence)
            for values in ({"quality": "CHANGED"}, {"expectedLabel": 1}):
                evidence["values"] = values
                evidence_path.write_text(json.dumps(evidence))
                with self.subTest(values=values), self.assertRaises(ValueError):
                    validate_row(annotations[0], root)
            evidence_path.write_text(original_evidence)
            current_meta_path = root / "0current" / "metadata.json"
            current_meta = json.loads(current_meta_path.read_text())
            current_meta["metadata"]["audioExpectedSamples"] = "2"
            current_meta_path.write_text(json.dumps(current_meta))
            with self.assertRaisesRegex(ValueError, "must be labeled INCONCLUSIVE"):
                validate_row(annotations[0], root)
            annotations[0]["label"] = "INCONCLUSIVE"
            self.assertEqual(validate_row(annotations[0], root)["expected"]["integrity"], "INCONCLUSIVE")
            annotations[0]["label"] = "MATCH"
            current_meta["metadata"]["audioExpectedSamples"] = "1"
            current_meta_path.write_text(json.dumps(current_meta))
            reference_meta_path = root / "0reference" / "metadata.json"
            reference_meta = json.loads(reference_meta_path.read_text())
            reference_meta["metadata"]["audioSampleCount"] = "0"
            reference_meta_path.write_text(json.dumps(reference_meta))
            with self.assertRaisesRegex(ValueError, "Reference capture audio is incomplete"):
                validate_row(annotations[0], root)
            reference_meta["metadata"]["audioSampleCount"] = "1"
            reference_meta_path.write_text(json.dumps(reference_meta))
            for name, message in (("audio.pcm", "Audio PCM digest"), ("imu.csv", "IMU digest")):
                artifact = root / "0current" / name
                original = artifact.read_bytes()
                artifact.write_bytes(bytes((original[0] ^ 1,)) + original[1:])
                with self.subTest(artifact=name), self.assertRaisesRegex(ValueError, message):
                    capture(root, "0current/metadata.json")
                artifact.write_bytes(original)
            metadata_path = root / "0current" / "metadata.json"
            original_metadata = metadata_path.read_text()
            metadata = json.loads(original_metadata)
            metadata["pcmEncoding"] = "float32"
            metadata_path.write_text(json.dumps(metadata))
            with self.assertRaisesRegex(ValueError, "PCM encoding"):
                capture(root, "0current/metadata.json")
            metadata_path.write_text(original_metadata)
            index = root / "annotations.jsonl"
            index.write_text("\n".join(json.dumps(item) for item in annotations))
            out = root / "prepared"
            report = prepare(index, root, out)
            loaded_report, parts = load_prepared(out)
            self.assertEqual(report, loaded_report)
            self.assertEqual([len(parts[name]) for name in ("train", "calibration", "test")], [1, 1, 1])
            (out / "test.jsonl").write_text("{}\n")
            with self.assertRaisesRegex(ValueError, "changed"):
                load_prepared(out)


if __name__ == "__main__":
    unittest.main()
