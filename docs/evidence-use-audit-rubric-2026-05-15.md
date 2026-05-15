# Evidence-Use Audit Rubric

Date: 2026-05-15

## Purpose

This rubric standardizes the final qualitative evidence-use audit. The labels
describe observed repair behavior; they are not ground truth about hidden model
reasoning.

## Primary Labels

| Label | Definition | Positive indicator | Exclusion note |
| --- | --- | --- | --- |
| `used_evidence` | The repair is consistent with following the supplied runtime evidence to the relevant value, branch, or history behavior. | The patch changes the traced field, helper, predicate, or state update in a way that matches the evidence. | Do not use merely because the repair passed. |
| `ignored_evidence` | The repair does not address the behavior identified by the evidence. | The wrong source, branch, or state update remains unchanged despite the evidence. | Use only when the output is interpretable enough to tell that the evidence target was untouched. |
| `misused_evidence` | The repair appears to react to the evidence but applies it incorrectly or too broadly. | The model changes the traced region but creates a brittle rewrite, representation error, or wrong-source fix. | Distinguish from `ignored_evidence`: some trace-relevant action must be visible. |
| `evidence_not_needed` | The evidence-assisted repair is not plausibly necessary because an equivalent baseline repair already appears for the same model/task context. | Paired baseline output already produces the same or materially equivalent fix. | This is a comparative interpretation, not a claim about hidden cognition. |
| `unclear` | The output is too malformed, incomplete, or heavily rewritten to classify reliably. | Syntax fragments, wholesale rewrites, or ambiguous changes prevent a stable interpretation. | Prefer `unclear` over forced classification when evidence is weak. |

## Worked Examples

### `used_evidence`

```text
Trace: adjustment.amount came from raw.price; expected discount should come from raw.discount.
Repair: amount: raw.discount
```

### `ignored_evidence`

```text
Trace: extractAmount returned raw.backup.total while raw.current.total was also shown.
Repair: leaves extractAmount returning raw.backup.total unchanged.
```

### `misused_evidence`

```text
Trace: discount source is wrong.
Repair: parses the backup value earlier but still keeps raw.backup.total.
```

### `evidence_not_needed`

```text
The provenance-assisted repair changes groups[0] to groups[1], but the paired
error-only repair for the same model/task/trial family already made the same
semantic fix.
```

### `unclear`

```text
The output mentions the relevant field but is syntactically malformed enough
that no stable patch interpretation can be made.
```

## Secondary Labels

Optional secondary labels may capture failure texture:

- `clean_semantic_fix`
- `alternate_semantic_fix`
- `downstream_rerouting_fix`
- `representation_overrepair`
- `test_specific_data_rewrite`
- `protocol_or_syntax_failure`
- `unchanged_wrong_source`

## Inter-Rater Plan

If a second rater is available before the paper version:

1. independently label at least 10 of the 30 final-audit repairs;
2. hide the first rater's labels during the second pass;
3. compute Cohen's kappa for the five primary labels;
4. reconcile disagreements only after agreement is recorded;
5. report the agreement score and the final reconciled label counts.

If a second rater is not available, the paper must state that the audit is
single-rater and interpretive.

## Reporting Rule

Use wording such as:

```text
consistent with evidence use
appears to ignore the evidence
appears to misuse the evidence
```

Avoid:

```text
the model definitely used the evidence
```
