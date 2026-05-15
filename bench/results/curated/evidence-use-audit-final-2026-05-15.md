# Final Evidence-Use Audit

Date: 2026-05-15
Released result file:
`bench/results/final/combined-results.csv`

Manual labels:
`bench/results/qualitative/evidence-use-audit-final-2026-05-15.csv`

## Sampling

The final audit follows the frozen plan:

| Slice | Count |
| --- | ---: |
| Evidence-assisted passes where baseline failed | 10 |
| Evidence-assisted failures | 10 |
| Evidence-assisted passes where the paired baseline also passed | 5 |
| Evidence-assisted cases that worsened relative to baseline | 5 |
| Total | 30 |

Coverage checks against the frozen audit plan:

| Slice | Count |
| --- | ---: |
| Value provenance | 18 |
| Branch behavior | 7 |
| Final-value history | 5 |
| Medium difficulty | 13 |
| Hard difficulty | 17 |

## Label Counts

| Label | Count |
| --- | ---: |
| `used_evidence` | 11 |
| `ignored_evidence` | 6 |
| `misused_evidence` | 7 |
| `evidence_not_needed` | 5 |
| `unclear` | 1 |

## Interpretation

The final audit supports the quantitative story rather than flattening it.

- Clean evidence-consistent repairs are common in the positive slice. Examples
  include `repair_040`, `repair_041`, `repair_034`, and `repair_028`, where the
  evidence-assisted patch changes the traced field, branch source, or copied
  value directly.
- Evidence-assisted success is still not identical to evidence use. Five audited
  passing repairs were labeled `evidence_not_needed` because the paired baseline
  already produced the same or materially equivalent fix.
- Failed repairs divide between ignoring the evidence and misusing it. The
  `repair_029` helper-source cases repeatedly leave the wrong source unchanged,
  while cases such as `repair_041` trial 3 and `repair_036` trial 2 react to the
  trace but redirect the later computation incorrectly.
- The weakest model still exhibits cases where a semantically relevant patch is
  present but malformed output prevents execution, preserving the importance of
  strict versus normalized scoring.

## Study Takeaway

The audit reinforces the final claim:

> Runtime evidence changes repair behavior, but not in one uniform way. It can
> support clean localization, remain unnecessary on easy tasks, be ignored, or be
> misused through brittle downstream rewrites.

This is why the paper should keep the qualitative audit alongside the final
pass-rate tables. The pass-rate gain is real, but the behavioral categories show
what that gain is made of.
