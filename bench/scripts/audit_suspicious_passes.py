#!/usr/bin/env python3

import argparse
import csv
import json
import re
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


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
        "failure_kind": task.get("failure_kind", "unknown"),
        "bug_type": task.get("bug_type", "unknown"),
        "evidence_type": task.get("evidence_type", "unspecified"),
        "trace_leakage_risk": task.get("trace_leakage_risk", "unspecified"),
        "difficulty": task.get("difficulty", "unknown"),
        "tests_total": len(task.get("tests", [])) or 1,
        "expected_output": str(task.get("expected_output", "")).strip(),
    }


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


def selected_attempt(pair):
    strict = pair.get("strict")
    normalized = pair.get("normalized")
    row = normalized or strict

    if row.get("failure_kind") != "wrong_output":
        return False

    return (
        as_bool(row.get("repair_success"))
        or row.get("failure_label") == "test_setup_error"
        or (strict and strict.get("failure_label") == "test_setup_error")
    )


def repair_text(row):
    path = Path(row["repair_path"])
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def contains_hardcoded_print(text, expected_output):
    if not expected_output:
        return False
    if re.search(r"\b(if|for|while)\s*\(", text):
        return False

    escaped = re.escape(expected_output)
    return bool(
        re.search(rf"\bprint\s*\(\s*{escaped}\s*\)\s*;", text)
        or re.search(rf'\bprint\s*\(\s*"{escaped}"\s*\)\s*;', text)
    )


def classify_repair(task_id, text, normalized_label, expected_output):
    compact = re.sub(r"\s+", " ", text)

    if normalized_label == "test_setup_error":
        if task_id == "repair_010_wrong_branch_condition" and "val score = 92" in text:
            return "constant_patch", "changed score constant, so variant replacements could not apply"
        if task_id == "repair_017_wrong_branch_nested_record" and "passMark: 70" in text:
            return "constant_patch", "changed passMark constant, so variant replacements could not apply"
        return "invalid_multitest_replacement", "repair changed source shape used by variant replacements"

    if task_id in {
        "repair_008_wrong_index_nested_list",
        "repair_015_wrong_index_function_return",
    }:
        if re.search(r"\[\s*1\s*\]", text):
            return "clean_semantic_fix", "selects the target index"
        return "unclear_requires_manual_review", "passed wrong-index task without obvious target index"

    if task_id in {
        "repair_011_incorrect_accumulator_loop",
        "repair_020_accumulator_nested_records",
    }:
        if re.search(r"total\s*=\s*total\s*\+", compact):
            return "clean_semantic_fix", "uses additive accumulator update"
        return "unclear_requires_manual_review", "passed accumulator task without obvious additive update"

    if task_id == "repair_012_swapped_function_arguments_chain":
        if "applyDiscount(price, discount)" in compact:
            return "clean_semantic_fix", "calls helper with price before discount"
        return "unclear_requires_manual_review", "passed swapped-argument task without obvious corrected call"

    if task_id == "repair_018_swapped_ratio_arguments":
        if "ratio(numerator, denominator)" in compact:
            return "clean_semantic_fix", "calls helper with numerator before denominator"
        return "unclear_requires_manual_review", "passed swapped-argument task without obvious corrected call"

    if task_id == "repair_010_wrong_branch_condition":
        if "val score = 92" in text:
            return "constant_patch", "changed score constant instead of fixing branch"
        if re.search(r"score\s*<\s*threshold", text):
            return "clean_semantic_fix", "flips comparison to classify below-threshold score as review"
        if 'print("review")' in text and 'print("pass")' in text:
            return "unclear_requires_manual_review", "passed branch task with nonstandard branch edit"
        return "unclear_requires_manual_review", "passed branch task without obvious semantic pattern"

    if task_id == "repair_017_wrong_branch_nested_record":
        if "passMark: 70" in text:
            return "constant_patch", "changed passMark constant instead of fixing branch"
        if re.search(r"score\s*<\s*report\.passMark", text):
            return "clean_semantic_fix", "flips nested branch comparison"
        return "unclear_requires_manual_review", "passed nested branch task without obvious semantic pattern"

    if contains_hardcoded_print(text, expected_output):
        return "hardcoded_expected_output", "prints expected output directly"

    return "unclear_requires_manual_review", "no task-specific classifier"


def audit_rows(rows):
    metadata_cache = {}
    audited = []

    for pair in paired_rows(rows).values():
        if not selected_attempt(pair):
            continue

        strict = pair.get("strict")
        normalized = pair.get("normalized")
        row = normalized or strict
        task_set = row["task_set"]
        task_id = row["task_id"]
        cache_key = (task_set, task_id)
        if cache_key not in metadata_cache:
            metadata_cache[cache_key] = task_metadata(task_set, task_id)
        metadata = metadata_cache[cache_key]

        text = repair_text(normalized or strict)
        label, reason = classify_repair(
            task_id,
            text,
            (normalized or {}).get("failure_label", ""),
            metadata.get("expected_output", ""),
        )

        strict_passed = strict is not None and as_bool(strict.get("repair_success"))
        normalized_passed = normalized is not None and as_bool(
            normalized.get("repair_success")
        )
        tests_passed = int((normalized or row).get("tests_passed") or 0)
        tests_total = int((normalized or row).get("tests_total") or 0)

        audited.append(
            {
                "model": row["model"],
                "task_set": task_set,
                "prompt_version": row["prompt_version"],
                "task_id": task_id,
                "condition": row["condition"],
                "trial": row["trial"],
                "scoring_mode": "paired",
                "failure_kind": metadata.get("failure_kind", row.get("failure_kind", "")),
                "bug_type": metadata.get("bug_type", row.get("bug_type", "")),
                "evidence_type": metadata.get(
                    "evidence_type", row.get("evidence_type", "unspecified")
                ),
                "trace_leakage_risk": metadata.get(
                    "trace_leakage_risk",
                    row.get("trace_leakage_risk", "unspecified"),
                ),
                "difficulty": metadata.get("difficulty", row.get("difficulty", "")),
                "strict_passed": strict_passed,
                "normalized_passed": normalized_passed,
                "multi_test_passed": normalized_passed and tests_passed == tests_total,
                "strict_failure_label": "" if strict is None else strict["failure_label"],
                "normalized_failure_label": ""
                if normalized is None
                else normalized["failure_label"],
                "tests_passed": tests_passed,
                "tests_total": tests_total,
                "label": label,
                "short_reason": reason,
                "repair_path": "" if normalized is None else normalized["repair_path"],
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
        "scoring_mode",
        "failure_kind",
        "bug_type",
        "evidence_type",
        "trace_leakage_risk",
        "difficulty",
        "strict_passed",
        "normalized_passed",
        "multi_test_passed",
        "strict_failure_label",
        "normalized_failure_label",
        "tests_passed",
        "tests_total",
        "label",
        "short_reason",
        "repair_path",
    ]
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def print_summary(rows):
    print(f"audited_attempts: {len(rows)}")
    for field in [
        "label",
        "model",
        "bug_type",
        "evidence_type",
        "condition",
        "normalized_failure_label",
    ]:
        print()
        print(field + ":")
        for key, count in sorted(Counter(row[field] for row in rows).items()):
            print(f"- {key}: {count}")


def main():
    parser = argparse.ArgumentParser(
        description="Audit suspicious passes and test-setup errors."
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
