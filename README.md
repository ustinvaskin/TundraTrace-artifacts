# TundraTrace Artifacts

Artifacts for the paper:

> **TundraTrace: Evaluating Compact Runtime Evidence for LLM Program Repair**

TundraTrace is a controlled benchmark stack for studying when compact
source-level runtime evidence changes large-language-model program-repair
behavior. This artifact repository contains:

- the Tundra interpreter source in `src/tundra/`;
- runnable language and provenance examples in `examples/`;
- the frozen 36-task benchmark, prompt templates, and scoring scripts in
  `bench/`; and
- methodology, leakage, and audit documentation in `docs/`.

The final study uses a frozen 36-task benchmark with paired baseline and
evidence-assisted prompts. In the final result matrix, evidence-assisted
prompting improves normalized repair success from `194/324` to `228/324`
overall and from `138/216` to `156/216` on the value-provenance slice.

## Repository Layout

```text
src/tundra/                  interpreter source
examples/                    runnable Tundra examples
bench/tasks/pilot/           frozen 36-task final benchmark inventory
bench/prompts/v2/            frozen final prompt templates
bench/scripts/               prompt generation, scoring, audit, and summary tools
bench/results/final/         frozen final-study outputs
bench/results/qualitative/   final qualitative audit labels
bench/results/curated/       final human-readable result summaries
docs/                        methodology and reproduction notes
```

## Quickstart

Compile the interpreter:

```sh
javac -d out src/tundra/*.java
```

Run a passing example:

```sh
java -cp out tundra.Tundra examples/runtime-basic.tds
```

Run a provenance example:

```sh
java -cp out tundra.Tundra examples/runtime-debug-chain.tds
```

## Generate Repair Prompts

Baseline prompt:

```sh
python3 bench/scripts/make_prompt.py \
  repair_021_deep_chain_type_mismatch \
  error-only \
  --task-set pilot \
  --prompt-version v2
```

Evidence-assisted prompt:

```sh
python3 bench/scripts/make_prompt.py \
  repair_021_deep_chain_type_mismatch \
  provenance-assisted \
  --task-set pilot \
  --prompt-version v2
```

## Reproduce the Final Tables

Summarize the frozen final run:

```sh
python3 bench/scripts/summarize_results.py \
  bench/results/final/combined-results.csv \
  --json
```

Audit strict-versus-normalized scoring gaps:

```sh
python3 bench/scripts/audit_strict_normalized_gaps.py \
  bench/results/final/combined-results.csv \
  --output /tmp/strict-normalized-gaps.csv
```

Recompute paired bootstrap intervals:

```sh
python3 bench/scripts/paired_bootstrap_ci.py \
  bench/results/final/combined-results.csv
```

Verify the final qualitative audit counts:

```sh
python3 - <<'PY'
import csv
import collections

rows = list(
    csv.DictReader(
        open("bench/results/qualitative/evidence-use-audit-final-2026-05-15.csv")
    )
)
print("rows", len(rows))
print(collections.Counter(row["evidence_use_label"] for row in rows))
PY
```

## Released Final Artifacts

- final benchmark tasks;
- frozen `v2` prompt templates;
- scoring and normalization pipeline;
- paired-bootstrap helper;
- final combined result files;
- final manifest and summary;
- final qualitative audit labels;
- trace-leakage rubric;
- evidence-use audit rubric;
- final-run reproduction check.

## Citation

Citation metadata is provided in `CITATION.cff` and should be updated with the final preprint citation after posting.

## License

This repository is released under the Apache License 2.0. See `LICENSE`.
