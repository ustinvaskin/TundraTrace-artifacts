# Trace Leakage Rubric

Date: 2026-05-14

## Purpose

TundraBench uses `trace_leakage_risk` to mark how close runtime evidence is to
giving away the repair.

This matters because the study should evaluate debugging evidence, not answer
leakage.

## Labels

### `low`

The trace explains value origin or runtime behavior without naming the exact
patch.

Good low-risk example:

```text
subtotal came from cart[0].cost
cart[0].cost came from item.cost
item.cost came from raw.price
raw.price was string "10"
```

Why it is low risk:

- it identifies the bad value path;
- it does not directly say where to add `parseNumber`;
- multiple plausible repairs may remain.

### `medium`

The trace strongly points to the faulty expression, field, index, or helper, but
still requires the model to form the patch.

Medium-risk example:

```text
selectRate returned rates[0]
rates[1].amount was string "3"
the current rate is stored at index 1
```

Why it is medium risk:

- the trace narrows the repair location;
- the model still has to edit the function or access correctly;
- the trace is useful debugging evidence, but close to a hint.

### `high`

The trace nearly states the fix or contains the corrected expression.

High-risk example:

```text
expected calculation should use account.billing.current instead of
account.billing.previous
```

Why it is high risk:

- it may be testing whether the model copies a hint, not whether provenance
  helps debugging;
- high-risk tasks should be used carefully or excluded from primary
  value-provenance claims.

## Evidence-Type Boundary

Not all evidence is value provenance.

Use:

```text
evidence_type: value_provenance
```

when the trace explains where values came from.

Use:

```text
evidence_type: branch_behavior
```

when the evidence explains branch outcomes or condition behavior.

Use:

```text
evidence_type: final_value_history
```

when the evidence explains how the final output value changed over time, such
as an accumulator being overwritten.

This boundary prevents branch-behavior results from being overinterpreted as
pure value-provenance evidence.

## Audit Questions

For each task, ask:

1. Does the trace identify a bad value, a bad control path, or a final-output
   history?
2. Does it name the exact source expression that should be edited?
3. Does it include the corrected expression?
4. Could a model copy the trace into a patch without reasoning?
5. Are there multiple reasonable repairs, or only one obvious edit?

## Current Rule

For primary value-provenance claims, prefer:

```text
evidence_type: value_provenance
trace_leakage_risk: low or medium
```

Use high-risk tasks only for developmental analysis or label them separately.
