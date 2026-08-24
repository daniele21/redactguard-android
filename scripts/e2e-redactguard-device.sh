#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEVICE=""
HOST_APK=""
APP_APK=""
HOST_SOURCE_REVISION=""
PRESET_REVISION=""
RELEASE=false
HOST_OWNED=false
APP_OWNED=false

usage() {
  cat <<'EOF'
Usage:
  bash scripts/e2e-redactguard-device.sh \
    --device SERIAL \
    --host-apk PATH \
    --app-apk PATH \
    --host-source-revision FULL_SHA \
    --preset-revision SAFE_REVISION \
    [--release]

Runs the physical two-APK RedactGuard/Harness critical-journey gate on a clean
interactive Android test target. The command verifies signer identity, stages
Host-absent then Host-present states, records operator attestations for the
manual SAF/review/recovery/export portions, and removes only installations it owns.

The complete scenario definitions remain canonical in docs/evidence/physical-two-apk.md.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --host-apk) HOST_APK="$2"; shift 2 ;;
    --app-apk) APP_APK="$2"; shift 2 ;;
    --host-source-revision) HOST_SOURCE_REVISION="$2"; shift 2 ;;
    --preset-revision) PRESET_REVISION="$2"; shift 2 ;;
    --release) RELEASE=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

[[ -n "$DEVICE" && -n "$HOST_APK" && -n "$APP_APK" && -n "$HOST_SOURCE_REVISION" && -n "$PRESET_REVISION" ]] || {
  usage >&2
  exit 2
}
[[ -f "$HOST_APK" ]] || { echo "Host APK not found: $HOST_APK" >&2; exit 2; }
[[ -f "$APP_APK" ]] || { echo "RedactGuard APK not found: $APP_APK" >&2; exit 2; }
[[ "$HOST_SOURCE_REVISION" =~ ^[0-9a-fA-F]{40,64}$ ]] || { echo "Host source revision must be a full revision SHA" >&2; exit 2; }
[[ "$PRESET_REVISION" =~ ^[A-Za-z0-9._:-]+$ ]] || { echo "Preset revision contains unsupported characters" >&2; exit 2; }
[[ -t 0 ]] || { echo "Physical E2E requires an interactive terminal for operator attestations" >&2; exit 2; }
command -v adb >/dev/null || { echo "adb is required" >&2; exit 2; }
command -v apksigner >/dev/null || { echo "apksigner is required" >&2; exit 2; }
command -v python3 >/dev/null || { echo "python3 is required" >&2; exit 2; }

if [[ "$RELEASE" == true ]]; then
  HOST_PACKAGE="io.github.daniele21.localllm.phonetest"
  APP_PACKAGE="io.github.daniele21.redactguard"
else
  HOST_PACKAGE="io.github.daniele21.localllm.phonetest.debug"
  APP_PACKAGE="io.github.daniele21.redactguard.debug"
fi

cleanup() {
  adb -s "$DEVICE" shell am force-stop "$APP_PACKAGE" >/dev/null 2>&1 || true
  adb -s "$DEVICE" shell am force-stop "$HOST_PACKAGE" >/dev/null 2>&1 || true
  if [[ "$APP_OWNED" == true ]]; then
    adb -s "$DEVICE" uninstall "$APP_PACKAGE" >/dev/null 2>&1 || true
  fi
  if [[ "$HOST_OWNED" == true ]]; then
    adb -s "$DEVICE" uninstall "$HOST_PACKAGE" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

signer_sha() {
  apksigner verify --print-certs "$1" \
    | awk -F': ' '/Signer #1 certificate SHA-256 digest:/ {print tolower($2); exit}'
}

file_sha() {
  python3 - "$1" <<'PY'
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
}

require_clean_package() {
  local package="$1"
  if adb -s "$DEVICE" shell pm path "$package" 2>/dev/null | grep -q '^package:'; then
    echo "Refusing to replace existing package $package on $DEVICE." >&2
    echo "Use a clean dedicated E2E target so this run owns its installations." >&2
    exit 4
  fi
}

attest() {
  local token="$1"
  local description="$2"
  echo
  echo "$description"
  printf 'Type %s to attest this checkpoint: ' "$token"
  local answer=""
  IFS= read -r answer
  [[ "$answer" == "$token" ]] || {
    echo "Checkpoint not attested: $token" >&2
    exit 10
  }
}

adb -s "$DEVICE" get-state >/dev/null
require_clean_package "$HOST_PACKAGE"
require_clean_package "$APP_PACKAGE"

HOST_SIGNER="$(signer_sha "$HOST_APK")"
APP_SIGNER="$(signer_sha "$APP_APK")"
[[ -n "$HOST_SIGNER" && -n "$APP_SIGNER" ]] || { echo "Unable to resolve signer digests" >&2; exit 3; }
[[ "$HOST_SIGNER" == "$APP_SIGNER" ]] || {
  echo "Signer mismatch: Harness=$HOST_SIGNER RedactGuard=$APP_SIGNER" >&2
  exit 5
}
HOST_SHA256="$(file_sha "$HOST_APK")"
APP_SHA256="$(file_sha "$APP_APK")"
APP_SOURCE_REVISION="$(git -C "$ROOT_DIR" rev-parse HEAD 2>/dev/null || printf 'unavailable')"
CONSUMER_SDK_VERSION="$(sed -n 's/^harnessConsumerSdk = "\([^"]*\)"/\1/p' "$ROOT_DIR/gradle/libs.versions.toml" | head -n 1)"
RUN_ID="e2e-$(date -u +%Y%m%dT%H%M%SZ)-$$"
DEVICE_MODEL="$(adb -s "$DEVICE" shell getprop ro.product.model | tr -d '\r')"
ANDROID_RELEASE="$(adb -s "$DEVICE" shell getprop ro.build.version.release | tr -d '\r')"
ANDROID_SDK="$(adb -s "$DEVICE" shell getprop ro.build.version.sdk | tr -d '\r')"
DEVICE_ABI="$(adb -s "$DEVICE" shell getprop ro.product.cpu.abi | tr -d '\r')"

# Stage 1: RedactGuard without Host. This makes Host-absence recovery observable before
# the Host package exists, instead of installing both APKs first and losing that state.
adb -s "$DEVICE" install "$APP_APK" >/dev/null
APP_OWNED=true
adb -s "$DEVICE" shell monkey -p "$APP_PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
attest "HOST_ABSENT_OK" "Confirm RedactGuard shows the AI-local unavailable/not-installed state and analysis cannot start."

# Stage 2: introduce the exact same-signer Host artifact and let the operator make the
# declared preset/model ready through Harness-owned controls.
adb -s "$DEVICE" install "$HOST_APK" >/dev/null
HOST_OWNED=true
attest "HOST_READY" "Open Local AI Harness, make preset revision '$PRESET_REVISION' ready, then return here."
adb -s "$DEVICE" shell am force-stop "$APP_PACKAGE" >/dev/null
adb -s "$DEVICE" shell monkey -p "$APP_PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
attest "LOCAL_AI_READY" "Confirm RedactGuard reaches the 'AI locale pronta' state without reinstalling RedactGuard."

cat <<EOF

Continue the exact physical scenarios in:
  $ROOT_DIR/docs/evidence/physical-two-apk.md

Use synthetic fixtures only. Do not paste document text, findings or prompts into this terminal.
EOF

attest "INPUTS_OK" "Confirm pasted text, text PDF, multi-page text PDF and image-only PDF behaviors match the runbook, including RG-PDF-008 for image-only input."
attest "REVIEW_OK" "Confirm local analysis is atomic; Review hides values by default; reveal/hide and Oscura/Ignora decisions behave correctly."
attest "RECOVERY_OK" "Confirm cancellation and Host death/restart produce classified recovery behavior with no partial findings."
attest "EXPORT_OK" "Confirm export, independent PDF reopen and a failed/unwritable destination behave as specified, including partial-output cleanup."
attest "PERSISTENCE_OK" "Confirm process kill/relaunch does not restore document text, revealed values or review state."

# Explicit success cleanup; the trap remains a best-effort fallback for every earlier exit.
adb -s "$DEVICE" shell am force-stop "$APP_PACKAGE" >/dev/null
adb -s "$DEVICE" shell am force-stop "$HOST_PACKAGE" >/dev/null
adb -s "$DEVICE" uninstall "$APP_PACKAGE" >/dev/null
APP_OWNED=false
adb -s "$DEVICE" uninstall "$HOST_PACKAGE" >/dev/null
HOST_OWNED=false
require_clean_package "$APP_PACKAGE"
require_clean_package "$HOST_PACKAGE"
trap - EXIT INT TERM

EVIDENCE_DIR="$ROOT_DIR/evidence/local/e2e/$RUN_ID"
mkdir -p "$EVIDENCE_DIR"
export RUN_ID DEVICE DEVICE_MODEL ANDROID_RELEASE ANDROID_SDK DEVICE_ABI
export HOST_PACKAGE APP_PACKAGE HOST_APK APP_APK HOST_SHA256 APP_SHA256 HOST_SIGNER APP_SIGNER
export HOST_SOURCE_REVISION APP_SOURCE_REVISION PRESET_REVISION CONSUMER_SDK_VERSION EVIDENCE_DIR
python3 <<'PY'
import json
import os
from pathlib import Path
payload = {
    "schemaVersion": 1,
    "runId": os.environ["RUN_ID"],
    "kind": "physical-two-apk-e2e",
    "result": "PASS",
    "device": {
        "serial": os.environ["DEVICE"],
        "model": os.environ["DEVICE_MODEL"],
        "androidRelease": os.environ["ANDROID_RELEASE"],
        "sdk": os.environ["ANDROID_SDK"],
        "abi": os.environ["DEVICE_ABI"],
    },
    "host": {
        "package": os.environ["HOST_PACKAGE"],
        "apk": str(Path(os.environ["HOST_APK"]).resolve()),
        "sha256": os.environ["HOST_SHA256"],
        "signerSha256": os.environ["HOST_SIGNER"],
        "sourceRevision": os.environ["HOST_SOURCE_REVISION"],
        "presetRevision": os.environ["PRESET_REVISION"],
    },
    "redactGuard": {
        "package": os.environ["APP_PACKAGE"],
        "apk": str(Path(os.environ["APP_APK"]).resolve()),
        "sha256": os.environ["APP_SHA256"],
        "signerSha256": os.environ["APP_SIGNER"],
        "sourceRevision": os.environ["APP_SOURCE_REVISION"],
        "consumerSdkVersion": os.environ["CONSUMER_SDK_VERSION"],
    },
    "operatorAttestations": [
        "HOST_ABSENT_OK",
        "HOST_READY",
        "LOCAL_AI_READY",
        "INPUTS_OK",
        "REVIEW_OK",
        "RECOVERY_OK",
        "EXPORT_OK",
        "PERSISTENCE_OK",
    ],
    "cleanupVerified": True,
    "privacy": "synthetic fixtures only; no document text/findings/prompts captured",
}
Path(os.environ["EVIDENCE_DIR"], "result.json").write_text(
    json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
)
PY

echo
echo "Physical two-APK E2E PASS"
echo "Run ID:       $RUN_ID"
echo "Host SHA:     $HOST_SHA256"
echo "RedactGuard:  $APP_SHA256"
echo "Signer:       $APP_SIGNER"
echo "Evidence:     $EVIDENCE_DIR/result.json"
