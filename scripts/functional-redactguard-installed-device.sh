#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEVICE="${REDACTGUARD_DEVICE:-}"
PRESET_REVISION="${REDACTGUARD_PRESET_REVISION:-installed-current}"
RELEASE=true

usage() {
  cat <<'EOF'
Usage:
  bash scripts/functional-redactguard-installed-device.sh \
    --device SERIAL \
    [--preset-revision REVISION] \
    [--debug]

Runs a non-destructive, interactive functional test against Harness and
RedactGuard packages that are already installed on a physical Android device.
It does not install, update or uninstall either package, so existing Harness
models/configuration are preserved.

This is useful for validating the currently installed pair. It is not the
canonical clean physical two-APK E2E because it cannot prove Host-absent install
behavior or exact local source/APK identity.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --preset-revision) PRESET_REVISION="$2"; shift 2 ;;
    --debug) RELEASE=false; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

[[ -n "$DEVICE" ]] || { usage >&2; exit 2; }
[[ -t 0 ]] || { echo "Functional device test requires an interactive terminal." >&2; exit 2; }
command -v adb >/dev/null || { echo "adb is required on PATH." >&2; exit 2; }
command -v python3 >/dev/null || { echo "python3 is required on PATH." >&2; exit 2; }
adb -s "$DEVICE" get-state >/dev/null

if [[ "$RELEASE" == true ]]; then
  HOST_PACKAGE="io.github.daniele21.localllm.phonetest"
  APP_PACKAGE="io.github.daniele21.redactguard"
else
  HOST_PACKAGE="io.github.daniele21.localllm.phonetest.debug"
  APP_PACKAGE="io.github.daniele21.redactguard.debug"
fi

package_present() {
  adb -s "$DEVICE" shell pm path "$1" 2>/dev/null | grep -q '^package:'
}

package_version_code() {
  adb -s "$DEVICE" shell dumpsys package "$1" 2>/dev/null \
    | sed -n 's/.*versionCode=\([0-9][0-9]*\).*/\1/p' \
    | head -n 1
}

package_version_name() {
  adb -s "$DEVICE" shell dumpsys package "$1" 2>/dev/null \
    | sed -n 's/^[[:space:]]*versionName=//p' \
    | head -n 1 \
    | tr -d '\r'
}

launch_package() {
  local package="$1"
  adb -s "$DEVICE" shell am force-stop "$package" >/dev/null
  adb -s "$DEVICE" shell monkey -p "$package" -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 1
  local pid
  pid="$(adb -s "$DEVICE" shell pidof "$package" 2>/dev/null | tr -d '\r' || true)"
  [[ -n "$pid" ]] || { echo "$package did not remain running after launch." >&2; exit 5; }
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

pause_for_operator() {
  local description="$1"
  echo
  echo "$description"
  printf 'Press ENTER when ready: '
  IFS= read -r _
}

package_present "$HOST_PACKAGE" || { echo "Harness is not installed: $HOST_PACKAGE" >&2; exit 4; }
package_present "$APP_PACKAGE" || { echo "RedactGuard is not installed: $APP_PACKAGE" >&2; exit 4; }

HOST_VERSION_CODE="$(package_version_code "$HOST_PACKAGE")"
HOST_VERSION_NAME="$(package_version_name "$HOST_PACKAGE")"
APP_VERSION_CODE="$(package_version_code "$APP_PACKAGE")"
APP_VERSION_NAME="$(package_version_name "$APP_PACKAGE")"
DEVICE_MODEL="$(adb -s "$DEVICE" shell getprop ro.product.model | tr -d '\r')"
ANDROID_RELEASE="$(adb -s "$DEVICE" shell getprop ro.build.version.release | tr -d '\r')"
ANDROID_SDK="$(adb -s "$DEVICE" shell getprop ro.build.version.sdk | tr -d '\r')"
DEVICE_ABI="$(adb -s "$DEVICE" shell getprop ro.product.cpu.abi | tr -d '\r')"
RUN_ID="installed-functional-$(date -u +%Y%m%dT%H%M%SZ)-$$"
EVIDENCE_DIR="$ROOT_DIR/evidence/local/installed-functional/$RUN_ID"
mkdir -p "$EVIDENCE_DIR"

cat <<EOF
Connected-device installed functional test
Device:      $DEVICE_MODEL ($DEVICE), Android $ANDROID_RELEASE / API $ANDROID_SDK / $DEVICE_ABI
Harness:     $HOST_PACKAGE versionCode=$HOST_VERSION_CODE versionName=$HOST_VERSION_NAME
RedactGuard: $APP_PACKAGE versionCode=$APP_VERSION_CODE versionName=$APP_VERSION_NAME
Preset:      $PRESET_REVISION

This run preserves installed app data. Use synthetic fixtures only.
It validates the currently installed pair, not exact local APK/source identity.
EOF

launch_package "$HOST_PACKAGE"
attest "HOST_READY" "On the phone, confirm Harness opens and the intended model/preset is ready. If '$PRESET_REVISION' is not the exact revision, stop and rerun with --preset-revision <exact-revision>."

launch_package "$APP_PACKAGE"
attest "LOCAL_AI_READY" "Confirm RedactGuard reaches 'AI locale pronta' and analysis is available without exposing model/runtime tuning."

cat <<'EOF'

Synthetic pasted-text fixture (safe to type into RedactGuard):
  Mario Rossi vive in Via Roma 25, Milano.
  Email: mario.rossi@example.com
  Telefono: +39 333 1234567
EOF
attest "PASTED_TEXT_OK" "Use the synthetic fixture. Confirm Input -> Definitions -> local analysis completes and reaches Review with plausible PII findings."

attest "REVIEW_OK" "Confirm finding values are hidden by default; reveal and hide one; mark at least one occurrence Oscura and one Ignora; confirm export stays gated until required decisions are complete."

pause_for_operator "Start another local analysis in RedactGuard. Return to this terminal while generation is active; the script will then force-stop Harness to test Binder Host death/recovery."
adb -s "$DEVICE" shell am force-stop "$HOST_PACKAGE" >/dev/null
attest "HOST_DEATH_OK" "Confirm RedactGuard reports a classified local-AI disconnect/recovery state and exposes no partial findings from the interrupted analysis."

launch_package "$HOST_PACKAGE"
attest "HOST_RECOVERED" "Confirm Harness is ready again. Return to RedactGuard and confirm local AI can become ready again without reinstalling either app."

attest "EXPORT_OK" "Complete a synthetic analysis/review, export to PDF, then reopen the exported PDF in an independent viewer. Confirm Oscura content is absent/replaced and Ignora content remains."

pause_for_operator "Before continuing, reveal at least one synthetic finding in RedactGuard. The script will force-stop and relaunch RedactGuard to test process-local privacy."
adb -s "$DEVICE" shell am force-stop "$APP_PACKAGE" >/dev/null
launch_package "$APP_PACKAGE"
attest "PERSISTENCE_OK" "Confirm relaunch does not restore prior document text, findings, revealed values or review decisions."

export RUN_ID EVIDENCE_DIR DEVICE DEVICE_MODEL ANDROID_RELEASE ANDROID_SDK DEVICE_ABI
export HOST_PACKAGE APP_PACKAGE HOST_VERSION_CODE HOST_VERSION_NAME APP_VERSION_CODE APP_VERSION_NAME PRESET_REVISION
python3 <<'PY'
import json
import os
from pathlib import Path
payload = {
    "schemaVersion": 1,
    "runId": os.environ["RUN_ID"],
    "kind": "installed-two-apk-functional",
    "result": "PASS",
    "scope": "interactive functional validation of currently installed packages; non-destructive; not canonical clean E2E",
    "device": {
        "serial": os.environ["DEVICE"],
        "model": os.environ["DEVICE_MODEL"],
        "androidRelease": os.environ["ANDROID_RELEASE"],
        "sdk": os.environ["ANDROID_SDK"],
        "abi": os.environ["DEVICE_ABI"],
    },
    "host": {
        "package": os.environ["HOST_PACKAGE"],
        "versionCode": os.environ["HOST_VERSION_CODE"],
        "versionName": os.environ["HOST_VERSION_NAME"],
        "presetRevision": os.environ["PRESET_REVISION"],
    },
    "redactGuard": {
        "package": os.environ["APP_PACKAGE"],
        "versionCode": os.environ["APP_VERSION_CODE"],
        "versionName": os.environ["APP_VERSION_NAME"],
    },
    "operatorAttestations": [
        "HOST_READY",
        "LOCAL_AI_READY",
        "PASTED_TEXT_OK",
        "REVIEW_OK",
        "HOST_DEATH_OK",
        "HOST_RECOVERED",
        "EXPORT_OK",
        "PERSISTENCE_OK",
    ],
    "privacy": "synthetic fixtures only; no document text, findings, prompts or binder payloads captured",
}
Path(os.environ["EVIDENCE_DIR"], "result.json").write_text(
    json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
)
PY

echo
echo "Installed functional test PASS"
echo "Run ID:   $RUN_ID"
echo "Evidence: $EVIDENCE_DIR/result.json"
echo
echo "This PASS applies to the currently installed pair only."
echo "Run scripts/e2e-redactguard-device.sh on a clean target to close the canonical physical two-APK gate."
