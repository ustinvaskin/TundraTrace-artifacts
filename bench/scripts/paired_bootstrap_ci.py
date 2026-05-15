#!/usr/bin/env python3

import argparse
import random
from pathlib import Path

from summarize_results import as_bool, enrich_rows, read_rows


def normalized_pairs(rows):
    normalized = [
        row
        for row in enrich_rows(rows)
        if row.get("scoring_mode", "strict") == "normalized"
    ]
    baseline = {}
    assisted = {}

    for row in normalized:
        key = (row["model"], row["task_id"], row.get("trial", "1"))
        if row["condition"] == "error-only":
            baseline[key] = row
        elif row["condition"] == "provenance-assisted":
            assisted[key] = row

    missing = sorted(set(baseline) ^ set(assisted))
    if missing:
        raise ValueError(f"missing paired normalized attempts: {len(missing)}")

    return [(baseline[key], assisted[key]) for key in sorted(baseline)]


def select_pairs(pairs, evidence_type=None, leakage=None):
    selected = []
    for baseline, assisted in pairs:
        if evidence_type and baseline.get("evidence_type") != evidence_type:
            continue
        if leakage and baseline.get("trace_leakage_risk") != leakage:
            continue
        selected.append((as_bool(baseline["repair_success"]), as_bool(assisted["repair_success"])))
    return selected


def paired_bootstrap(values, resamples, seed):
    if not values:
        raise ValueError("cannot bootstrap an empty slice")

    rng = random.Random(seed)
    size = len(values)
    observed = sum(assisted - baseline for baseline, assisted in values) / size
    samples = []

    for _ in range(resamples):
        delta_sum = 0
        for _ in range(size):
            baseline, assisted = values[rng.randrange(size)]
            delta_sum += assisted - baseline
        samples.append(delta_sum / size)

    samples.sort()
    lower = samples[int(0.025 * resamples)]
    upper = samples[int(0.975 * resamples)]
    return observed, lower, upper


def task_clustered_bootstrap(pairs, resamples, seed, evidence_type=None, leakage=None):
    filtered = []
    for baseline, assisted in pairs:
        if evidence_type and baseline.get("evidence_type") != evidence_type:
            continue
        if leakage and baseline.get("trace_leakage_risk") != leakage:
            continue
        filtered.append((baseline, assisted))

    if not filtered:
        raise ValueError("cannot bootstrap an empty slice")

    by_task = {}
    for baseline, assisted in filtered:
        by_task.setdefault(baseline["task_id"], []).append(
            (as_bool(baseline["repair_success"]), as_bool(assisted["repair_success"]))
        )

    task_ids = sorted(by_task)
    observed_values = [pair for task_id in task_ids for pair in by_task[task_id]]
    observed = sum(assisted - baseline for baseline, assisted in observed_values) / len(
        observed_values
    )

    rng = random.Random(seed)
    samples = []
    for _ in range(resamples):
        sampled_values = []
        for _ in task_ids:
            sampled_task_id = task_ids[rng.randrange(len(task_ids))]
            sampled_values.extend(by_task[sampled_task_id])
        delta = sum(assisted - baseline for baseline, assisted in sampled_values) / len(
            sampled_values
        )
        samples.append(delta)

    samples.sort()
    lower = samples[int(0.025 * resamples)]
    upper = samples[int(0.975 * resamples)]
    return len(observed_values), observed, lower, upper


def main():
    parser = argparse.ArgumentParser(
        description="Compute paired bootstrap CIs for normalized repair-success deltas."
    )
    parser.add_argument("results_path", type=Path)
    parser.add_argument("--resamples", type=int, default=20000)
    parser.add_argument("--seed", type=int, default=20260515)
    args = parser.parse_args()

    pairs = normalized_pairs(read_rows(args.results_path))
    slices = [
        ("all tasks", {}),
        ("value provenance", {"evidence_type": "value_provenance"}),
        (
            "low-leakage value provenance",
            {"evidence_type": "value_provenance", "leakage": "low"},
        ),
        (
            "medium-leakage value provenance",
            {"evidence_type": "value_provenance", "leakage": "medium"},
        ),
    ]

    print(f"resamples: {args.resamples}")
    print(f"seed: {args.seed}")
    print("attempt-level paired bootstrap")
    for label, filters in slices:
        values = select_pairs(pairs, **filters)
        observed, lower, upper = paired_bootstrap(values, args.resamples, args.seed)
        print(
            f"{label}: n={len(values)} "
            f"delta={observed * 100:.1f} pp "
            f"95% CI [{lower * 100:.1f}, {upper * 100:.1f}] pp"
        )

    print("task-clustered paired bootstrap sensitivity")
    for label, filters in slices:
        count, observed, lower, upper = task_clustered_bootstrap(
            pairs, args.resamples, args.seed, **filters
        )
        print(
            f"{label}: n={count} "
            f"delta={observed * 100:.1f} pp "
            f"95% CI [{lower * 100:.1f}, {upper * 100:.1f}] pp"
        )


if __name__ == "__main__":
    main()
