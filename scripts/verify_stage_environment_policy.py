#!/usr/bin/env python3
"""Verify that real-environment evidence is release-gated, not integration-gated."""
from __future__ import annotations
import argparse, json, sys
from pathlib import Path

def load(path: Path, errors: list[str]) -> dict:
    try: value=json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc: errors.append(f"invalid JSON {path}: {exc}"); return {}
    return value if isinstance(value, dict) else {}

def expect(section: dict, key: str, value: object, errors: list[str], prefix: str) -> None:
    if section.get(key) != value: errors.append(f"{prefix}.{key} must be {value!r}")

def main() -> int:
    parser=argparse.ArgumentParser(); parser.add_argument("--root", default="."); parser.add_argument("--template-mode", action="store_true"); args=parser.parse_args()
    root=Path(args.root).resolve(); errors=[]; commands=load(root/".engineering"/"commands.json", errors); e2e=load(root/".engineering"/"e2e.json", errors)
    velocity=commands.get("development_velocity", {}); integration=velocity.get("integration", {}); release=velocity.get("release", {})
    expect(integration,"automated_e2e_required_when_affected",True,errors,"development_velocity.integration")
    expect(integration,"real_environment_blocking",False,errors,"development_velocity.integration")
    expect(integration,"real_environment_deferred_to_release",True,errors,"development_velocity.integration")
    expect(release,"required_real_environment_blocking",True,errors,"development_velocity.release")
    policy=e2e.get("stage_policy", {}); integ=policy.get("integration", {}); rel=policy.get("release", {})
    expect(integ,"automated_e2e_before_shared_integration",True,errors,"stage_policy.integration")
    expect(integ,"real_environment_blocking",False,errors,"stage_policy.integration")
    expect(integ,"real_environment_deferred_to_release",True,errors,"stage_policy.integration")
    expect(integ,"material_ui_journey_minimum_evidence_mode","full_media",errors,"stage_policy.integration")
    expect(integ,"incidental_ui_may_use_assertions",True,errors,"stage_policy.integration")
    expect(rel,"full_validation_required",True,errors,"stage_policy.release")
    expect(rel,"release_critical_e2e_required",True,errors,"stage_policy.release")
    expect(rel,"required_real_environment_blocking",True,errors,"stage_policy.release")
    if "material_ui_integration_outcome" not in set(e2e.get("ui_evidence",{}).get("full_media_triggers") or []): errors.append("ui_evidence.full_media_triggers must include material_ui_integration_outcome")
    print("Stage environment policy check"); print(f"root: {root}")
    for error in errors: print(f"FAIL: {error}")
    if errors: print(f"RESULT: FAIL ({len(errors)} error(s))"); return 1
    print("RESULT: PASS"); return 0

if __name__ == "__main__": sys.exit(main())
