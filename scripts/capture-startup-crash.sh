#!/usr/bin/env bash
set -euo pipefail

PACKAGE="io.github.daniele21.redactguard"
ACTIVITY="io.github.daniele21.redactguard.MainActivity"
DEVICE=""
OUTPUT=""

usage() {
  cat <<'EOF'
Usage: bash scripts/capture-startup-crash.sh [--device SERIAL] [--output PATH]

Captures privacy-safe startup evidence for the Play/release RedactGuard package.
The script clears logcat immediately before launch and records AndroidRuntime /
ActivityManager startup diagnostics only. Do not import or open a document while
running this capture.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --output) OUTPUT="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

command -v adb >/dev/null 2>&1 || { echo "adb is required" >&2; exit 2; }

ADB=(adb)
if [[ -n "$DEVICE" ]]; then
  ADB+=( -s "$DEVICE" )
fi

"${ADB[@]}" get-state >/dev/null
if ! "${ADB[@]}" shell pm path "$PACKAGE" >/dev/null 2>&1; then
  echo "RedactGuard release package is not installed: $PACKAGE" >&2
  exit 3
fi

if [[ -z "$OUTPUT" ]]; then
  mkdir -p evidence/local
  OUTPUT="evidence/local/redactguard-startup-$(date +%Y%m%d-%H%M%S).log"
fi
mkdir -p "$(dirname "$OUTPUT")"

VERSION_NAME="$("${ADB[@]}" shell dumpsys package "$PACKAGE" | sed -n 's/.*versionName=//p' | head -n 1 | tr -d '\r')"
VERSION_CODE="$("${ADB[@]}" shell dumpsys package "$PACKAGE" | sed -n 's/.*versionCode=\([0-9]*\).*/\1/p' | head -n 1 | tr -d '\r')"
INSTALLER="$("${ADB[@]}" shell dumpsys package "$PACKAGE" | sed -n 's/.*installerPackageName=//p' | head -n 1 | tr -d '\r')"

"${ADB[@]}" shell am force-stop "$PACKAGE"
"${ADB[@]}" logcat -c

set +e
START_OUTPUT="$("${ADB[@]}" shell am start -W -n "$PACKAGE/$ACTIVITY" 2>&1)"
START_STATUS=$?
set -e

sleep 2
PID="$("${ADB[@]}" shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' || true)"

{
  echo "RedactGuard startup capture"
  echo "package=$PACKAGE"
  echo "versionName=${VERSION_NAME:-unknown}"
  echo "versionCode=${VERSION_CODE:-unknown}"
  echo "installer=${INSTALLER:-unknown}"
  echo "amStartExit=$START_STATUS"
  echo "pidAfter2s=${PID:-<none>}"
  echo
  echo "--- am start -W ---"
  printf '%s\n' "$START_OUTPUT"
  echo
  echo "--- AndroidRuntime / ActivityTaskManager ---"
  "${ADB[@]}" logcat -d -v threadtime 'AndroidRuntime:E' 'ActivityTaskManager:I' '*:S' || true
} >"$OUTPUT"

echo "Startup evidence written to: $OUTPUT"
if [[ -z "$PID" ]]; then
  echo "FAIL: RedactGuard process is not alive two seconds after launch." >&2
  echo "Inspect the first FATAL EXCEPTION in $OUTPUT." >&2
  exit 4
fi

echo "PASS: RedactGuard process is alive after startup (pid=$PID)."
