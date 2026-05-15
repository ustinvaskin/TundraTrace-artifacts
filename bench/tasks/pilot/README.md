# Final Task Set

This directory contains the frozen 36-task TundraBench evaluation inventory used
in the final study. The directory name remains `pilot` because it is part of the
frozen experiment interface used by the prompt-generation and scoring scripts.

## Composition

| Dimension | Breakdown |
| --- | --- |
| Evidence type | 24 value provenance, 6 branch behavior, 6 final-value history |
| Difficulty | 18 medium, 18 hard |
| Leakage risk | 15 low, 20 medium, 1 high |
| Failure kind | 11 runtime, 25 wrong output |

## Task Format

Each JSON file stores the buggy program, expected repair target, prompt metadata,
task-family labels, and trace-leakage annotation needed for reproduction and
analysis. The benchmark is intentionally mixed across runtime failures and silent
wrong-output failures so that the study can distinguish syntax handling, repair
correctness, evidence type, and leakage risk.

## Reproduction Notes

- Use `--task-set pilot` when invoking the released scripts.
- The one high-leakage task is retained as a calibration case and should not be
  treated as a clean low-leakage estimate.
- See `docs/final-evaluation-spec-2026-05-15.md` for the frozen evaluation
  design and `docs/trace-leakage-rubric.md` for the leakage labels.
