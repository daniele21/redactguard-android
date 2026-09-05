#!/usr/bin/env python3
"""0.9.2 operating-contract verifier preserving the adopted 0.9.1 checks."""
from __future__ import annotations
import argparse, json, subprocess, sys, tempfile
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(); parser.add_argument("--root", default="."); parser.add_argument("--template-mode", action="store_true")
    args = parser.parse_args(); root = Path(args.root).resolve(); path = root / ".engineering" / "commands.json"
    try: data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"FAIL: invalid .engineering/commands.json: {exc}"); return 1
    if data.get("contract_version") != "0.6.1":
        print("FAIL: contract_version must be 0.6.1"); return 1
    with tempfile.TemporaryDirectory() as td:
        tmp = Path(td); (tmp / ".engineering").mkdir(parents=True)
        legacy = dict(data); legacy["contract_version"] = "0.6.0"
        (tmp / ".engineering" / "commands.json").write_text(json.dumps(legacy, indent=2) + "\n", encoding="utf-8")
        command = [sys.executable, str(root / "scripts" / "verify_operations_legacy.py"), "--root", str(tmp)]
        if args.template_mode: command.append("--template-mode")
        result = subprocess.run(command, check=False)
        if result.returncode: return result.returncode
    print("0.9.2 operating contract version: PASS")
    return 0

if __name__ == "__main__": sys.exit(main())
