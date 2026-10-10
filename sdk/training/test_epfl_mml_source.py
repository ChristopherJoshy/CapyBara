import tempfile
import unittest
import zipfile
from pathlib import Path


from train_epfl_mml_source import (CLASSES, Record, parse_record, read_records,
                                   split_records)


class EpflSourceTrainingTests(unittest.TestCase):
    def test_record_parser_ignores_unselected_and_macos_sidecars(self):
        record = parse_record("annotated_bags_mml/20210825_vertical_rice_11.bag")
        self.assertEqual(record.label, CLASSES.index("rice"))
        self.assertEqual(record.trial, 11)
        self.assertIsNone(parse_record("__MACOSX/._20210825_vertical_rice_11.bag"))
        self.assertIsNone(parse_record("annotated_bags_mml/20210913_rotation_rice_11.bag"))
        with self.assertRaises(ValueError):
            parse_record("annotated_bags_mml/20210825_vertical_rice_30.bag")

    def test_archive_contract_accepts_only_complete_selected_trial_groups(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "dataset.zip"
            with zipfile.ZipFile(path, "w") as archive:
                for date, motion in (("20210825", "vertical"), ("20210826", "rotation")):
                    for content in CLASSES:
                        for trial in range(0, 30):
                            member = f"annotated_bags_mml/{date}_{motion}_{content}_{trial}.bag"
                            archive.writestr(member, b"")
                            archive.writestr("__MACOSX/annotated_bags_mml/._" +
                                             Path(member).name, b"")
                records = read_records(archive)
            self.assertEqual(len(records), 180)
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("annotated_bags_mml/20210825_vertical_rice_0.bag", b"")
                with self.assertRaisesRegex(ValueError, "exactly 30 trials"):
                    read_records(archive)

    def test_seeded_split_holds_out_trials_and_a_complete_test_date(self):
        records = [Record(f"{date}_{motion}_{label}_{trial}", date, motion,
                          class_index, trial)
                   for class_index, label in enumerate(CLASSES)
                   for date, motion in (("20210825", "vertical"), ("20210826", "rotation"))
                   for trial in range(0, 30)]
        first = split_records(records, seed=20261010)
        second = split_records(records, seed=20261010)
        self.assertEqual(first, second)
        self.assertEqual([len(first[name]) for name in ("train", "validation", "test")],
                         [72, 18, 90])
        group_sets = {name: {(row.date, row.motion, row.label, row.trial) for row in rows}
                      for name, rows in first.items()}
        self.assertFalse(group_sets["train"] & group_sets["validation"])
        self.assertFalse(group_sets["train"] & group_sets["test"])
        self.assertFalse(group_sets["validation"] & group_sets["test"])
        self.assertEqual({row.date for row in first["test"]}, {"20210826"})



if __name__ == "__main__":
    unittest.main()
