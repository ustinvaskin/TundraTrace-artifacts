# Run Notes

Status: completed
Purpose: final thesis-scale evaluation run
Started: 2026-05-15 14:45:16 +0200
Completed: 2026-05-15

Experiment directory:
`bench/results/experiments/20260515-144516-thesis-scale-final-2026-05-15`

Code state at run start:
`85ccc498a8bb4989252432e6161bf8b5d8bcce41`

Task set:
`pilot`

Task count:
`36`

Prompt version:
`v2`

Models:

- `deepseek-coder:6.7b`
- `qwen2.5-coder:14b`
- `qwen3-coder:30b`

Trials:
`3`

Scoring modes:

- `strict`
- `normalized`

Runner settings:

- provider: `ollama`
- timeout: `300` seconds
- resume enabled: `true`
- temperature: not explicitly configured by `run_ollama_pilot.py`; Ollama
  provider defaults applied

Experiment-surface freeze statement:

This run was started after the frozen final evaluation protocol had been written.
No task definitions, prompt templates, scorer logic, normalizer logic, or runner
logic were changed while the run was in progress. During the run, only research
documentation and result-reporting notes were edited.

Classification:

This run qualifies as the final thesis-scale evaluation run because it matches
the frozen specification:

- frozen 36-task set used;
- prompt version `v2` used;
- all three planned models used;
- three trials completed;
- strict and normalized scoring both recorded;
- no result-sensitive experiment-surface files changed after results began.

Primary output files:

- `manifest.json`
- `combined-results.csv`
- `combined-results.jsonl`
- `summary.json`
