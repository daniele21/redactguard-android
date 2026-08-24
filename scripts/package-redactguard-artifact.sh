#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VARIANT="${1:-}"

usage() {
    cat <<'EOF'
Usage:
  bash scripts/package-redactguard-artifact.sh debug
  bash scripts/package-redactguard-artifact.sh release-ci

`debug` builds a debug APK.
`release-ci` builds the intentionally unsigned/minified release APK used for CI evidence.
Signed release AABs remain owned by scripts/build-redactguard-release.sh.

Every promoted artifact receives:
- a unique build ID distinct from product version;
- full source revision and dirty-state identity;
- an immutable successful output directory;
- SHA-256 checksum;
- machine-readable manifest;
- build delta against the previous successful comparable build;
- bounded local retention (latest two successful builds per variant).
EOF
}

if [[ "${VARIANT}" != "debug" && "${VARIANT}" != "release-ci" ]]; then
    usage >&2
    exit 2
fi

cd "${ROOT_DIR}"

if ! command -v git >/dev/null 2>&1; then
    echo "git is required to produce source identity." >&2
    exit 1
fi
if ! command -v python3 >/dev/null 2>&1; then
    echo "python3 is required for build ID generation and artifact promotion." >&2
    exit 1
fi

SOURCE_REVISION="$(git rev-parse HEAD)"
if [[ ! "${SOURCE_REVISION}" =~ ^[0-9a-fA-F]{40,64}$ ]]; then
    echo "Unable to determine a full source revision." >&2
    exit 1
fi

if [[ -n "$(git status --porcelain --untracked-files=normal)" ]]; then
    SOURCE_DIRTY="true"
else
    SOURCE_DIRTY="false"
fi

if [[ -n "${REDACTGUARD_BUILD_ID:-}" ]]; then
    BUILD_ID="${REDACTGUARD_BUILD_ID}"
else
    BUILD_ID="$(python3 - <<'PY'
from datetime import datetime, timezone
import uuid
stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
print(f"{stamp}-{uuid.uuid4().hex[:12]}")
PY
)"
fi

GRADLE_IDENTITY_ARGS=(
    "-PredactGuardBuildId=${BUILD_ID}"
    "-PredactGuardSourceRevision=${SOURCE_REVISION}"
    "-PredactGuardSourceDirty=${SOURCE_DIRTY}"
)

case "${VARIANT}" in
    debug)
        ./gradlew "${GRADLE_IDENTITY_ARGS[@]}" :app:assembleDebug
        ARTIFACT="${ROOT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
        VALIDATION="./gradlew :app:assembleDebug"
        ;;
    release-ci)
        REDACTGUARD_ALLOW_UNSIGNED_RELEASE=true \
            ./gradlew "${GRADLE_IDENTITY_ARGS[@]}" :app:assembleRelease
        ARTIFACT="$(find "${ROOT_DIR}/app/build/outputs/apk/release" -maxdepth 1 -type f -name '*.apk' | sort | head -n 1)"
        VALIDATION="REDACTGUARD_ALLOW_UNSIGNED_RELEASE=true ./gradlew :app:assembleRelease"
        ;;
esac

if [[ -z "${ARTIFACT:-}" || ! -f "${ARTIFACT}" ]]; then
    echo "Expected build artifact was not produced for ${VARIANT}." >&2
    exit 1
fi

python3 scripts/promote-redactguard-artifact.py \
    --artifact "${ARTIFACT}" \
    --variant "${VARIANT}" \
    --build-id "${BUILD_ID}" \
    --source-revision "${SOURCE_REVISION}" \
    --source-dirty "${SOURCE_DIRTY}" \
    --validation "${VALIDATION}"
