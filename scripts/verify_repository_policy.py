#!/usr/bin/env python3
"""Validate the repository-owned desired governance policy without claiming remote enforcement."""

from __future__ import annotations

import json
from pathlib import Path
import sys


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    policy_path = root / ".engineering" / "repository-policy.json"
    workflow_path = root / ".github" / "workflows" / "repository-health.yml"
    validate_path = root / ".github" / "workflows" / "validate.yml"
    errors: list[str] = []

    try:
        policy = json.loads(policy_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"Repository governance policy check\nFAIL: cannot read policy: {exc}")
        return 1

    if policy.get("schema_version") != 1:
        errors.append("schema_version must be 1")
    if policy.get("integration_branch") != "dev":
        errors.append("integration_branch must be dev")
    if policy.get("release_branch") != "main":
        errors.append("release_branch must be main")
    if policy.get("default_branch_target") != "dev":
        errors.append("default_branch_target must be dev")

    pull_requests = policy.get("pull_requests") or {}
    for key in (
        "required_for_integration",
        "draft_by_default",
        "delete_head_branch_after_merge",
        "require_conversation_resolution",
    ):
        if pull_requests.get(key) is not True:
            errors.append(f"pull_requests.{key} must be true")
    for key in ("allow_force_push", "allow_deletion"):
        if pull_requests.get(key) is not False:
            errors.append(f"pull_requests.{key} must be false")

    merge_policy = policy.get("merge_policy") or {}
    if merge_policy.get("preferred_method") != "squash":
        errors.append("merge_policy.preferred_method must be squash")
    if merge_policy.get("require_up_to_date_before_merge") is not True:
        errors.append("merge_policy.require_up_to_date_before_merge must be true")
    if merge_policy.get("direct_push") is not False:
        errors.append("merge_policy.direct_push must be false")

    expected_checks = {"Validate / android", "Repository health / engineering-baseline"}
    required_checks = policy.get("required_checks") or {}
    for branch in ("dev", "main"):
        actual = set(required_checks.get(branch) or [])
        missing = sorted(expected_checks - actual)
        if missing:
            errors.append(f"required_checks.{branch} missing: {', '.join(missing)}")

    evidence = policy.get("evidence") or {}
    if evidence.get("remote_settings_must_be_verified") is not True:
        errors.append("evidence.remote_settings_must_be_verified must be true")
    if evidence.get("repository_policy_file_is_desired_state_not_remote_proof") is not True:
        errors.append("evidence.repository_policy_file_is_desired_state_not_remote_proof must be true")

    for required_path in (workflow_path, validate_path):
        if not required_path.is_file():
            errors.append(f"missing workflow: {required_path.relative_to(root)}")

    print("Repository governance policy check")
    print(f"root: {root}")
    for error in errors:
        print(f"FAIL: {error}")
    if errors:
        print(f"RESULT: FAIL ({len(errors)} error(s))")
        return 1

    print("RESULT: PASS")
    print("NOTE: this validates desired repository policy only; live GitHub branch protection remains external evidence.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
