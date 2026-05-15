#!/usr/bin/env python3

import argparse
import csv
import json
from collections import Counter, defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def read_rows(path):
    if path.is_dir():
        combined = path / "combined-results.csv"
        if combined.exists():
            path = combined
        else:
            rows = []
            for results_path in sorted(path.glob("*/results.csv")):
                rows.extend(read_rows(results_path))
            return rows

    if path.suffix == ".jsonl":
        return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]

    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def task_metadata(task_set, task_id):
    task_path = ROOT / "bench" / "tasks" / task_set / f"{task_id}.json"
    if not task_path.exists():
        return {}

    task = json.loads(task_path.read_text(encoding="utf-8"))
    return {
        "failure_kind": task.get("failure_kind", "unknown"),
        "bug_type": task.get("bug_type", "unknown"),
        "evidence_type": task.get("evidence_type", "unspecified"),
        "trace_leakage_risk": task.get("trace_leakage_risk", "unspecified"),
        "difficulty": task.get("difficulty", "unknown"),
    }


def enrich_rows(rows):
    metadata_cache = {}
    enriched = []

    for row in rows:
        updated = dict(row)
        task_set = updated.get("task_set") or "seed"
        task_id = updated.get("task_id")
        cache_key = (task_set, task_id)

        if task_id and cache_key not in metadata_cache:
            metadata_cache[cache_key] = task_metadata(task_set, task_id)

        metadata = metadata_cache.get(cache_key, {})
        for key, value in metadata.items():
            if not updated.get(key):
                updated[key] = value

        enriched.append(updated)

    return enriched


def as_bool(value):
    if isinstance(value, bool):
        return value
    return str(value).lower() == "true"


def summarize(rows):
    rows = enrich_rows(rows)

    by_model = defaultdict(list)
    by_condition = defaultdict(list)
    by_scoring_mode = defaultdict(list)
    by_trial = defaultdict(list)
    by_task = defaultdict(list)
    by_failure_kind = defaultdict(list)
    by_bug_type = defaultdict(list)
    by_evidence_type = defaultdict(list)
    by_trace_leakage_risk = defaultdict(list)
    by_difficulty = defaultdict(list)
    by_model_bug_type = defaultdict(list)
    by_model_evidence_type = defaultdict(list)
    by_condition_evidence_type = defaultdict(list)
    by_scoring_mode_evidence_type = defaultdict(list)
    by_model_difficulty = defaultdict(list)

    for row in rows:
        by_model[row["model"]].append(row)
        by_condition[row["condition"]].append(row)
        by_scoring_mode[row.get("scoring_mode", "strict")].append(row)
        by_trial[row.get("trial", "1")].append(row)
        by_task[row["task_id"]].append(row)
        by_failure_kind[row.get("failure_kind", "unknown")].append(row)
        by_bug_type[row.get("bug_type", "unknown")].append(row)
        by_evidence_type[row.get("evidence_type", "unknown")].append(row)
        by_trace_leakage_risk[row.get("trace_leakage_risk", "unknown")].append(row)
        by_difficulty[row.get("difficulty", "unknown")].append(row)
        by_model_bug_type[(row["model"], row.get("bug_type", "unknown"))].append(row)
        by_model_evidence_type[
            (row["model"], row.get("evidence_type", "unknown"))
        ].append(row)
        by_condition_evidence_type[
            (row["condition"], row.get("evidence_type", "unknown"))
        ].append(row)
        by_scoring_mode_evidence_type[
            (row.get("scoring_mode", "strict"), row.get("evidence_type", "unknown"))
        ].append(row)
        by_model_difficulty[(row["model"], row.get("difficulty", "unknown"))].append(row)

    def bucket_summary(groups):
        return {
            key: {
                "total": len(value),
                "passed": sum(1 for row in value if as_bool(row["repair_success"])),
                "failure_labels": Counter(row["failure_label"] for row in value),
            }
            for key, value in sorted(groups.items())
        }

    def tuple_bucket_summary(groups):
        return {
            " / ".join(key): {
                "total": len(value),
                "passed": sum(1 for row in value if as_bool(row["repair_success"])),
                "failure_labels": Counter(row["failure_label"] for row in value),
            }
            for key, value in sorted(groups.items())
        }

    return {
        "total_runs": len(rows),
        "passed": sum(1 for row in rows if as_bool(row["repair_success"])),
        "failure_labels": Counter(row["failure_label"] for row in rows),
        "by_model": bucket_summary(by_model),
        "by_condition": bucket_summary(by_condition),
        "by_scoring_mode": bucket_summary(by_scoring_mode),
        "by_trial": bucket_summary(by_trial),
        "by_failure_kind": bucket_summary(by_failure_kind),
        "by_bug_type": bucket_summary(by_bug_type),
        "by_evidence_type": bucket_summary(by_evidence_type),
        "by_trace_leakage_risk": bucket_summary(by_trace_leakage_risk),
        "by_difficulty": bucket_summary(by_difficulty),
        "by_model_bug_type": tuple_bucket_summary(by_model_bug_type),
        "by_model_evidence_type": tuple_bucket_summary(by_model_evidence_type),
        "by_condition_evidence_type": tuple_bucket_summary(
            by_condition_evidence_type
        ),
        "by_scoring_mode_evidence_type": tuple_bucket_summary(
            by_scoring_mode_evidence_type
        ),
        "by_model_difficulty": tuple_bucket_summary(by_model_difficulty),
        "by_task": {
            key: [
                (
                    row["condition"],
                    row["model"],
                    row.get("scoring_mode", "strict"),
                    row["failure_label"],
                )
                for row in value
            ]
            for key, value in sorted(by_task.items())
        },
    }


def print_text(summary):
    print(f"total_runs: {summary['total_runs']}")
    print(f"passed: {summary['passed']}")
    print(f"failure_labels: {dict(summary['failure_labels'])}")

    print()
    print("by_model:")
    for model, data in summary["by_model"].items():
        print(f"- {model}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_condition:")
    for condition, data in summary["by_condition"].items():
        print(f"- {condition}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_scoring_mode:")
    for scoring_mode, data in summary["by_scoring_mode"].items():
        print(f"- {scoring_mode}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_trial:")
    for trial, data in summary["by_trial"].items():
        print(f"- {trial}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_failure_kind:")
    for failure_kind, data in summary["by_failure_kind"].items():
        print(f"- {failure_kind}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_bug_type:")
    for bug_type, data in summary["by_bug_type"].items():
        print(f"- {bug_type}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_evidence_type:")
    for evidence_type, data in summary["by_evidence_type"].items():
        print(f"- {evidence_type}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_trace_leakage_risk:")
    for risk, data in summary["by_trace_leakage_risk"].items():
        print(f"- {risk}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_difficulty:")
    for difficulty, data in summary["by_difficulty"].items():
        print(f"- {difficulty}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_model_bug_type:")
    for key, data in summary["by_model_bug_type"].items():
        print(f"- {key}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_model_evidence_type:")
    for key, data in summary["by_model_evidence_type"].items():
        print(f"- {key}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_condition_evidence_type:")
    for key, data in summary["by_condition_evidence_type"].items():
        print(f"- {key}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_scoring_mode_evidence_type:")
    for key, data in summary["by_scoring_mode_evidence_type"].items():
        print(f"- {key}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_model_difficulty:")
    for key, data in summary["by_model_difficulty"].items():
        print(f"- {key}: {data['passed']} / {data['total']} passed {dict(data['failure_labels'])}")

    print()
    print("by_task:")
    for task_id, results in summary["by_task"].items():
        print(f"- {task_id}: {results}")


def main():
    parser = argparse.ArgumentParser(description="Summarize TundraBench result CSV/JSONL files.")
    parser.add_argument("path", help="Experiment directory, CSV file, or JSONL file")
    parser.add_argument("--json", action="store_true", help="Print machine-readable JSON")
    args = parser.parse_args()

    rows = read_rows(Path(args.path))
    summary = summarize(rows)

    if args.json:
        print(json.dumps(summary, indent=2))
    else:
        print_text(summary)


if __name__ == "__main__":
    main()
