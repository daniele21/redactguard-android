#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/physical-two-apk-preflight.sh --device SERIAL --host-apk PATH --app-apk PATH [--release]

Installs Harness + RedactGuard on one Android device after verifying both APKs have the same signer.
Debug defaults:
  host package: io.github.daniele21.localllm.phonetest.debug
  app package:  io.github.daniele21.redactguard.debug
EOF
}

DEVICE=""
HOST_APK=""
APP_APK=""
RELEASE=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --host-apk) HOST_APK="$2"; shift 2 ;;
    --app-apk) APP_APK="$2"; shift 2 ;;
    --release) RELEASE=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

[[ -n "$DEVICE" && -n "$HOST_APK" && -n "$APP_APK" ]] || { usage >&2; exit 2; }
[[ -f "$HOST_APK" ]] || { echo "Host APK not found: $HOST_APK" >&2; exit 2; }
[[ -f "$APP_APK" ]] || { echo "RedactGuard APK not found: $APP_APK" >&2; exit 2; }
command -v adb >/dev/null || { echo "adb is required" >&2; exit 2; }
command -v apksigner >/dev/null || { echo "apksigner is required" >&2; exit 2; }

if [[ "$RELEASE" == true ]]; then
  HOST_PACKAGE="io.github.daniele21.localllm.phonetest"
  APP_PACKAGE="io.github.daniele21.redactguard"
else
  HOST_PACKAGE="io.github.daniele21.localllm.phonetest.debug"
  APP_PACKAGE="io.github.daniele21.redactguard.debug"
fi

signer_sha() {
  apksigner verify --print-certs "$1" \
    | awk -F': ' '/Signer #1 certificate SHA-256 digest:/ {print tolower($2); exit}'
}

HOST_SIGNER="$(signer_sha "$HOST_APK")"
APP_SIGNER="$(signer_sha "$APP_APK")"
[[ -n "$HOST_SIGNER" && -n "$APP_SIGNER" ]] || { echo "Unable to resolve APK signer digests" >&2; exit 3; }
if [[ "$HOST_SIGNER" != "$APP_SIGNER" ]]; then
  echo "Signer mismatch: Harness=$HOST_SIGNER RedactGuard=$APP_SIGNER" >&2
  exit 4
fi

echo "Signer match: $HOST_SIGNER"
adb -s "$DEVICE" get-state >/dev/null
adb -s "$DEVICE" install -r "$HOST_APK"
adb -s "$DEVICE" install -r "$APP_APK"

adb -s "$DEVICE" shell pm path "$HOST_PACKAGE" >/dev/null
adb -s "$DEVICE" shell pm path "$APP_PACKAGE" >/dev/null

echo "Installed host: $HOST_PACKAGE"
echo "Installed app:  $APP_PACKAGE"

adb -s "$DEVICE" shell am force-stop "$APP_PACKAGE"
adb -s "$DEVICE" shell monkey -p "$APP_PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null

echo "RedactGuard launched on $DEVICE. Continue the SAF/import/analyze/review/export evidence steps from docs/evidence/physical-two-apk.md."
