# Final Run Reproduction Check

Date: 2026-05-15

This check recomputes the paper-facing final results from the frozen released
result file:

```text
bench/results/final/combined-results.csv
```

## Structural Verification

| Quantity | Reproduced value |
| --- | ---: |
| Total scored rows | 1,296 |
| Strict rows | 648 |
| Normalized rows | 648 |
| Rows per model | 432 each |
| Paired attempts | 648 |
| Missing strict/normalized mates | 0 |

## Headline Repair Results

| Slice | Baseline | Evidence-assisted |
| --- | ---: | ---: |
| Strict, all tasks | 155 / 324 | 188 / 324 |
| Normalized, all tasks | 194 / 324 | 228 / 324 |
| Normalized, value provenance | 138 / 216 | 156 / 216 |

## Normalized Results by Model

| Model | Baseline | Evidence-assisted |
| --- | ---: | ---: |
| `deepseek-coder:6.7b` | 39 / 108 | 41 / 108 |
| `qwen2.5-coder:14b` | 77 / 108 | 89 / 108 |
| `qwen3-coder:30b` | 78 / 108 | 98 / 108 |

## Leakage-Sensitive Value-Provenance Results

| Leakage risk | Baseline | Evidence-assisted |
| --- | ---: | ---: |
| Low | 88 / 99 | 84 / 99 |
| Medium | 50 / 108 | 72 / 108 |
| High | 0 / 9 | 0 / 9 |

## Failure Labels

| Label | Count |
| --- | ---: |
| `passed` | 765 |
| `syntax_error` | 326 |
| `wrong_output` | 161 |
| `runtime_error_after_patch` | 22 |
| `test_setup_error` | 22 |

## Reproduced Confidence Intervals

Attempt-level paired bootstrap, `20,000` resamples:

| Slice | Difference | 95% CI |
| --- | ---: | ---: |
| All tasks | +10.5 pp | +5.6 to +15.4 pp |
| Value provenance | +8.3 pp | +2.3 to +14.4 pp |
| Low-leakage value provenance | -4.0 pp | -10.1 to +2.0 pp |
| Medium-leakage value provenance | +20.4 pp | +10.2 to +30.6 pp |

Task-clustered bootstrap sensitivity:

| Slice | Difference | 95% CI |
| --- | ---: | ---: |
| All tasks | +10.5 pp | +3.1 to +18.8 pp |
| Value provenance | +8.3 pp | -1.4 to +19.4 pp |
| Low-leakage value provenance | -4.0 pp | -11.1 to +3.0 pp |
| Medium-leakage value provenance | +20.4 pp | +4.6 to +38.0 pp |

## Qualitative Audit Verification

The final evidence-use audit file reproduces:

| Quantity | Count |
| --- | ---: |
| Audit cases | 30 |
| `used_evidence` | 11 |
| `ignored_evidence` | 6 |
| `misused_evidence` | 7 |
| `evidence_not_needed` | 5 |
| `unclear` | 1 |

Evidence-type coverage is `18` value-provenance, `7` branch-behavior, and `5`
final-value-history cases.

## Commands Used

```sh
python3 bench/scripts/summarize_results.py \
  bench/results/final/combined-results.csv \
  --json

python3 bench/scripts/audit_strict_normalized_gaps.py \
  bench/results/final/combined-results.csv \
  --output /tmp/strict-normalized-gaps.csv

python3 bench/scripts/paired_bootstrap_ci.py \
  bench/results/final/combined-results.csv
```

## Conclusion

The frozen raw outputs reproduce the current manuscript-facing result claims.
No discrepancy was found in the final quantitative matrix after the preprint
cleanup edits.
