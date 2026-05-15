# Final Run Summary

Date: 2026-05-15
Released result file:
`bench/results/final/combined-results.csv`

## Run Summary

| Item | Value |
| --- | ---: |
| Tasks | 36 |
| Models | 3 |
| Conditions | 2 |
| Trials | 3 |
| Repair attempts | 648 |
| Scored rows | 1,296 |

Models:

- `deepseek-coder:6.7b`
- `qwen2.5-coder:14b`
- `qwen3-coder:30b`

## Headline Result

Under the primary normalized outcome, evidence-assisted prompts outperform the
baseline overall:

| Condition | Passed | Total | Rate |
| --- | ---: | ---: | ---: |
| error-only / failure-only | 194 | 324 | 59.9% |
| evidence-assisted | 228 | 324 | 70.4% |

The paired attempt-level profile is:

| Pair outcome | Count |
| --- | ---: |
| Evidence-assisted improves over baseline | 53 |
| Evidence-assisted worsens relative to baseline | 19 |
| Same outcome | 252 |

This is a real overall advantage, but not a uniform one.

Paired bootstrap intervals over normalized model-task-trial attempts
(`20,000` resamples) give:

| Slice | Difference | 95% paired-bootstrap CI |
| --- | ---: | ---: |
| all tasks | +10.5 pp | +5.6 to +15.4 pp |
| value provenance | +8.3 pp | +2.3 to +14.4 pp |
| low-leakage value provenance | -4.0 pp | -10.1 to +2.0 pp |
| medium-leakage value provenance | +20.4 pp | +10.2 to +30.6 pp |

A task-clustered sensitivity analysis, which resamples tasks and keeps each
sampled task's paired model/trial attempts together, gives:

| Slice | Difference | 95% task-clustered CI |
| --- | ---: | ---: |
| all tasks | +10.5 pp | +3.1 to +18.8 pp |
| value provenance | +8.3 pp | -1.4 to +19.4 pp |
| low-leakage value provenance | -4.0 pp | -11.1 to +3.0 pp |
| medium-leakage value provenance | +20.4 pp | +4.6 to +38.0 pp |

## Strict Versus Normalized Scoring

| Scoring mode | Baseline | Evidence-assisted |
| --- | ---: | ---: |
| strict | 155 / 324 | 188 / 324 |
| normalized | 194 / 324 | 228 / 324 |

The strict/normalized split remains methodologically important. Across all rows,
normalized scoring recovers more successful repairs than strict scoring
(`422 / 648` versus `343 / 648`), confirming that protocol compliance and
recoverable semantic repair remain distinct measured behaviors.

## By Model

Normalized repair success:

| Model | Baseline | Evidence-assisted | Difference |
| --- | ---: | ---: | ---: |
| `deepseek-coder:6.7b` | 39 / 108 | 41 / 108 | +1.9 pp |
| `qwen2.5-coder:14b` | 77 / 108 | 89 / 108 | +11.1 pp |
| `qwen3-coder:30b` | 78 / 108 | 98 / 108 | +18.5 pp |

Model identity remains a major effect. The weakest model gains little from the
evidence channel, while the two stronger models show clearer positive effects.

## By Evidence Type

Normalized repair success:

| Evidence type | Baseline | Evidence-assisted | Difference |
| --- | ---: | ---: | ---: |
| value provenance | 138 / 216 | 156 / 216 | +8.3 pp |
| branch behavior | 13 / 54 | 25 / 54 | +22.2 pp |
| final-value history | 43 / 54 | 47 / 54 | +7.4 pp |

The value-provenance slice remains the confirmatory study mechanism and shows a
modest positive effect. Branch behavior shows the largest gain, but that slice
is smaller and remains exploratory under the frozen protocol.

## Leakage Sensitivity

Normalized repair success:

| Leakage slice | Baseline | Evidence-assisted | Difference |
| --- | ---: | ---: | ---: |
| low leakage, all evidence types | 117 / 135 | 115 / 135 | -1.5 pp |
| medium leakage, all evidence types | 77 / 180 | 113 / 180 | +20.0 pp |
| high leakage, all evidence types | 0 / 9 | 0 / 9 | 0.0 pp |
| low-leakage value provenance | 88 / 99 | 84 / 99 | -4.0 pp |
| medium-leakage value provenance | 50 / 108 | 72 / 108 | +20.4 pp |

This is the most important qualification on the aggregate result. The overall
benefit is not driven by the single high-leakage calibration task, but it is
concentrated in medium-leakage traces rather than the cleanest low-leakage
subset. The final paper should therefore avoid claiming that compact evidence
helps uniformly, or that the cleanest provenance evidence already demonstrates a
stable benefit.

## By Difficulty

Normalized repair success:

| Difficulty | Baseline | Evidence-assisted | Difference |
| --- | ---: | ---: | ---: |
| medium | 94 / 162 | 110 / 162 | +9.9 pp |
| hard | 100 / 162 | 118 / 162 | +11.1 pp |

The final run does not show a large difficulty split. Hard tasks do retain a
slightly larger lift, but the more important interactions are with model and
leakage profile.

## Failure Profile

Across both scoring modes:

| Failure label | Count |
| --- | ---: |
| `syntax_error` | 326 |
| `wrong_output` | 161 |
| `runtime_error_after_patch` | 22 |
| `test_setup_error` | 22 |

`test_setup_error` remains meaningful rather than incidental: it typically
marks repairs that rewrite literals or source fragments that replacement-based
multi-test variants expect to preserve, exposing brittle or overfit repairs.

## Current Best Interpretation

The final run supports a more precise study claim:

> Compact runtime evidence changes repair behavior, but its benefit is
> conditional. In the frozen 36-task study, evidence-assisted prompting improves
> normalized repair success overall and shows a modest positive effect on the
> confirmatory value-provenance slice. However, the gain is concentrated in
> medium-leakage traces and stronger models, while low-leakage value-provenance
> tasks show no positive advantage. The main contribution is therefore a
> controlled method for identifying when runtime evidence helps, when it does
> not, and how scoring and task design change that interpretation.

## Final Interpretation

1. The frozen final run supports a conditional rather than universal evidence
   effect claim.
2. The 30-repair evidence-use audit is complete and summarized separately.
3. Leakage-sensitive interpretation is required for the headline result.
4. Branch-behavior and final-value-history findings remain exploratory slices
   rather than overextended headline claims.
