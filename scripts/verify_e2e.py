#!/usr/bin/env python3
"""0.9.2 E2E-contract verifier preserving the adopted 0.9.1 checks."""
from __future__ import annotations
import argparse, json, subprocess, sys, tempfile
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(); parser.add_argument("--root", default="."); parser.add_argument("--template-mode", action="store_true")
    args = parser.parse_args(); root = Path(args.root).resolve(); e2e_path = root / ".engineering" / "e2e.json"; commands_path = root / ".engineering" / "commands.json"
    try: e2e = json.loads(e2e_path.read_text(encoding="utf-8")); commands = json.loads(commands_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"FAIL: invalid engineering JSON: {exc}"); return 1
    if e2e.get("contract_version") != "0.2.1":
        print("FAIL: e2e contract_version must be 0.2.1"); return 1
    with tempfile.TemporaryDirectory() as td:
        tmp = Path(td); (tmp / ".engineering").mkdir(parents=True)
        legacy = dict(e2e); legacy["contract_version"] = "0.2.0"
        (tmp / ".engineering" / "e2e.json").write_text(json.dumps(legacy, indent=2) + "\n", encoding="utf-8")
        (tmp / ".engineering" / "commands.json").write_text(json.dumps(commands, indent=2) + "\n", encoding="utf-8")
        command = [sys.executable, str(root / "scripts" / "verify_e2e_legacy.py"), "--root", str(tmp)]
        if args.template_mode: command.append("--template-mode")
        result = subprocess.run(command, check=False)
        if result.returncode: return result.returncode
    print("0.9.2 E2E contract version: PASS")
    return 0

if __name__ == "__main__": sys.exit(main())
