#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-snapshot}"
if [[ $# -gt 0 ]]; then shift; fi

DEVICE="${REDACTGUARD_DEVICE:-}"
RELEASE=true
RESTART=false
OUTPUT_DIR=""

usage() {
  cat <<'USAGE'
Usage:
  bash scripts/diagnose-redactguard-local-ai-device.sh snapshot \
    --device SERIAL [--debug] [--restart] [--output-dir PATH]

  bash scripts/diagnose-redactguard-local-ai-device.sh reproduce \
    --device SERIAL [--debug] [--restart] [--output-dir PATH]

Modes:
  snapshot
    Non-destructive inspection of the already-installed Harness + RedactGuard pair.
    Checks package identity, signature permission, explicit Host service resolution,
    process state and the active Binder service record.

  reproduce
    Runs snapshot first, then asks you to reproduce the Local AI failure and captures
    only RedactGuard's privacy-safe RG_LOCAL_AI technical events emitted after the
    reproduction checkpoint. It never captures UI text, document text, prompts,
    model output or raw Binder payloads.

Options:
  --device SERIAL     Required unless REDACTGUARD_DEVICE is set.
  --debug             Use .debug package identities and debug signature permission.
  --restart           Force-stop both apps before relaunch. App data is NOT cleared.
  --output-dir PATH   Override the local evidence directory.

This script never installs, uninstalls, runs pm clear, or deletes app/model data.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --debug) RELEASE=false; shift ;;
    --restart) RESTART=true; shift ;;
    --output-dir) OUTPUT_DIR="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

case "$MODE" in
  snapshot|reproduce) ;;
  help|-h|--help) usage; exit 0 ;;
  *) echo "Unknown mode: $MODE" >&2; usage >&2; exit 2 ;;
esac

[[ -n "$DEVICE" ]] || { echo "--device is required (or set REDACTGUARD_DEVICE)." >&2; exit 2; }
command -v adb >/dev/null || { echo "adb is required on PATH." >&2; exit 2; }
adb -s "$DEVICE" get-state >/dev/null

if [[ "$RELEASE" == true ]]; then
  HOST_PACKAGE="io.github.daniele21.localllm.phonetest"
  APP_PACKAGE="io.github.daniele21.redactguard"
  SHARED_RUNTIME_PERMISSION="io.github.daniele21.localllm.permission.USE_LOCAL_LLM"
else
  HOST_PACKAGE="io.github.daniele21.localllm.phonetest.debug"
  APP_PACKAGE="io.github.daniele21.redactguard.debug"
  SHARED_RUNTIME_PERMISSION="io.github.daniele21.localllm.debug.permission.USE_LOCAL_LLM"
fi

HOST_SERVICE_CLASS="io.github.daniele21.localllm.phonetest.HarnessSharedRuntimeService"
HOST_SERVICE_COMPONENT="$HOST_PACKAGE/$HOST_SERVICE_CLASS"
RUN_ID="local-ai-diagnostics-$(date -u +%Y%m%dT%H%M%SZ)-$$"
if [[ -z "$OUTPUT_DIR" ]]; then
  OUTPUT_DIR="$ROOT_DIR/evidence/local/local-ai-diagnostics/$RUN_ID"
fi
mkdir -p "$OUTPUT_DIR"

REPORT="$OUTPUT_DIR/report.txt"
TECHNICAL_LOG="$OUTPUT_DIR/technical-log.txt"
SERVICE_STATE="$OUTPUT_DIR/service-state.txt"
DIAGNOSIS="$OUTPUT_DIR/diagnosis.txt"
exec > >(tee "$REPORT") 2>&1

pass() { printf '[PASS] %s\n' "$*"; }
warn() { printf '[WARN] %s\n' "$*"; }
fail() { printf '[FAIL] %s\n' "$*"; }
info() { printf '[INFO] %s\n' "$*"; }

package_path() {
  adb -s "$DEVICE" shell pm path "$1" 2>/dev/null | head -n 1 | sed 's/^package://' | tr -d '\r'
}

package_installed() {
  [[ -n "$(package_path "$1")" ]]
}

package_dump() {
  adb -s "$DEVICE" shell dumpsys package "$1" 2>/dev/null | tr -d '\r'
}

package_pid() {
  adb -s "$DEVICE" shell pidof "$1" 2>/dev/null | tr -d '\r' | awk '{print $1}' || true
}

permission_granted() {
  package_dump "$1" | awk -v permission="$SHARED_RUNTIME_PERMISSION" '
    index($0, permission) && /granted=true/ { found=1 }
    END { exit(found ? 0 : 1) }
  '
}

resolve_host_service() {
  adb -s "$DEVICE" shell cmd package resolve-service --brief -n "$HOST_SERVICE_COMPONENT" 2>/dev/null \
    | tr -d '\r' \
    | head -n 1 || true
}

launch_packages() {
  if [[ "$RESTART" == true ]]; then
    info "Restart requested: force-stopping both apps without clearing data."
    adb -s "$DEVICE" shell am force-stop "$APP_PACKAGE" >/dev/null
    adb -s "$DEVICE" shell am force-stop "$HOST_PACKAGE" >/dev/null
  fi
  adb -s "$DEVICE" shell monkey -p "$HOST_PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
  sleep 1
  adb -s "$DEVICE" shell monkey -p "$APP_PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
  sleep 2
}

capture_service_state() {
  adb -s "$DEVICE" shell dumpsys activity services "$HOST_PACKAGE" 2>/dev/null \
    | tr -d '\r' \
    | grep -E "ServiceRecord|HarnessSharedRuntimeService|$HOST_PACKAGE|$APP_PACKAGE|Connections|binding" \
    | head -n 200 > "$SERVICE_STATE" || true

  if grep -Fq "HarnessSharedRuntimeService" "$SERVICE_STATE"; then
    pass "Harness shared-runtime service is present in ActivityManager service state."
  else
    warn "Harness shared-runtime service is not visible in ActivityManager service state."
  fi

  if grep -Fq "$APP_PACKAGE" "$SERVICE_STATE"; then
    pass "ActivityManager shows RedactGuard in the shared-runtime service state."
  else
    warn "ActivityManager does not expose a RedactGuard binding line in the filtered service state."
  fi
}

capture_redactguard_technical_log() {
  local pid="$1"
  local start_epoch="$2"
  [[ -n "$pid" ]] || return 0
  adb -s "$DEVICE" logcat -d -v epoch --pid="$pid" 'RG_LOCAL_AI:I' '*:S' 2>/dev/null \
    | awk -v start="$start_epoch" '$1 ~ /^[0-9]+\.[0-9]+$/ && ($1 + 0) >= (start + 0)' \
    | grep -F 'RG_LOCAL_AI' || true
}

classify_technical_log() {
  : > "$DIAGNOSIS"

  local rejected_line rejected_reason rejected_step
  rejected_line="$(grep -E 'step=control-plane\.[A-Za-z0-9._:+-]+ result=REJECTED reason=[A-Za-z0-9._:+-]+' "$TECHNICAL_LOG" | tail -n 1 || true)"
  if [[ -n "$rejected_line" ]]; then
    rejected_step="$(printf '%s\n' "$rejected_line" | sed -n -E 's/.*step=([^ ]+).*/\1/p')"
    rejected_reason="$(printf '%s\n' "$rejected_line" | sed -n -E 's/.*reason=([^ ]+).*/\1/p')"
    printf 'status=CONTROL_PLANE_REJECTED\nstep=%s\nreason=%s\n' "$rejected_step" "$rejected_reason" | tee "$DIAGNOSIS"
    fail "Control Plane rejected $rejected_step with reason=$rejected_reason"
    return 0
  fi

  if grep -Eq 'step=control-plane\.[A-Za-z0-9._:+-]+ result=(FAILED|INCOMPATIBLE)' "$TECHNICAL_LOG"; then
    local failure_line
    failure_line="$(grep -E 'step=control-plane\.[A-Za-z0-9._:+-]+ result=(FAILED|INCOMPATIBLE)' "$TECHNICAL_LOG" | tail -n 1)"
    printf 'status=CONTROL_PLANE_FAILURE\nevent=%s\n' "$failure_line" | tee "$DIAGNOSIS"
    fail "Control Plane emitted a classified failure; see diagnosis.txt."
    return 0
  fi

  if grep -Fq 'step=control-plane.activate result=ACTIVATED' "$TECHNICAL_LOG"; then
    printf 'status=ACTIVATION_SUCCEEDED\n' | tee "$DIAGNOSIS"
    pass "Control Plane activation succeeded during reproduction."
    return 0
  fi

  if grep -Fq 'step=transport result=' "$TECHNICAL_LOG"; then
    printf 'status=TRANSPORT_ONLY\n' | tee "$DIAGNOSIS"
    warn "Transport diagnostics were emitted but no terminal Control Plane event was captured."
    return 0
  fi

  printf 'status=NO_RG_LOCAL_AI_EVENTS\n' | tee "$DIAGNOSIS"
  warn "No RG_LOCAL_AI events were captured. The installed RedactGuard may predate this diagnostic instrumentation."
}

run_snapshot() {
  echo "RedactGuard Local AI connected-device diagnostics"
  echo "Run ID: $RUN_ID"
  echo "Mode:   $MODE"
  echo "Device: $DEVICE"
  echo "Output: $OUTPUT_DIR"
  echo
  echo "Privacy boundary: only bounded technical identity is captured; no UI/document/prompt/output/Binder payload data."

  if [[ -f "$ROOT_DIR/scripts/test-redactguard-connected-device.sh" ]]; then
    echo
    echo "== Existing installed-build inspection =="
    local inspect_args=(inspect --device "$DEVICE")
    if [[ "$RELEASE" == false ]]; then inspect_args+=(--debug); fi
    bash "$ROOT_DIR/scripts/test-redactguard-connected-device.sh" "${inspect_args[@]}" || warn "Existing inspect helper returned non-zero."
  fi

  package_installed "$HOST_PACKAGE" || { fail "Harness package is not installed: $HOST_PACKAGE"; exit 4; }
  package_installed "$APP_PACKAGE" || { fail "RedactGuard package is not installed: $APP_PACKAGE"; exit 4; }

  echo
  echo "== Signature permission / service contract =="
  if permission_granted "$APP_PACKAGE"; then
    pass "RedactGuard has $SHARED_RUNTIME_PERMISSION granted."
  else
    fail "RedactGuard does not show $SHARED_RUNTIME_PERMISSION as granted."
  fi

  local resolved_service
  resolved_service="$(resolve_host_service)"
  if [[ "$resolved_service" == *"HarnessSharedRuntimeService"* ]]; then
    pass "PackageManager resolves the explicit Harness shared-runtime service."
  else
    warn "PackageManager resolve-service did not return the explicit Host component; runtime ActivityManager state will be authoritative for this snapshot."
  fi

  echo
  echo "== Launch / Binder service state =="
  launch_packages
  local host_pid app_pid
  host_pid="$(package_pid "$HOST_PACKAGE")"
  app_pid="$(package_pid "$APP_PACKAGE")"
  [[ -n "$host_pid" ]] && pass "Harness process running (pid=$host_pid)." || fail "Harness process is not running."
  [[ -n "$app_pid" ]] && pass "RedactGuard process running (pid=$app_pid)." || fail "RedactGuard process is not running."
  capture_service_state

  echo
  echo "Snapshot files:"
  echo "- report:        $REPORT"
  echo "- service state: $SERVICE_STATE"
  info "A live ActivityManager service/binding record supersedes package-dump string matching for service-presence diagnosis."
}

run_reproduce() {
  run_snapshot

  local start_epoch app_pid_before app_pid_after host_pid_before host_pid_after
  start_epoch="$(adb -s "$DEVICE" shell date +%s | tr -d '\r')"
  app_pid_before="$(package_pid "$APP_PACKAGE")"
  host_pid_before="$(package_pid "$HOST_PACKAGE")"

  echo
  echo "== Reproduce Local AI failure =="
  echo "On the phone, trigger the failing RedactGuard analysis (or tap 'Riprova')."
  echo "When the error is visible again, return here and press Enter."
  read -r _
  sleep 1

  app_pid_after="$(package_pid "$APP_PACKAGE")"
  host_pid_after="$(package_pid "$HOST_PACKAGE")"
  : > "$TECHNICAL_LOG"
  {
    capture_redactguard_technical_log "$app_pid_before" "$start_epoch"
    if [[ -n "$app_pid_after" && "$app_pid_after" != "$app_pid_before" ]]; then
      capture_redactguard_technical_log "$app_pid_after" "$start_epoch"
    fi
  } | awk '!seen[$0]++' | tee "$TECHNICAL_LOG"

  echo
  if [[ -n "$host_pid_before" && -z "$host_pid_after" ]]; then
    fail "Harness process disappeared during reproduction."
  elif [[ -n "$host_pid_after" && "$host_pid_before" != "$host_pid_after" ]]; then
    warn "Harness process PID changed during reproduction ($host_pid_before -> $host_pid_after)."
  else
    pass "Harness process remained alive during reproduction (pid=${host_pid_after:-unknown})."
  fi

  if [[ -n "$app_pid_before" && -z "$app_pid_after" ]]; then
    fail "RedactGuard process disappeared during reproduction."
  elif [[ -n "$app_pid_after" && "$app_pid_before" != "$app_pid_after" ]]; then
    warn "RedactGuard process PID changed during reproduction ($app_pid_before -> $app_pid_after)."
  else
    pass "RedactGuard process remained alive during reproduction (pid=${app_pid_after:-unknown})."
  fi

  capture_service_state
  echo
  echo "== Classified diagnosis =="
  classify_technical_log

  echo
  echo "Reproduction files:"
  echo "- report:         $REPORT"
  echo "- technical log:  $TECHNICAL_LOG"
  echo "- service state:  $SERVICE_STATE"
  echo "- diagnosis:      $DIAGNOSIS"
}

case "$MODE" in
  snapshot) run_snapshot ;;
  reproduce) run_reproduce ;;
esac
