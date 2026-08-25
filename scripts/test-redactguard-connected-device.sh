#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-help}"
if [[ $# -gt 0 ]]; then shift; fi

DEVICE="${REDACTGUARD_DEVICE:-}"
HOST_APK="${REDACTGUARD_HOST_APK:-}"
APP_APK="${REDACTGUARD_APP_APK:-}"
HOST_SOURCE_REVISION="${REDACTGUARD_HOST_SOURCE_REVISION:-}"
PRESET_REVISION="${REDACTGUARD_PRESET_REVISION:-}"
RELEASE=true

usage() {
  cat <<'EOF'
Usage:
  bash scripts/test-redactguard-connected-device.sh inspect \
    --device SERIAL [--host-apk PATH] [--app-apk PATH] [--debug]

  bash scripts/test-redactguard-connected-device.sh installed-smoke \
    --device SERIAL [--debug]

  bash scripts/test-redactguard-connected-device.sh clean-e2e \
    --device SERIAL \
    --host-apk PATH \
    --app-apk PATH \
    --host-source-revision FULL_SHA \
    --preset-revision REVISION \
    [--debug]

Modes:
  inspect
    Non-destructive. Shows device identity, installed package/version/signing identity,
    optional local APK version/signing identity, and whether an in-place adb update is
    compatible. It never installs or uninstalls anything.

  installed-smoke
    Non-destructive. Launches the already-installed Harness and RedactGuard packages,
    verifies both processes start, and records a small privacy-safe evidence file.
    This proves the currently installed build launches; it is NOT the canonical clean
    two-APK E2E and does not prove that the installed build equals the local source.

  clean-e2e
    Delegates to scripts/e2e-redactguard-device.sh. Requires both selected packages to
    be absent before the run. This is the canonical physical two-APK gate and records
    exact APK/source/signer/preset/device identity plus interactive operator attestations.

Environment aliases:
  REDACTGUARD_DEVICE
  REDACTGUARD_HOST_APK
  REDACTGUARD_APP_APK
  REDACTGUARD_HOST_SOURCE_REVISION
  REDACTGUARD_PRESET_REVISION
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --host-apk) HOST_APK="$2"; shift 2 ;;
    --app-apk) APP_APK="$2"; shift 2 ;;
    --host-source-revision) HOST_SOURCE_REVISION="$2"; shift 2 ;;
    --preset-revision) PRESET_REVISION="$2"; shift 2 ;;
    --debug) RELEASE=false; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

if [[ "$MODE" == "help" || "$MODE" == "-h" || "$MODE" == "--help" ]]; then
  usage
  exit 0
fi

[[ -n "$DEVICE" ]] || { echo "--device is required (or set REDACTGUARD_DEVICE)." >&2; exit 2; }
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

find_android_tool() {
  local tool="$1"
  if command -v "$tool" >/dev/null 2>&1; then
    command -v "$tool"
    return 0
  fi
  local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [[ -n "$sdk" && -d "$sdk/build-tools" ]]; then
    find "$sdk/build-tools" -type f -name "$tool" 2>/dev/null | sort -V | tail -n 1
    return 0
  fi
  return 1
}

APKSIGNER="$(find_android_tool apksigner || true)"
AAPT="$(find_android_tool aapt || true)"

package_path() {
  adb -s "$DEVICE" shell pm path "$1" 2>/dev/null | head -n 1 | sed 's/^package://' | tr -d '\r'
}

package_installed() {
  [[ -n "$(package_path "$1")" ]]
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

local_apk_version_code() {
  local apk="$1"
  [[ -n "$AAPT" && -f "$apk" ]] || return 0
  "$AAPT" dump badging "$apk" 2>/dev/null \
    | sed -n "s/.*versionCode='\([0-9][0-9]*\)'.*/\1/p" \
    | head -n 1
}

local_apk_version_name() {
  local apk="$1"
  [[ -n "$AAPT" && -f "$apk" ]] || return 0
  "$AAPT" dump badging "$apk" 2>/dev/null \
    | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" \
    | head -n 1
}

local_apk_signer() {
  local apk="$1"
  [[ -n "$APKSIGNER" && -f "$apk" ]] || return 0
  "$APKSIGNER" verify --print-certs "$apk" 2>/dev/null \
    | awk -F': ' '/Signer #1 certificate SHA-256 digest:/ {print tolower($2); exit}'
}

installed_apk_signer() {
  local package="$1"
  [[ -n "$APKSIGNER" ]] || return 0
  local remote
  remote="$(package_path "$package")"
  [[ -n "$remote" ]] || return 0
  local tmp
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' RETURN
  adb -s "$DEVICE" pull "$remote" "$tmp/base.apk" >/dev/null
  "$APKSIGNER" verify --print-certs "$tmp/base.apk" 2>/dev/null \
    | awk -F': ' '/Signer #1 certificate SHA-256 digest:/ {print tolower($2); exit}'
  rm -rf "$tmp"
  trap - RETURN
}

print_device_identity() {
  echo "Device serial:   $DEVICE"
  echo "Device model:    $(adb -s "$DEVICE" shell getprop ro.product.model | tr -d '\r')"
  echo "Android release: $(adb -s "$DEVICE" shell getprop ro.build.version.release | tr -d '\r')"
  echo "Android SDK:     $(adb -s "$DEVICE" shell getprop ro.build.version.sdk | tr -d '\r')"
  echo "ABI:             $(adb -s "$DEVICE" shell getprop ro.product.cpu.abi | tr -d '\r')"
}

inspect_one() {
  local label="$1"
  local package="$2"
  local apk="$3"
  local installed_code=""
  local installed_name=""
  local installed_signer=""
  local local_code=""
  local local_name=""
  local local_signer=""

  echo
  echo "== $label =="
  echo "Package: $package"

  if package_installed "$package"; then
    installed_code="$(package_version_code "$package")"
    installed_name="$(package_version_name "$package")"
    installed_signer="$(installed_apk_signer "$package")"
    echo "Installed: yes"
    echo "Installed versionCode: ${installed_code:-unknown}"
    echo "Installed versionName: ${installed_name:-unknown}"
    echo "Installed signer:      ${installed_signer:-unknown}"
  else
    echo "Installed: no"
  fi

  if [[ -n "$apk" ]]; then
    if [[ -f "$apk" ]]; then
      local_code="$(local_apk_version_code "$apk")"
      local_name="$(local_apk_version_name "$apk")"
      local_signer="$(local_apk_signer "$apk")"
      echo "Local APK: $apk"
      echo "Local versionCode: ${local_code:-unknown}"
      echo "Local versionName: ${local_name:-unknown}"
      echo "Local signer:      ${local_signer:-unknown}"
      if [[ -n "$installed_signer" && -n "$local_signer" ]]; then
        if [[ "$installed_signer" == "$local_signer" ]]; then
          echo "Update signer compatibility: OK"
        else
          echo "Update signer compatibility: MISMATCH"
        fi
      fi
      if [[ -n "$installed_code" && -n "$local_code" ]]; then
        if (( local_code >= installed_code )); then
          echo "Update version compatibility: OK"
        else
          echo "Update version compatibility: DOWNGRADE"
        fi
      fi
    else
      echo "Local APK: MISSING ($apk)"
    fi
  fi
}

run_inspect() {
  print_device_identity
  inspect_one "Harness" "$HOST_PACKAGE" "$HOST_APK"
  inspect_one "RedactGuard" "$APP_PACKAGE" "$APP_APK"

  if package_installed "$HOST_PACKAGE" && package_installed "$APP_PACKAGE" && [[ -n "$APKSIGNER" ]]; then
    local host_installed_signer app_installed_signer
    host_installed_signer="$(installed_apk_signer "$HOST_PACKAGE")"
    app_installed_signer="$(installed_apk_signer "$APP_PACKAGE")"
    echo
    if [[ -n "$host_installed_signer" && "$host_installed_signer" == "$app_installed_signer" ]]; then
      echo "Installed Harness <-> RedactGuard signer: SAME"
    else
      echo "Installed Harness <-> RedactGuard signer: DIFFERENT"
    fi
  fi

  if [[ -n "$HOST_APK" && -n "$APP_APK" && -f "$HOST_APK" && -f "$APP_APK" && -n "$APKSIGNER" ]]; then
    local host_local_signer app_local_signer
    host_local_signer="$(local_apk_signer "$HOST_APK")"
    app_local_signer="$(local_apk_signer "$APP_APK")"
    echo
    if [[ -n "$host_local_signer" && "$host_local_signer" == "$app_local_signer" ]]; then
      echo "Local Harness <-> RedactGuard signer: SAME"
    else
      echo "Local Harness <-> RedactGuard signer: DIFFERENT"
    fi
  fi

  echo
  echo "Interpretation:"
  echo "- installed-smoke is safe and non-destructive for already-installed packages."
  echo "- adb install -r requires installed/local signer compatibility and a non-downgrade versionCode."
  echo "- clean-e2e requires selected packages to be absent and proves the canonical two-APK gate."
}

run_installed_smoke() {
  package_installed "$HOST_PACKAGE" || { echo "Harness package is not installed: $HOST_PACKAGE" >&2; exit 4; }
  package_installed "$APP_PACKAGE" || { echo "RedactGuard package is not installed: $APP_PACKAGE" >&2; exit 4; }

  local run_id evidence_dir host_pid app_pid
  run_id="installed-smoke-$(date -u +%Y%m%dT%H%M%SZ)-$$"
  evidence_dir="$ROOT_DIR/evidence/local/installed-smoke/$run_id"
  mkdir -p "$evidence_dir"

  adb -s "$DEVICE" shell am force-stop "$HOST_PACKAGE" >/dev/null
  adb -s "$DEVICE" shell monkey -p "$HOST_PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 1
  host_pid="$(adb -s "$DEVICE" shell pidof "$HOST_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
  [[ -n "$host_pid" ]] || { echo "Harness did not remain running after launch." >&2; exit 5; }

  adb -s "$DEVICE" shell am force-stop "$APP_PACKAGE" >/dev/null
  adb -s "$DEVICE" shell monkey -p "$APP_PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 1
  app_pid="$(adb -s "$DEVICE" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
  [[ -n "$app_pid" ]] || { echo "RedactGuard did not remain running after launch." >&2; exit 5; }

  export RUN_ID="$run_id" EVIDENCE_DIR="$evidence_dir" DEVICE HOST_PACKAGE APP_PACKAGE HOST_PID="$host_pid" APP_PID="$app_pid"
  export HOST_VERSION_CODE="$(package_version_code "$HOST_PACKAGE")"
  export APP_VERSION_CODE="$(package_version_code "$APP_PACKAGE")"
  export HOST_VERSION_NAME="$(package_version_name "$HOST_PACKAGE")"
  export APP_VERSION_NAME="$(package_version_name "$APP_PACKAGE")"
  export DEVICE_MODEL="$(adb -s "$DEVICE" shell getprop ro.product.model | tr -d '\r')"
  export ANDROID_RELEASE="$(adb -s "$DEVICE" shell getprop ro.build.version.release | tr -d '\r')"
  export ANDROID_SDK="$(adb -s "$DEVICE" shell getprop ro.build.version.sdk | tr -d '\r')"
  export DEVICE_ABI="$(adb -s "$DEVICE" shell getprop ro.product.cpu.abi | tr -d '\r')"

  python3 <<'PY'
import json
import os
from pathlib import Path
payload = {
    "schemaVersion": 1,
    "runId": os.environ["RUN_ID"],
    "kind": "installed-two-app-smoke",
    "result": "PASS",
    "scope": "non-destructive launch/process smoke of currently installed packages; not canonical clean E2E",
    "device": {
        "serial": os.environ["DEVICE"],
        "model": os.environ["DEVICE_MODEL"],
        "androidRelease": os.environ["ANDROID_RELEASE"],
        "sdk": os.environ["ANDROID_SDK"],
        "abi": os.environ["DEVICE_ABI"],
    },
    "host": {
        "package": os.environ["HOST_PACKAGE"],
        "versionCode": os.environ.get("HOST_VERSION_CODE", ""),
        "versionName": os.environ.get("HOST_VERSION_NAME", ""),
        "processObserved": True,
    },
    "redactGuard": {
        "package": os.environ["APP_PACKAGE"],
        "versionCode": os.environ.get("APP_VERSION_CODE", ""),
        "versionName": os.environ.get("APP_VERSION_NAME", ""),
        "processObserved": True,
    },
    "privacy": "no document text, findings, prompts or binder payloads captured",
}
Path(os.environ["EVIDENCE_DIR"], "result.json").write_text(
    json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
)
PY

  echo
  echo "Installed-app smoke PASS"
  echo "Harness PID:   $host_pid"
  echo "RedactGuard:   $app_pid"
  echo "Evidence:      $evidence_dir/result.json"
  echo
  echo "Next manual functional checks on the phone:"
  echo "1. Harness: confirm a model/preset is ready and note the exact preset revision."
  echo "2. RedactGuard: confirm 'AI locale collegata'; task readiness is verified when analysis starts."
  echo "3. Run synthetic pasted text through Definitions -> local analysis -> Review."
  echo "4. Confirm values are hidden by default; exercise Oscura/Ignora and export."
  echo "5. Reopen the exported PDF independently and verify redaction/ignored text."
  echo "6. For canonical evidence, run clean-e2e later against exact local release APKs."
}

run_clean_e2e() {
  [[ -n "$HOST_APK" && -n "$APP_APK" && -n "$HOST_SOURCE_REVISION" && -n "$PRESET_REVISION" ]] || {
    echo "clean-e2e requires --host-apk, --app-apk, --host-source-revision and --preset-revision." >&2
    exit 2
  }
  local args=(
    --device "$DEVICE"
    --host-apk "$HOST_APK"
    --app-apk "$APP_APK"
    --host-source-revision "$HOST_SOURCE_REVISION"
    --preset-revision "$PRESET_REVISION"
  )
  if [[ "$RELEASE" == true ]]; then
    args+=(--release)
  fi
  exec bash "$ROOT_DIR/scripts/e2e-redactguard-device.sh" "${args[@]}"
}

case "$MODE" in
  inspect) run_inspect ;;
  installed-smoke) run_installed_smoke ;;
  clean-e2e) run_clean_e2e ;;
  *) echo "Unknown mode: $MODE" >&2; usage >&2; exit 2 ;;
esac
