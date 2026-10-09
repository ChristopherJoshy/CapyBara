import unittest

import torch

from evaluate_moment_uci import pad_windows, split_masks


class MomentEvaluationContractTest(unittest.TestCase):
    def test_speed_groups_are_disjoint_and_complete(self):
        speeds = torch.tensor([20, 30, 40, 60, 70, 90, 100])
        train, val, test = split_masks(speeds)
        self.assertEqual(train.nonzero().flatten().tolist(), [0])
        self.assertEqual(val.nonzero().flatten().tolist(), [1, 3, 5])
        self.assertEqual(test.nonzero().flatten().tolist(), [2, 4, 6])

    def test_padding_masks_only_the_real_window(self):
        from train_uci_vibration_baseline import WINDOW_SIZE

        # Deterministic utility fixture only; never used as sensor or training evidence.
        signals = torch.arange(3 * WINDOW_SIZE, dtype=torch.float32).reshape(1, 3, WINDOW_SIZE)
        padded, mask = pad_windows(signals[:1], 512)
        self.assertTrue(torch.equal(padded[:, :, :WINDOW_SIZE], signals[:1]))
        self.assertEqual(mask.sum().item(), WINDOW_SIZE)
        self.assertEqual(padded[:, :, WINDOW_SIZE:].abs().sum().item(), 0)


if __name__ == "__main__":
    unittest.main()
