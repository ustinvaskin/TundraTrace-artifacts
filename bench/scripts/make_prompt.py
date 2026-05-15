#!/usr/bin/env python3

import argparse
import json
from pathlib import Path


def format_value(value, value_type):
    if value_type == "String":
        return json.dumps(value)
    return value


def format_runtime_error(runtime_error):
    if runtime_error is None:
        return "The program runs but produces the wrong output."

    lines = [
        f"{runtime_error['type']}: {runtime_error['message']}",
    ]

    if "expression" in runtime_error:
        lines.append(f"Failing expression: {runtime_error['expression']}")

    if "left_value" in runtime_error and "left_type" in runtime_error:
        lines.append(
            "Left value: "
            + format_value(runtime_error["left_value"], runtime_error["left_type"])
            + f" : {runtime_error['left_type']}"
        )

    if "right_value" in runtime_error and "right_type" in runtime_error:
        lines.append(
            "Right value: "
            + format_value(runtime_error["right_value"], runtime_error["right_type"])
            + f" : {runtime_error['right_type']}"
        )

    if "index_value" in runtime_error and "index_type" in runtime_error:
        lines.append(
            "Index value: "
            + format_value(runtime_error["index_value"], runtime_error["index_type"])
            + f" : {runtime_error['index_type']}"
        )

    return "\n".join(lines)


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


def load_template(root, condition, prompt_version):
    template_path = root / "bench" / "prompts" / prompt_version / f"{condition}.txt"
    if not template_path.exists():
        raise SystemExit(f"Unknown condition or prompt version: {prompt_version}/{condition}")

    return template_path.read_text(encoding="utf-8")


def make_prompt(root, task_id, condition, task_set="seed", prompt_version="v1"):
    task = load_task(root, task_id, task_set)
    template = load_template(root, condition, prompt_version)

    provenance = "\n".join(f"- {line}" for line in task["provenance_compact"])

    return template.format(
        program=task["program"],
        runtime_error=format_runtime_error(task["runtime_error"]),
        provenance_compact=provenance,
        expected_output=task["expected_output"],
    )


def main():
    parser = argparse.ArgumentParser(description="Generate a TundraBench prompt.")
    parser.add_argument(
        "task_id",
        help="Task id, matching a file in bench/tasks/<task-set> without .json",
    )
    parser.add_argument(
        "condition",
        choices=["error-only", "provenance-assisted"],
        help="Prompt condition to generate",
    )
    parser.add_argument("--task-set", default="seed", help="Task set under bench/tasks")
    parser.add_argument("--prompt-version", default="v1", help="Prompt version under bench/prompts")
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[2]
    print(make_prompt(root, args.task_id, args.condition, args.task_set, args.prompt_version))


if __name__ == "__main__":
    main()
