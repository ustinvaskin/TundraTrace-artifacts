# Final Evaluation Specification

Date: 2026-05-15
Status: frozen final evaluation protocol

## 1. Study Claim

This study does not test whether runtime evidence is universally beneficial.
It tests how compact runtime evidence changes LLM repair behavior across models,
bug types, evidence types, task difficulty, and scoring regimes.

The intended study claim is:

> Compact runtime evidence is a conditional repair signal. It can improve repair
> success on some tasks, especially harder value-flow tasks with genuine
> localization headroom, but its effect is shaped by model capability, bug type,
> evidence type, task difficulty, and output-compliance behavior.

## 2. Research Questions

| RQ | Question |
| --- | --- |
| RQ1 | Does compact runtime evidence improve normalized repair success compared with error-only or failure-only prompts? |
| RQ2 | How do evidence effects vary by model, bug type, evidence type, and task difficulty? |
| RQ3 | How much do strict and normalized scoring differ, and what does that reveal about repair ability versus output-protocol compliance? |
| RQ4 | In selected repairs, do evidence-assisted outputs appear to use, ignore, misuse, or not need the evidence? |

## 3. Primary Hypotheses

These are directional study expectations, not claims assumed before analysis.

| ID | Hypothesis |
| --- | --- |
| H1 | Evidence-assisted prompting improves normalized repair success overall, but the aggregate effect is modest rather than universal. |
| H2 | The positive effect is strongest on value-provenance tasks with nontrivial value-flow localization demands. |
| H3 | Model identity explains substantial variation, with weaker models showing more syntax/protocol failure and stronger models reaching ceiling on easier tasks. |
| H4 | Strict scoring is lower than normalized scoring, especially for weaker models, because output-protocol compliance is distinct from recoverable repair content. |
| H5 | Qualitative audits reveal a mix of evidence use, evidence non-need, misuse, and occasional ignoring rather than a single uniform response pattern. |

## 4. Fixed Experimental Design

### 4.1 Models

The final model set is fixed at three local coding models:

| Model | Role |
| --- | --- |
| `deepseek-coder:6.7b` | Weaker baseline that exposes syntax and protocol failures |
| `qwen2.5-coder:14b` | Strong non-ceiling middle model |
| `qwen3-coder:30b` | Stronger newer model that tests remaining task headroom |

No additional models are added to the frozen final run.

### 4.2 Prompt Conditions

| Condition | Description |
| --- | --- |
| Baseline | Error-only for runtime-error tasks; failure-only for wrong-output tasks |
| Evidence-assisted | Same program and oracle information plus compact runtime evidence |

Prompt version is fixed at `v2`. Earlier `v1` results remain developmental and
are not pooled into the final claims.

### 4.3 Task Set

The final task set is fixed at **36 tasks**.

Target composition:

| Dimension | Target |
| --- | ---: |
| Total tasks | 36 |
| Value-provenance tasks | 24 |
| Branch-behavior tasks | 6 |
| Final-value-history tasks | 6 |
| Medium difficulty | 18 |
| Hard difficulty | 18 |

Final task inventory:

| Dimension | Current |
| --- | ---: |
| Total tasks | 36 |
| Value-provenance tasks | 24 |
| Branch-behavior tasks | 6 |
| Final-value-history tasks | 6 |
| Medium difficulty | 18 |
| Hard difficulty | 18 |

### 4.4 Primary Analysis Set

The primary full-study analysis includes all 36 tasks.

The confirmatory evidence-effect slice is the value-provenance subset, because:

- it is the central study mechanism;
- it has the strongest prior support;
- it is the largest evidence-type stratum;
- the branch-behavior and final-value-history strata are useful but smaller and
  more exploratory.

Branch-behavior and final-value-history tasks are included to test whether the
benchmark can distinguish evidence types, but they are not powered for strong
independent conclusions.

`repair_026_list_record_value_flow` remains in the benchmark as a hard
calibration task and is reported separately when leakage-sensitive claims are
made. It should not be treated as a clean low-leakage exemplar when interpreting
the main value-provenance effect.

### 4.5 Trials and Scoring

| Element | Fixed choice |
| --- | --- |
| Trials per model-condition-task | 3 |
| Scoring modes | strict and normalized |
| Primary outcome | normalized repair success |
| Secondary outcome | strict repair success |
| Additional checks | parse validity, runtime success, multi-test pass count, failure label |

The fixed run size is:

```text
36 tasks x 3 models x 2 conditions x 3 trials = 648 repair attempts
648 attempts x 2 scoring modes = 1,296 scored rows
```

## 5. Task Inclusion and Exclusion Rules

### Include a task when

- it has a clear oracle repair intent;
- it has complete metadata;
- wrong-output variants have multi-test coverage where shallow constants are
  plausible;
- its compact evidence explains behavior without directly spelling out the
  patch;
- it contributes useful coverage to the frozen evidence-type and difficulty
  mix.

### Do not add a task when

- it is only a near-duplicate of an existing task;
- it exists mainly to inflate task count;
- it is so easy that all strong models are already at ceiling;
- it is so hard that all models remain at floor and it adds no interpretive
  value beyond calibration;
- its evidence is effectively a direct answer key.

### Calibration tasks

Calibration tasks may remain in the set when they serve a clear role:

- `repair_027` checks ceiling/stability behavior;
- `repair_026` remains a hard calibration task and is reported separately when
  leakage-sensitive claims are made;
- model-separation tasks such as `repair_029` remain useful even when they do
  not estimate an evidence effect directly.

## 6. Unit of Analysis and Independence

The primary unit of analysis is:

```text
model x task x condition x trial
```

Each repair attempt is scored under both strict and normalized modes, but those
paired scoring rows are two views of the same attempt rather than independent
observations.

Because attempts repeat across the same models and tasks, rows are not treated
as fully independent in inferential analyses.

## 7. Statistical Analysis Plan

The primary descriptive effect is the normalized repair-success difference
between evidence-assisted and baseline prompts.

The final analysis reports:

- descriptive pass-rate differences;
- confidence intervals for the main descriptive contrasts;
- odds ratios for the main modeled contrasts where a fitted model is reported;
- stratified summaries by model, task, bug type, evidence type, difficulty, and
  scoring mode.

Where sample size permits, a mixed-effects logistic regression is used with
condition as the main fixed effect and task/model as grouped factors.

Primary inference is reserved for the main condition effect and the
value-provenance slice. Additional comparisons are either corrected for
multiple testing or labeled explicitly as exploratory.

Before the final write-up, the observed design should be compared with the
smallest effect size considered meaningful for the study so the discussion can
state whether the study is powered for large effects only or also for moderate
effects.

The value-provenance subset is treated as the main confirmatory slice.
Analyses by branch-behavior and final-value-history evidence are exploratory
because those strata are smaller.

## 8. Normalization Rules

Normalization may remove only non-semantic wrappers around the candidate
program. It may:

- remove Markdown fences;
- remove leading or trailing prose;
- remove surrounding whitespace;
- extract one obvious Tundra code block when the model wraps the answer in
  Markdown.

Normalization may not:

- add missing syntax;
- rename variables;
- change expressions;
- insert semicolons;
- repair brackets;
- modify literals;
- reorder code;
- alter program behavior.

Strict scoring evaluates the raw model output. Normalized scoring remains a
diagnostic estimate of recoverable repair content, not a second repair system.

## 9. Trace-Leakage Risk

Each task receives a trace-leakage label:

| Label | Meaning |
| --- | --- |
| `low` | Evidence explains value origin or runtime behavior without identifying the edit. |
| `medium` | Evidence narrows the suspicious field, expression, helper, or branch but still requires the model to form the patch. |
| `high` | Evidence nearly states the repair or corrected expression. |

Primary evidence-effect claims are reported both with all value-provenance tasks
and, where relevant, with high-leakage tasks excluded. The final analysis also
reports descriptive leakage-sensitivity checks following
`docs/trace-leakage-rubric.md`.

## 10. Qualitative Evidence-Use Audit

The final qualitative audit is fixed at **30 selected repairs** from the final
study.

Sampling targets:

| Slice | Target |
| --- | ---: |
| Evidence-assisted passes | 10 |
| Evidence-assisted failures | 10 |
| Paired cases where baseline already passes | 5 |
| Paired cases where evidence appears to worsen behavior | 5 |

The audit should cover:

- at least 15 value-provenance repairs;
- at least 5 branch-behavior repairs;
- at least 5 final-value-history repairs;
- all three models;
- both medium and hard tasks.

Labels remain:

```text
used_evidence
ignored_evidence
misused_evidence
evidence_not_needed
unclear
```

The final audit uses the written rubric in
`docs/evidence-use-audit-rubric-2026-05-15.md`. If a second rater is
available, at least 10 repairs are independently double-labeled and agreement
is reported before reconciliation.

Audit language must remain interpretive:

```text
consistent with evidence use
```

not:

```text
the model definitely used the evidence
```

## 11. Planned Result Tables

The final write-up must include at least these four operational summaries:

1. `condition x scoring_mode`
2. `task x condition x scoring_mode`
3. `model x task x condition x scoring_mode`
4. `failure_label x model x task`

It should also include:

5. `evidence_type x condition x scoring_mode`
6. `bug_type x condition x scoring_mode`
7. `difficulty x condition x scoring_mode`
8. evidence-use audit label counts with selected examples

## 12. Planned Research-Question Answer Table

This table should be filled only after the final run, but its shape is fixed now
so the analysis remains anchored to the study questions.

| Research question | Planned answer form |
| --- | --- |
| RQ1 | Report the normalized evidence effect overall and on the value-provenance slice. |
| RQ2 | Report where effects differ by model, bug type, evidence type, and difficulty. |
| RQ3 | Report the strict-normalized gap and identify which models/tasks drive it. |
| RQ4 | Report qualitative label counts and a few paired examples of use, non-need, misuse, and ignoring. |

## 13. Stop Rules

Stop expanding the study once all of the following are true:

1. the benchmark contains the frozen 36-task mix;
2. the three-model final run is complete;
3. the planned summary tables are generated;
4. the 30-repair qualitative audit is complete;
5. the research-question answer table can be filled without inventing new
   metrics or new prompt conditions.

Do not add before the frozen final run:

- extra models;
- extra prompt variants;
- interactive loops;
- agentic repair systems;
- real-language transfer benchmarks;
- complex automatic trace-use detection;
- large statistical machinery beyond what the study actually needs.

## 14. Immediate Next Work

1. Run one small validation smoke over the completed 36-task set.
2. Execute the fixed final run exactly as specified above.
