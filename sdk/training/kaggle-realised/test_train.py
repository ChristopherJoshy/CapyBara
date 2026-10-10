import unittest
from pathlib import Path

import torch

from train import (CLASS_NAMES, SOURCE_NAMES, EventClassifier, Record,
                   default_artifacts_path, parse_record, split_records)


class ReaLISEDTrainingTests(unittest.TestCase):

    def test_kaggle_artifact_root_works_without_repo_tree(self):
        self.assertEqual(default_artifacts_path(Path("/kaggle/src/script.py"), True),
                         Path("/kaggle/working"))

    def test_filename_labels_event_class_and_groups_recordings(self):
        door = parse_record("dataset/doo_040_02_00_3.flac")
        furniture = parse_record("fur_058_06_01_2.flac")
        self.assertEqual(door.label, CLASS_NAMES.index("door"))
        self.assertEqual(door.group, "doo_040")
        self.assertEqual(furniture.label, CLASS_NAMES.index("furniture_movement"))
        self.assertEqual(furniture.group, "fur_058")

    def test_malformed_or_unknown_annotations_are_refused(self):
        for name in ("door.wav", "unknown_001_00_00_0.flac",
                     "doo_191_00_00_0.flac", "doo_001_11_00_0.flac",
                     "doo_001_00_14_0.flac", "doo_001_00_00_4.flac"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                parse_record(name)

    def test_event_groups_do_not_cross_splits(self):
        records = []
        for label, code in enumerate(SOURCE_NAMES):
            for event_id in range(1, 7):
                records.append(Record(f"{code}_{event_id:03}_00_00_0.flac", label,
                                      f"{code}_{event_id:03}"))
                if event_id == 1:
                    records.append(Record(f"{code}_{event_id:03}_01_00_0.flac", label,
                                          f"{code}_{event_id:03}"))
        first = split_records(records, seed=20261010)
        second = split_records(records, seed=20261010)
        self.assertEqual(first, second)
        self.assertEqual(sum(map(len, first.values())), len(records))
        groups = {name: {record.group for record in rows} for name, rows in first.items()}
        self.assertFalse(groups["train"] & groups["validation"])
        self.assertFalse(groups["train"] & groups["test"])
        self.assertFalse(groups["validation"] & groups["test"])
        self.assertEqual({record.path for rows in first.values() for record in rows},
                         {record.path for record in records})

    def test_model_produces_one_logit_per_event_class(self):
        model = EventClassifier(len(CLASS_NAMES))
        output = model(torch.zeros(2, 1, 64, 173))
        self.assertEqual(tuple(output.shape), (2, len(CLASS_NAMES)))
        self.assertTrue(torch.isfinite(output).all())


if __name__ == "__main__":
    unittest.main()
