#!/usr/bin/env python3
"""Verify bounded repository instruction context."""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
import sys


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".")
    return parser.parse_args()


def estimate(path: Path, chars_per_token: int) -> int:
    if not path.is_file():
        return 0
    return math.ceil(len(path.read_text(encoding="utf-8")) / chars_per_token)


def main() -> int:
    args = parse_args()
    root = Path(args.root).resolve()
    policy_path = root / ".engineering/documentation-policy.json"
    if not policy_path.is_file():
        print("FAIL: missing .engineering/documentation-policy.json")
        return 1
    policy = json.loads(policy_path.read_text(encoding="utf-8"))
    chars_per_token = int(policy.get("estimated_token_characters", 4))
    targets = policy["context_targets"]

    root_tokens = estimate(root / "AGENTS.md", chars_per_token)
    scoped = [
        (path, estimate(path, chars_per_token))
        for path in root.rglob("AGENTS.md")
        if path != root / "AGENTS.md" and ".git" not in path.parts
    ]
    scoped_path, scoped_tokens = max(scoped, key=lambda item: item[1], default=(None, 0))

    workstream_root = root / "docs/workstreams"
    workstreams = [
        (path, estimate(path, chars_per_token))
        for path in workstream_root.glob("*.md")
        if path.name != "README.md" and not path.name.startswith("_")
    ] if workstream_root.is_dir() else []
    work_path, work_tokens = max(workstreams, key=lambda item: item[1], default=(None, 0))

    bootstrap = root_tokens
    focused = root_tokens + scoped_tokens + work_tokens
    errors: list[str] = []
    if bootstrap > targets["bootstrap_max_estimated_tokens"]:
        errors.append(
            f"bootstrap context ~{bootstrap} > {targets['bootstrap_max_estimated_tokens']} token target"
        )
    if focused > targets["root_scoped_workstream_max_estimated_tokens"]:
        errors.append(
            f"root+largest scoped+largest workstream ~{focused} > "
            f"{targets['root_scoped_workstream_max_estimated_tokens']} token target"
        )

    print("Context health")
    print(f"root AGENTS: ~{root_tokens} tokens")
    if scoped_path:
        print(f"largest scoped AGENTS: ~{scoped_tokens} tokens ({scoped_path.relative_to(root)})")
    else:
        print("largest scoped AGENTS: ~0 tokens")
    if work_path:
        print(f"largest active workstream: ~{work_tokens} tokens ({work_path.relative_to(root)})")
    else:
        print("largest active workstream: ~0 tokens")
    print(f"bootstrap cost: ~{bootstrap} tokens")
    print(f"worst focused routing bundle: ~{focused} tokens")
    for error in errors:
        print(f"FAIL: {error}")
    if errors:
        print(f"RESULT: FAIL ({len(errors)} error(s))")
        return 1
    print("RESULT: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
