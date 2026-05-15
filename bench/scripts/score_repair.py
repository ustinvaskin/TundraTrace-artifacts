#!/usr/bin/env python3

import argparse
import json
import re
import subprocess
import tempfile
from pathlib import Path


FENCE_RE = re.compile(r"```(?:[A-Za-z0-9_.+-]+)?\s*\n(.*?)\n```", re.DOTALL)
CODE_START_RE = re.compile(r"^\s*(val|var|def|if|while|for|return|print)\b")


def normalize_repair_text(text):
    text = text.strip()

    fence_match = FENCE_RE.search(text)
    if fence_match:
        return fence_match.group(1).strip() + "\n"

    lines = text.splitlines()
    start = 0
    while start < len(lines) and not CODE_START_RE.match(lines[start]):
        start += 1

    if start == len(lines):
        return text + "\n"

    end = len(lines)
    while end > start:
        line = lines[end - 1].strip()
        if line.endswith(";") or line.endswith("}") or line.startswith("}"):
            break
        end -= 1

    return "\n".join(lines[start:end]).strip() + "\n"


def task_set_dir(root, task_set):
    return root / "bench" / "tasks" / task_set


def load_task(root, task_id, task_set):
    tasks_dir = task_set_dir(root, task_set)
    task_path = tasks_dir / f"{task_id}.json"
    if not task_path.exists():
        available = sorted(path.stem for path in tasks_dir.glob("*.json"))
        raise SystemExit(
            f"Unknown task id in task set '{task_set}': {task_id}\n"
            f"Available tasks:\n- " + "\n- ".join(available)
        )

    return json.loads(task_path.read_text(encoding="utf-8"))


def classify_result(returncode, actual_output, expected_output):
    if returncode == 65:
        return "syntax_error"

    if returncode == 70:
        return "runtime_error_after_patch"

    if returncode != 0:
        return "execution_error"

    if actual_output != expected_output:
        return "wrong_output"

    return "passed"


def run_tundra(root, repair_path):
    return subprocess.run(
        ["java", "-cp", str(root / "out"), "tundra.Tundra", str(repair_path)],
        cwd=root,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )


def repair_text(repair_path, normalize):
    text = repair_path.read_text(encoding="utf-8")
    if normalize:
        return normalize_repair_text(text)
    return text


def test_name(test, index):
    return test.get("name") or f"test_{index + 1}"


def apply_replacements(text, replacements):
    updated = text
    for replacement in replacements:
        old = replacement["old"]
        new = replacement["new"]
        count = updated.count(old)
        if count != 1:
            raise ValueError(
                f"replacement expected exactly one match for {old!r}, found {count}"
            )
        updated = updated.replace(old, new, 1)
    return updated


def run_test(root, base_text, test, index):
    expected_output = test["expected_output"].strip()
    name = test_name(test, index)

    try:
        test_text = apply_replacements(base_text, test.get("replacements", []))
    except (KeyError, ValueError) as error:
        return {
            "name": name,
            "failure_label": "test_setup_error",
            "repair_success": False,
            "matched_expected_output": False,
            "parse_valid": True,
            "runtime_success": False,
            "expected_output": expected_output,
            "actual_output": "",
            "stderr": str(error),
            "exit_code": "",
        }

    with tempfile.NamedTemporaryFile("w", suffix=".tds", delete=False) as handle:
        handle.write(test_text)
        temp_path = Path(handle.name)

    try:
        result = run_tundra(root, temp_path)
    finally:
        temp_path.unlink(missing_ok=True)

    actual_output = result.stdout.strip()
    failure_label = classify_result(result.returncode, actual_output, expected_output)

    return {
        "name": name,
        "failure_label": failure_label,
        "repair_success": failure_label == "passed",
        "matched_expected_output": actual_output == expected_output,
        "parse_valid": result.returncode != 65,
        "runtime_success": result.returncode == 0,
        "expected_output": expected_output,
        "actual_output": actual_output,
        "stderr": result.stderr.strip(),
        "exit_code": result.returncode,
    }


def task_tests(task):
    return task.get("tests") or [
        {
            "name": "default",
            "expected_output": task["expected_output"],
        }
    ]


def score_repair(root, task_id, repair_path, task_set="seed", normalize=False):
    task = load_task(root, task_id, task_set)
    scoring_mode = "normalized" if normalize else "strict"
    base_text = repair_text(repair_path, normalize)
    test_results = [
        run_test(root, base_text, test, index)
        for index, test in enumerate(task_tests(task))
    ]

    first_failure = next(
        (result for result in test_results if not result["repair_success"]),
        None,
    )
    representative = first_failure or test_results[0]
    repair_success = first_failure is None

    return {
        "task_id": task["id"],
        "task_set": task_set,
        "failure_kind": task["failure_kind"],
        "bug_type": task["bug_type"],
        "evidence_type": task.get("evidence_type", "unspecified"),
        "trace_leakage_risk": task.get("trace_leakage_risk", "unspecified"),
        "difficulty": task["difficulty"],
        "scoring_mode": scoring_mode,
        "repair_path": str(repair_path),
        "parse_valid": all(result["parse_valid"] for result in test_results),
        "runtime_success": all(result["runtime_success"] for result in test_results),
        "repair_success": repair_success,
        "matched_expected_output": all(
            result["matched_expected_output"] for result in test_results
        ),
        "failure_label": representative["failure_label"],
        "expected_output": representative["expected_output"],
        "actual_output": representative["actual_output"],
        "stderr": representative["stderr"],
        "exit_code": representative["exit_code"],
        "tests_passed": sum(1 for result in test_results if result["repair_success"]),
        "tests_total": len(test_results),
        "failed_test": "" if repair_success else representative["name"],
        "test_results": test_results,
    }


def print_text(score):
    print(f"task: {score['task_id']}")
    print(f"task_set: {score['task_set']}")
    print(f"scoring_mode: {score['scoring_mode']}")
    print(f"failure_kind: {score['failure_kind']}")
    print(f"bug_type: {score['bug_type']}")
    print(f"evidence_type: {score['evidence_type']}")
    print(f"trace_leakage_risk: {score['trace_leakage_risk']}")
    print(f"difficulty: {score['difficulty']}")
    print(f"repair_path: {score['repair_path']}")
    print(f"parse_valid: {str(score['parse_valid']).lower()}")
    print(f"runtime_success: {str(score['runtime_success']).lower()}")
    print(f"repair_success: {str(score['repair_success']).lower()}")
    print(f"matched_expected_output: {str(score['matched_expected_output']).lower()}")
    print(f"failure_label: {score['failure_label']}")
    print(f"tests_passed: {score['tests_passed']} / {score['tests_total']}")
    if score["failed_test"]:
        print(f"failed_test: {score['failed_test']}")
    print(f"expected_output: {json.dumps(score['expected_output'])}")
    print(f"actual_output: {json.dumps(score['actual_output'])}")
    print(f"exit_code: {score['exit_code']}")

    if score["stderr"]:
        print()
        print("stderr:")
        print(score["stderr"])


def main():
    parser = argparse.ArgumentParser(description="Score a repaired TundraBench program.")
    parser.add_argument(
        "task_id",
        help="Task id, matching a file in bench/tasks/<task-set> without .json",
    )
    parser.add_argument("repair_path", help="Path to the repaired .tds file")
    parser.add_argument("--task-set", default="seed", help="Task set under bench/tasks")
    parser.add_argument(
        "--normalize",
        action="store_true",
        help="Strip Markdown fences and obvious prose before scoring",
    )
    parser.add_argument("--json", action="store_true", help="Print machine-readable JSON")
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[2]
    repair_path = Path(args.repair_path)
    if not repair_path.exists():
        raise SystemExit(f"Repair file does not exist: {repair_path}")

    score = score_repair(root, args.task_id, repair_path, args.task_set, args.normalize)

    if args.json:
        print(json.dumps(score, indent=2))
    else:
        print_text(score)


if __name__ == "__main__":
    main()
