#!/usr/bin/env python3

import argparse
import csv
import json
import re
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
FENCE_RE = re.compile(r"```")
CODE_START_RE = re.compile(r"^\s*(val|var|def|if|while|for|return|print)\b")
PROSE_RE = re.compile(
    r"\b(this|because|bug|fix|fixed|corrected|output|should|explanation)\b",
    re.IGNORECASE,
)


def read_rows(path):
    if path.is_dir():
        path = path / "combined-results.csv"

    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def as_bool(value):
    if isinstance(value, bool):
        return value
    return str(value).lower() == "true"


def task_metadata(task_set, task_id):
    task_path = ROOT / "bench" / "tasks" / task_set / f"{task_id}.json"
    if not task_path.exists():
        return {}

    task = json.loads(task_path.read_text(encoding="utf-8"))
    return {
        "evidence_type": task.get("evidence_type", "unspecified"),
        "trace_leakage_risk": task.get("trace_leakage_risk", "unspecified"),
    }


def first_nonblank_line(text):
    return next((line.strip() for line in text.splitlines() if line.strip()), "")


def classify_raw_output(text):
    stripped = text.strip()
    first_line = first_nonblank_line(stripped)
    has_fence = bool(FENCE_RE.search(stripped))

    if has_fence:
        before = stripped.split("```", 1)[0].strip()
        after = stripped.rsplit("```", 1)[-1].strip() if stripped.count("```") >= 2 else ""

        if not before and not after:
            return "markdown_fence_only"
        if before and after:
            return "markdown_fence_with_prose"
        if before:
            return "leading_explanation_with_fence"
        return "trailing_explanation_with_fence"

    if not CODE_START_RE.match(first_line):
        if PROSE_RE.search(first_line):
            return "leading_explanation"
        return "non_code_prefix"

    prose_lines = [
        line.strip()
        for line in stripped.splitlines()
        if line.strip()
        and PROSE_RE.search(line)
        and not CODE_START_RE.match(line)
        and not line.strip().startswith(("}", "{"))
    ]
    if prose_lines:
        return "trailing_explanation"

    return "other_recoverable_wrapper"


def paired_rows(rows):
    pairs = {}
    for row in rows:
        key = (
            row["model"],
            row["task_set"],
            row["prompt_version"],
            row["task_id"],
            row["condition"],
            row["trial"],
        )
        pairs.setdefault(key, {})[row["scoring_mode"]] = row
    return pairs


def audit_rows(rows):
    metadata_cache = {}
    audited = []

    for pair in paired_rows(rows).values():
        if "strict" not in pair or "normalized" not in pair:
            continue

        strict = pair["strict"]
        normalized = pair["normalized"]
        if as_bool(strict["repair_success"]) or not as_bool(normalized["repair_success"]):
            continue

        repair_path = Path(strict["repair_path"])
        raw_text = repair_path.read_text(encoding="utf-8")
        task_set = strict["task_set"]
        task_id = strict["task_id"]
        metadata_key = (task_set, task_id)
        if metadata_key not in metadata_cache:
            metadata_cache[metadata_key] = task_metadata(task_set, task_id)

        metadata = metadata_cache[metadata_key]
        audited.append(
            {
                "model": strict["model"],
                "task_set": task_set,
                "prompt_version": strict["prompt_version"],
                "task_id": task_id,
                "condition": strict["condition"],
                "trial": strict["trial"],
                "failure_kind": strict.get("failure_kind", ""),
                "bug_type": strict.get("bug_type", ""),
                "evidence_type": strict.get("evidence_type")
                or metadata.get("evidence_type", "unspecified"),
                "trace_leakage_risk": strict.get("trace_leakage_risk")
                or metadata.get("trace_leakage_risk", "unspecified"),
                "difficulty": strict.get("difficulty", ""),
                "strict_failure_label": strict["failure_label"],
                "normalized_failure_label": normalized["failure_label"],
                "classification": classify_raw_output(raw_text),
                "raw_first_line": first_nonblank_line(raw_text)[:200],
                "strict_repair_path": strict["repair_path"],
                "normalized_repair_path": normalized["repair_path"],
            }
        )

    return audited


def write_csv(path, rows):
    fieldnames = [
        "model",
        "task_set",
        "prompt_version",
        "task_id",
        "condition",
        "trial",
        "failure_kind",
        "bug_type",
        "evidence_type",
        "trace_leakage_risk",
        "difficulty",
        "strict_failure_label",
        "normalized_failure_label",
        "classification",
        "raw_first_line",
        "strict_repair_path",
        "normalized_repair_path",
    ]
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def print_summary(rows):
    print(f"strict_fail_normalized_pass: {len(rows)}")
    for field in [
        "classification",
        "model",
        "evidence_type",
        "bug_type",
        "condition",
        "strict_failure_label",
    ]:
        print()
        print(field + ":")
        for key, count in sorted(Counter(row[field] for row in rows).items()):
            print(f"- {key}: {count}")


def main():
    parser = argparse.ArgumentParser(
        description="Audit strict-fail / normalized-pass TundraBench rows."
    )
    parser.add_argument("path", help="Experiment directory or combined-results.csv")
    parser.add_argument("--output", required=True, help="CSV path to write")
    args = parser.parse_args()

    rows = audit_rows(read_rows(Path(args.path)))
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    write_csv(output, rows)
    print_summary(rows)
    print()
    print(f"wrote: {output}")


if __name__ == "__main__":
    main()
