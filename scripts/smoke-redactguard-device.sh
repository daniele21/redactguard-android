#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEVICE=""
APP_APK=""
RELEASE=false
OWNED_INSTALL=false

usage() {
  cat <<'EOF'
Usage: bash scripts/smoke-redactguard-device.sh --device SERIAL --app-apk PATH [--release]

Installs and launches the exact RedactGuard APK on a clean physical/emulated Android target,
verifies the app process starts, force-stops it, then removes only the package installed by
this run. The command refuses to replace an already-installed RedactGuard package.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --app-apk) APP_APK="$2"; shift 2 ;;
    --release) RELEASE=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

[[ -n "$DEVICE" && -n "$APP_APK" ]] || { usage >&2; exit 2; }
[[ -f "$APP_APK" ]] || { echo "RedactGuard APK not found: $APP_APK" >&2; exit 2; }
command -v adb >/dev/null || { echo "adb is required" >&2; exit 2; }
command -v apksigner >/dev/null || { echo "apksigner is required" >&2; exit 2; }
command -v python3 >/dev/null || { echo "python3 is required" >&2; exit 2; }

if [[ "$RELEASE" == true ]]; then
  APP_PACKAGE="io.github.daniele21.redactguard"
else
  APP_PACKAGE="io.github.daniele21.redactguard.debug"
fi

cleanup() {
  if [[ "$OWNED_INSTALL" == true ]]; then
    adb -s "$DEVICE" shell am force-stop "$APP_PACKAGE" >/dev/null 2>&1 || true
    adb -s "$DEVICE" uninstall "$APP_PACKAGE" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

adb -s "$DEVICE" get-state >/dev/null
if adb -s "$DEVICE" shell pm path "$APP_PACKAGE" 2>/dev/null | grep -q '^package:'; then
  echo "Refusing to replace existing package $APP_PACKAGE on $DEVICE." >&2
  echo "Use a clean test target so the smoke run owns and can remove its installation." >&2
  exit 4
fi

APK_SHA256="$(python3 - "$APP_APK" <<'PY'
import hashlib
import pathlib
import sys
path = pathlib.Path(sys.argv[1])
digest = hashlib.sha256()
with path.open('rb') as stream:
    for chunk in iter(lambda: stream.read(1024 * 1024), b''):
        digest.update(chunk)
print(digest.hexdigest())
PY
)"
SIGNER_SHA256="$(apksigner verify --print-certs "$APP_APK" | awk -F': ' '/Signer #1 certificate SHA-256 digest:/ {print tolower($2); exit}')"
[[ -n "$SIGNER_SHA256" ]] || { echo "Unable to resolve APK signer digest" >&2; exit 3; }

SOURCE_REVISION="$(git -C "$ROOT_DIR" rev-parse HEAD 2>/dev/null || printf 'unavailable')"
if git -C "$ROOT_DIR" diff --quiet --ignore-submodules HEAD -- 2>/dev/null && \
   git -C "$ROOT_DIR" diff --cached --quiet --ignore-submodules HEAD -- 2>/dev/null; then
  SOURCE_DIRTY=false
else
  SOURCE_DIRTY=true
fi
RUN_ID="smoke-$(date -u +%Y%m%dT%H%M%SZ)-$$"
DEVICE_MODEL="$(adb -s "$DEVICE" shell getprop ro.product.model | tr -d '\r')"
ANDROID_RELEASE="$(adb -s "$DEVICE" shell getprop ro.build.version.release | tr -d '\r')"
ANDROID_SDK="$(adb -s "$DEVICE" shell getprop ro.build.version.sdk | tr -d '\r')"
DEVICE_ABI="$(adb -s "$DEVICE" shell getprop ro.product.cpu.abi | tr -d '\r')"

adb -s "$DEVICE" install "$APP_APK" >/dev/null
OWNED_INSTALL=true
adb -s "$DEVICE" shell pm path "$APP_PACKAGE" >/dev/null
adb -s "$DEVICE" shell am force-stop "$APP_PACKAGE" >/dev/null
adb -s "$DEVICE" shell monkey -p "$APP_PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 1

PID="$(adb -s "$DEVICE" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
[[ -n "$PID" ]] || { echo "RedactGuard did not remain running after launch" >&2; exit 5; }

adb -s "$DEVICE" shell am force-stop "$APP_PACKAGE" >/dev/null
sleep 1
if [[ -n "$(adb -s "$DEVICE" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)" ]]; then
  echo "RedactGuard process remained after force-stop" >&2
  exit 6
fi

adb -s "$DEVICE" uninstall "$APP_PACKAGE" >/dev/null
OWNED_INSTALL=false
if adb -s "$DEVICE" shell pm path "$APP_PACKAGE" 2>/dev/null | grep -q '^package:'; then
  echo "RedactGuard package remained installed after smoke cleanup" >&2
  exit 7
fi
trap - EXIT INT TERM

EVIDENCE_DIR="$ROOT_DIR/evidence/local/smoke/$RUN_ID"
mkdir -p "$EVIDENCE_DIR"
export RUN_ID DEVICE APP_PACKAGE APP_APK APK_SHA256 SIGNER_SHA256 SOURCE_REVISION SOURCE_DIRTY
export DEVICE_MODEL ANDROID_RELEASE ANDROID_SDK DEVICE_ABI EVIDENCE_DIR
python3 <<'PY'
import json
import os
from pathlib import Path
payload = {
    "schemaVersion": 1,
    "runId": os.environ["RUN_ID"],
    "kind": "android-app-smoke",
    "result": "PASS",
    "sourceRevision": os.environ["SOURCE_REVISION"],
    "sourceDirty": os.environ["SOURCE_DIRTY"] == "true",
    "device": {
        "serial": os.environ["DEVICE"],
        "model": os.environ["DEVICE_MODEL"],
        "androidRelease": os.environ["ANDROID_RELEASE"],
        "sdk": os.environ["ANDROID_SDK"],
        "abi": os.environ["DEVICE_ABI"],
    },
    "application": {
        "package": os.environ["APP_PACKAGE"],
        "apk": str(Path(os.environ["APP_APK"]).resolve()),
        "sha256": os.environ["APK_SHA256"],
        "signerSha256": os.environ["SIGNER_SHA256"],
    },
    "checks": {
        "cleanPackageBeforeInstall": True,
        "install": True,
        "launchProcessObserved": True,
        "forceStopClearedProcess": True,
        "ownedInstallRemoved": True,
    },
}
Path(os.environ["EVIDENCE_DIR"], "result.json").write_text(
    json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
)
PY

echo "RedactGuard smoke PASS"
echo "Run ID:   $RUN_ID"
echo "APK SHA:  $APK_SHA256"
echo "Signer:   $SIGNER_SHA256"
echo "Evidence: $EVIDENCE_DIR/result.json"
