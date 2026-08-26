#!/usr/bin/env python3
"""Select RedactGuard validation depth from the exact changed-file blast radius."""

from __future__ import annotations

import argparse
import os
import subprocess
import sys
from dataclasses import dataclass, replace
from pathlib import PurePosixPath
from typing import Iterable, Sequence

ZERO_SHA = "0" * 40
PROFILE_RANK = {"lean": 0, "scoped": 1, "strong": 2, "full": 3}

DOC_ONLY_PATHS = {
    ".github/CODEOWNERS",
    ".github/dependabot.yml",
    ".gitattributes",
    ".gitignore",
    "CODE_OF_CONDUCT.md",
    "CONTRIBUTING.md",
    "LICENSE",
    "NOTICE",
    "README.md",
    "SECURITY.md",
}
DOC_ONLY_PREFIXES = ("docs/",)
DOC_ONLY_SUFFIXES = (".md", ".mdx", ".rst")

FORCE_FULL_PATHS = {
    ".engineering/commands.json",
    ".github/workflows/validate.yml",
    ".github/workflows/remote-preflight.yml",
    "scripts/detect_ci_scope.py",
    "scripts/test_detect_ci_scope.py",
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradle.properties",
    "gradle/libs.versions.toml",
    "gradle/wrapper/gradle-wrapper.properties",
}

STRONG_PATHS = {
    "app/build.gradle.kts",
    "app/proguard-rules.pro",
    "app/version.properties",
    "app/src/main/AndroidManifest.xml",
    "scripts/build-redactguard-release.sh",
    "scripts/package-redactguard-artifact.sh",
    "scripts/e2e-redactguard-device.sh",
    "scripts/smoke-redactguard-device.sh",
    "scripts/physical-two-apk-preflight.sh",
}
STRONG_NAME_TOKENS = (
    "sharedruntime",
    "shared_runtime",
    "harness",
    "binder",
    "persistence",
    "repository",
    "redaction",
    "pii",
)

KNOWN_EXECUTABLE_PREFIXES = (
    "app/src/main/kotlin/",
    "app/src/main/res/",
    "app/src/test/",
    "app/src/androidTest/",
    "scripts/",
    "skills/",
    ".engineering/",
)


@dataclass(frozen=True)
class ValidationScope:
    profile: str
    android: bool
    android_test: bool
    release: bool
    reason: str


def normalize_path(path: str) -> str:
    return str(PurePosixPath(path.strip().replace("\\", "/")))


def is_docs_only(path: str) -> bool:
    return path in DOC_ONLY_PATHS or path.startswith(DOC_ONLY_PREFIXES) or path.endswith(DOC_ONLY_SUFFIXES)


def is_known_executable(path: str) -> bool:
    return path.startswith(KNOWN_EXECUTABLE_PREFIXES) or path in STRONG_PATHS or path in FORCE_FULL_PATHS


def is_strong(path: str) -> bool:
    if path in STRONG_PATHS:
        return True
    lowered = path.lower().replace("-", "_")
    return path.startswith("app/src/main/kotlin/") and any(token in lowered for token in STRONG_NAME_TOKENS)


def classify_paths(paths: Iterable[str], *, force_all: bool = False) -> ValidationScope:
    normalized = tuple(sorted({normalize_path(path) for path in paths if path.strip()}))
    if force_all:
        return ValidationScope("full", True, True, True, "explicit full validation")
    if not normalized:
        return ValidationScope("full", True, True, True, "no reliable diff available")
    if any(path in FORCE_FULL_PATHS for path in normalized):
        return ValidationScope("full", True, True, True, "validation selector, global build or dependency inventory changed")

    implementation = tuple(path for path in normalized if not is_docs_only(path))
    if not implementation:
        return ValidationScope("lean", False, False, False, "documentation or repository metadata only")

    unknown = [path for path in implementation if not is_known_executable(path)]
    if unknown:
        return ValidationScope("full", True, True, True, "unknown executable scope: " + ", ".join(unknown[:3]))

    if any(is_strong(path) for path in implementation):
        return ValidationScope("strong", True, True, True, "cross-boundary, privacy/persistence or release-sensitive Android change")

    return ValidationScope("scoped", True, False, False, "contained RedactGuard implementation or test change")


def apply_requested_profile(scope: ValidationScope, requested: str) -> ValidationScope:
    requested = requested.lower()
    if requested == "auto":
        return scope
    if requested not in {"strong", "full"}:
        raise ValueError("requested profile must be auto, strong or full")
    if PROFILE_RANK[requested] <= PROFILE_RANK[scope.profile]:
        return scope
    if requested == "full":
        return ValidationScope("full", True, True, True, f"explicit full override; auto={scope.profile}: {scope.reason}")
    return replace(scope, profile="strong", android=True, android_test=True, release=True, reason=f"explicit strong override; auto={scope.profile}: {scope.reason}")


def ensure_commit_available(sha: str) -> None:
    if not sha or sha == ZERO_SHA:
        raise ValueError("missing or unusable comparison SHA")
    present = subprocess.run(["git", "cat-file", "-e", f"{sha}^{{commit}}"], check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    if present.returncode == 0:
        return
    subprocess.run(["git", "fetch", "--no-tags", "--depth=1", "origin", sha], check=True)


def git_changed_files(base_sha: str, head_sha: str) -> Sequence[str]:
    ensure_commit_available(base_sha)
    ensure_commit_available(head_sha)
    result = subprocess.run(["git", "diff", "--name-only", "--diff-filter=ACMRD", base_sha, head_sha], check=True, capture_output=True, text=True)
    return tuple(line for line in result.stdout.splitlines() if line.strip())


def write_outputs(path: str, scope: ValidationScope) -> None:
    with open(path, "a", encoding="utf-8") as output:
        output.write(f"profile={scope.profile}\n")
        output.write(f"android={'true' if scope.android else 'false'}\n")
        output.write(f"android_test={'true' if scope.android_test else 'false'}\n")
        output.write(f"release={'true' if scope.release else 'false'}\n")
        output.write(f"reason={scope.reason}\n")


def append_step_summary(paths: Sequence[str], scope: ValidationScope) -> None:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return
    with open(summary_path, "a", encoding="utf-8") as summary:
        summary.write("## Validation scope\n\n")
        summary.write(f"- Profile: **{scope.profile.upper()}**\n")
        summary.write(f"- Android: **{scope.android}**\n")
        summary.write(f"- AndroidTest assembly: **{scope.android_test}**\n")
        summary.write(f"- Release/R8: **{scope.release}**\n")
        summary.write(f"- Reason: {scope.reason}\n")
        summary.write(f"- Changed paths considered: {len(paths)}\n")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--event", required=True)
    parser.add_argument("--base-sha", default="")
    parser.add_argument("--head-sha", default="")
    parser.add_argument("--github-output", required=True)
    parser.add_argument("--profile", default="auto", choices=("auto", "strong", "full"))
    parser.add_argument("--force-full", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    paths: Sequence[str] = ()
    try:
        if args.force_full:
            scope = classify_paths((), force_all=True)
        else:
            paths = git_changed_files(args.base_sha, args.head_sha)
            scope = apply_requested_profile(classify_paths(paths), args.profile)
    except (ValueError, subprocess.CalledProcessError) as exc:
        print(f"warning: unable to determine safe validation scope: {exc}", file=sys.stderr)
        scope = classify_paths((), force_all=True)

    write_outputs(args.github_output, scope)
    append_step_summary(paths, scope)
    print(
        "validation scope: "
        f"profile={scope.profile} android={str(scope.android).lower()} "
        f"android_test={str(scope.android_test).lower()} release={str(scope.release).lower()} reason={scope.reason}"
    )
    if paths:
        print("changed paths:")
        for path in paths:
            print(f"  - {path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
