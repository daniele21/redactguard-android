#!/usr/bin/env python3
"""Promote one validated RedactGuard build artifact with identity, checksum and bounded retention."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from datetime import datetime, timezone

KEEP_SUCCESSFUL_PER_LINEAGE = 2
SAFE_BUILD_ID = re.compile(r"^[A-Za-z0-9._-]+$")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact", required=True)
    parser.add_argument("--variant", required=True, choices=("debug", "release-ci", "release"))
    parser.add_argument("--build-id", required=True)
    parser.add_argument("--source-revision", required=True)
    parser.add_argument("--source-dirty", required=True, choices=("true", "false"))
    parser.add_argument("--validation", action="append", default=[])
    return parser.parse_args()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def combined_hash(root: Path, relative_paths: list[str]) -> str:
    digest = hashlib.sha256()
    for rel in sorted(relative_paths):
        path = root / rel
        digest.update(rel.encode("utf-8"))
        digest.update(b"\0")
        if path.is_file():
            digest.update(path.read_bytes())
        else:
            digest.update(b"<missing>")
        digest.update(b"\0")
    return digest.hexdigest()


def load_version(root: Path) -> tuple[str, int]:
    values: dict[str, str] = {}
    for line in (root / "app/version.properties").read_text(encoding="utf-8").splitlines():
        if "=" not in line or line.lstrip().startswith("#"):
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    version_name = values.get("versionName", "")
    version_code_text = values.get("versionCode", "")
    if not version_name or not version_code_text.isdigit():
        raise ValueError("app/version.properties must define versionName and integer versionCode")
    return version_name, int(version_code_text)


def run_git(root: Path, *args: str) -> str | None:
    completed = subprocess.run(
        ["git", *args],
        cwd=root,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        check=False,
    )
    if completed.returncode != 0:
        return None
    return completed.stdout.strip()


def previous_manifest(lineage_dir: Path) -> tuple[Path | None, dict | None]:
    manifests: list[tuple[float, Path]] = []
    if lineage_dir.is_dir():
        for path in lineage_dir.glob("*/manifest.json"):
            try:
                manifests.append((path.stat().st_mtime, path))
            except OSError:
                continue
    if not manifests:
        return None, None
    _, path = max(manifests, key=lambda item: item[0])
    try:
        return path, json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return path, None


def changed_files(root: Path, previous_revision: str | None, current_revision: str) -> list[str]:
    if not previous_revision or previous_revision == current_revision:
        return []
    output = run_git(root, "diff", "--name-only", f"{previous_revision}..{current_revision}")
    if output is None:
        return []
    return [line for line in output.splitlines() if line]


def recover_owned_staging(staging_dir: Path) -> None:
    """Recover only the staging directory owned by this exact build identity.

    Never delete sibling staging directories: another package operation may be using
    a different build ID concurrently. Reusing the same build ID means the caller is
    explicitly taking ownership of that identity's abandoned staging state.
    """
    if staging_dir.is_dir():
        shutil.rmtree(staging_dir)
    elif staging_dir.exists():
        staging_dir.unlink()


def enforce_retention(lineage_dir: Path, keep: int) -> None:
    builds: list[tuple[float, Path]] = []
    for child in lineage_dir.iterdir():
        if not child.is_dir():
            continue
        try:
            builds.append((child.stat().st_mtime, child))
        except OSError:
            continue
    builds.sort(key=lambda item: item[0], reverse=True)
    for _, stale in builds[keep:]:
        shutil.rmtree(stale, ignore_errors=True)


def main() -> int:
    args = parse_args()
    root = Path(__file__).resolve().parents[1]
    source = Path(args.artifact).resolve()
    if not source.is_file():
        print(f"FAIL: artifact not found: {source}", file=sys.stderr)
        return 1
    if not SAFE_BUILD_ID.fullmatch(args.build_id):
        print("FAIL: build ID may contain only letters, digits, dot, underscore and hyphen", file=sys.stderr)
        return 1
    if not re.fullmatch(r"[0-9a-fA-F]{40,64}", args.source_revision):
        print("FAIL: source revision must be a full Git/GitHub revision identity", file=sys.stderr)
        return 1

    version_name, version_code = load_version(root)
    source_dirty = args.source_dirty == "true"
    short_revision = args.source_revision[:12]
    channel = "ci" if os.environ.get("GITHUB_ACTIONS") == "true" else "local"
    extension = source.suffix.lower().lstrip(".") or "bin"
    lineage = args.variant
    artifact_name = (
        f"redactguard-{version_name}-{args.build_id}-{short_revision}-"
        f"android-universal-{lineage}.{extension}"
    )

    artifacts_root = root / "artifacts"
    staging_root = artifacts_root / ".staging"
    lineage_dir = artifacts_root / "success" / lineage
    staging_dir = staging_root / args.build_id
    destination_dir = lineage_dir / args.build_id

    staging_root.mkdir(parents=True, exist_ok=True)
    lineage_dir.mkdir(parents=True, exist_ok=True)
    if destination_dir.exists():
        print(f"FAIL: successful artifact identity already exists: {destination_dir}", file=sys.stderr)
        return 1

    recover_owned_staging(staging_dir)
    staging_dir.mkdir(parents=False, exist_ok=False)

    previous_path, previous = previous_manifest(lineage_dir)
    previous_revision = previous.get("sourceRevision") if isinstance(previous, dict) else None
    built_at = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")

    try:
        staged_artifact = staging_dir / artifact_name
        shutil.copy2(source, staged_artifact)
        artifact_sha = sha256(staged_artifact)
        artifact_bytes = staged_artifact.stat().st_size

        dependency_inputs = [
            "gradle/libs.versions.toml",
            "settings.gradle.kts",
        ]
        toolchain_inputs = [
            "gradle/wrapper/gradle-wrapper.properties",
            "gradle/libs.versions.toml",
        ]
        configuration_inputs = [
            "app/build.gradle.kts",
            "gradle.properties",
            "app/version.properties",
        ]
        source_changes = changed_files(root, previous_revision, args.source_revision)

        manifest = {
            "schemaVersion": 1,
            "project": "daniele21/redactguard-android",
            "product": "RedactGuard",
            "productVersion": version_name,
            "versionCode": version_code,
            "buildId": args.build_id,
            "sourceRevision": args.source_revision,
            "sourceDirty": source_dirty,
            "platform": "android",
            "architecture": "universal",
            "channel": channel,
            "variant": lineage,
            "artifactName": artifact_name,
            "artifactBytes": artifact_bytes,
            "checksum": {"algorithm": "sha256", "value": artifact_sha},
            "builtAtUtc": built_at,
            "validation": args.validation,
        }
        delta = {
            "schemaVersion": 1,
            "compareTo": "previous-successful-comparable-build",
            "previousManifest": str(previous_path.relative_to(root)) if previous_path else None,
            "previousSourceRevision": previous_revision,
            "currentSourceRevision": args.source_revision,
            "dimensions": {
                "source": {"changedFiles": source_changes, "dirty": source_dirty},
                "dependencies": {"inputHash": combined_hash(root, dependency_inputs)},
                "toolchain": {"inputHash": combined_hash(root, toolchain_inputs)},
                "configuration": {"inputHash": combined_hash(root, configuration_inputs)},
                "compatibility_migrations": {
                    "changedFiles": [
                        path
                        for path in source_changes
                        if path.startswith("docs/adr/") or "migration" in path.lower()
                    ]
                },
                "artifact_metrics": {"bytes": artifact_bytes, "sha256": artifact_sha},
                "validation": {"evidence": args.validation},
            },
        }

        (staging_dir / "manifest.json").write_text(
            json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8"
        )
        (staging_dir / "build-delta.json").write_text(
            json.dumps(delta, indent=2, sort_keys=True) + "\n", encoding="utf-8"
        )
        (staging_dir / f"{artifact_name}.sha256").write_text(
            f"{artifact_sha}  {artifact_name}\n", encoding="utf-8"
        )

        os.replace(staging_dir, destination_dir)
    except Exception:
        shutil.rmtree(staging_dir, ignore_errors=True)
        raise

    enforce_retention(lineage_dir, KEEP_SUCCESSFUL_PER_LINEAGE)

    print("Promoted RedactGuard artifact")
    print(f"artifact: {destination_dir / artifact_name}")
    print(f"manifest: {destination_dir / 'manifest.json'}")
    print(f"build delta: {destination_dir / 'build-delta.json'}")
    print(f"sha256: {artifact_sha}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
