# Failure Taxonomy

Date: 2026-05-14

## Purpose

This taxonomy defines the main failure modes TundraBench should track.

The core idea is:

```text
repair failure is not one thing
```

A model can understand the bug but fail the output contract. It can produce
valid Tundra but fail the tests. It can pass the visible test while making a
shallow constant patch. These should be measured separately.

## Scoring Layers

```text
strict scoring      = output protocol compliance
normalized scoring  = recoverable repair content
multi-test scoring  = semantic robustness / overfit resistance
qualitative audit   = explanation of suspicious outcomes
```

## Primary Failure Classes

### `protocol_failure`

The response contains a repair, but it is wrapped in non-executable text.

Examples:

- Markdown fence;
- heading;
- "Here is the fixed program";
- prose before or after code.

Typical scoring pattern:

```text
strict: fail
normalized: pass or different failure
```

### `syntax_failure`

The repair is invalid Tundra after any allowed normalization.

Examples:

- missing semicolon;
- JavaScript/Python syntax;
- unsupported assignment shape;
- invalid record syntax.

### `foreign_language_repair`

The model repairs the bug conceptually but uses another language's idioms.

Examples:

- `parseInt(...)`;
- `Number(...)`;
- `int(...)`;
- JavaScript object syntax that is not Tundra-compatible.

This is a subtype or note under `syntax_failure`, but it is important enough to
track because Tundra is intentionally unfamiliar.

### `runtime_failure_after_patch`

The patch parses, but the repaired program still crashes at runtime.

Examples:

- `parseNumber(5)` where `5` is already a number;
- accessing a missing field;
- indexing outside a list.

### `partial_fix_with_unrelated_deletion`

The model makes a relevant repair but accidentally deletes or rewrites another
needed part of the program.

Example:

- adds `parseNumber(raw.invoice.amount)` in the correct helper;
- deletes a still-needed helper function;
- leaves a call to the deleted helper, causing a runtime error.

This is useful because it shows partial bug understanding, even though the
final patch fails.

### `comment_induced_syntax_failure`

The model makes an otherwise plausible repair but adds or mangles a comment in
a way that breaks the Tundra parser.

This matters because prompt `v2` already asks models not to include comments
unless they already appear in the program. If this failure mode persists, it is
evidence that output discipline is still model-sensitive.

### `over_repair_representation_change`

The model changes the representation of a value in one place but does not update
dependent uses consistently.

Example:

- changes a helper to return parsed numbers instead of strings;
- later still calls `parseNumber(...)` on those already-parsed numbers;
- the repaired program crashes at runtime.

This is different from simply ignoring the trace. It suggests the model
understood the local data issue but failed to preserve the program-wide value
contract.

### `wrong_output`

The patch parses and runs, but the output does not match the expected result.

This can happen when the model:

- fixes the wrong line;
- preserves the original bug;
- changes a nearby value without fixing the data flow;
- misunderstands expected behavior.

### `test_setup_error`

The scorer could not apply a variant test replacement.

This is not just a nuisance. It can indicate that a model rewrote protected
input data or changed the source shape so heavily that benchmark variants no
longer apply.

### `overfit_visible_case`

The repair passes the visible/default case but fails nearby variants.

Examples:

- hardcoded expected output;
- changed only the input constant;
- swapped labels rather than fixing the condition;
- removed logic and printed the expected value.

### `constant_patch`

The repair changes data constants instead of fixing program logic.

This is especially important for wrong-output tasks and branch-behavior tasks.

### `removed_logic`

The repair deletes or bypasses the original computation rather than fixing it.

Example:

```tundra
print(42);
```

instead of repairing the value flow that should produce `42`.

### `clean_semantic_fix`

The repair appears to fix the underlying bug in a task-specific way.

Examples:

- converts the value that provenance identifies as a string before arithmetic;
- changes a wrong field access to the intended field;
- changes a wrong index to the intended index;
- fixes an accumulator update from replacement to addition;
- swaps function arguments into the intended order.

Use this label carefully. It means "clean under the audit rule," not formal
proof of semantic equivalence.

### `unclear_requires_manual_review`

The repair passes or partly passes, but the automatic/task-specific audit
cannot confidently classify the change.

Use this when a model rewrites the program heavily.

## Trace-Use Labels

Trace-use labels should stay conservative:

- `not_applicable`
- `not_observed`
- `used_evidence`
- `ignored_evidence`
- `misused_evidence`
- `evidence_not_needed`
- `unclear`
- `trace_use_possible`
- `trace_misread_possible`
- `trace_use_confirmed`

For program-only outputs, prefer `trace_use_possible` at most. Use
`trace_use_confirmed` only when the model explicitly explains its reasoning or
the experiment asks for an explanation.

For qualitative evidence-use audits, prefer the more descriptive labels:

- `used_evidence`: repair is consistent with following the runtime evidence;
- `ignored_evidence`: repair does not address the value or behavior identified
  by evidence;
- `misused_evidence`: repair appears to over-focus on or misapply evidence;
- `evidence_not_needed`: the task was solved without evidence or evidence does
  not appear necessary;
- `unclear`: output is too ambiguous to classify.

## Why This Matters

Without this taxonomy, a single pass/fail score hides too much:

- A strict failure may contain a correct repair wrapped in Markdown.
- A normalized pass may still be unusable in a direct execution pipeline.
- A visible-test pass may be an overfit patch.
- A wrong-output failure may still show correct localization.

The study should report repair behavior as a profile, not a single number.
