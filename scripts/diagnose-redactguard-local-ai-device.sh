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
    Diagnostic snapshot of the installed Harness + RedactGuard pair. Checks package
    identity, signature-gated permission, declared/bound Host service, process state,
    and (when run-as is allowed) the persisted Harness control-plane DB.

  reproduce
    Runs snapshot first, then asks you to reproduce the RedactGuard Local AI failure on
    the phone and captures only technical log lines from the two target app processes.
    No UI dump, document text, prompts, model output, or Binder payload is collected.

Options:
  --device SERIAL     Required unless REDACTGUARD_DEVICE is set.
  --debug             Use .debug package identities and debug shared-runtime permission.
  --restart           Force-stop both apps before relaunch. App data is NOT cleared.
  --output-dir PATH   Override the local evidence directory.

Important:
  This script never runs pm clear, uninstall, or any command that deletes app data.
  It launches the installed apps as needed. Prefer synthetic/non-sensitive input when
  reproducing the failure.
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

if [[ "$MODE" == "help" || "$MODE" == "-h" || "$MODE" == "--help" ]]; then
  usage
  exit 0
fi
case "$MODE" in
  snapshot|reproduce) ;;
  *) echo "Unknown mode: $MODE" >&2; usage >&2; exit 2 ;;
esac

[[ -n "$DEVICE" ]] || { echo "--device is required (or set REDACTGUARD_DEVICE)." >&2; exit 2; }
command -v adb >/dev/null || { echo "adb is required on PATH." >&2; exit 2; }
command -v python3 >/dev/null || { echo "python3 is required on PATH." >&2; exit 2; }
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
CONTROL_PLANE_DB="harness-control-plane.db"
EXPECTED_APPLICATION_ID="redactguard"
EXPECTED_USE_CASE_ID="document-pii-detection"

RUN_ID="local-ai-diagnostics-$(date -u +%Y%m%dT%H%M%SZ)-$$"
if [[ -z "$OUTPUT_DIR" ]]; then
  OUTPUT_DIR="$ROOT_DIR/evidence/local/local-ai-diagnostics/$RUN_ID"
fi
mkdir -p "$OUTPUT_DIR"
REPORT="$OUTPUT_DIR/report.txt"
TECHNICAL_LOG="$OUTPUT_DIR/technical-log.txt"
SERVICE_STATE="$OUTPUT_DIR/service-state.txt"
CONTROL_PLANE_REPORT="$OUTPUT_DIR/control-plane.txt"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
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

package_version_code() {
  package_dump "$1" | sed -n 's/.*versionCode=\([0-9][0-9]*\).*/\1/p' | head -n 1
}

package_version_name() {
  package_dump "$1" | sed -n 's/^[[:space:]]*versionName=//p' | head -n 1
}

package_update_time() {
  package_dump "$1" | sed -n 's/^[[:space:]]*lastUpdateTime=//p' | head -n 1
}

package_uid() {
  package_dump "$1" | sed -n 's/^[[:space:]]*userId=\([0-9][0-9]*\).*/\1/p' | head -n 1
}

package_pid() {
  adb -s "$DEVICE" shell pidof "$1" 2>/dev/null | tr -d '\r' | awk '{print $1}' || true
}

installed_apk_sha256() {
  local package="$1"
  local remote local_apk
  remote="$(package_path "$package")"
  [[ -n "$remote" ]] || return 0
  local_apk="$TMP_DIR/${package//./_}.apk"
  adb -s "$DEVICE" pull "$remote" "$local_apk" >/dev/null 2>&1 || return 0
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$local_apk" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$local_apk" | awk '{print $1}'
  fi
}

permission_granted() {
  package_dump "$1" | awk -v permission="$SHARED_RUNTIME_PERMISSION" '
    index($0, permission) && /granted=true/ { found=1 }
    END { exit(found ? 0 : 1) }
  '
}

service_declared() {
  package_dump "$HOST_PACKAGE" | awk -v service="$HOST_SERVICE_CLASS" '
    index($0, service) { found=1 }
    END { exit(found ? 0 : 1) }
  '
}

run_as_available() {
  adb -s "$DEVICE" shell run-as "$HOST_PACKAGE" sh -c 'test -r databases/'"$CONTROL_PLANE_DB" 2>/dev/null
}

copy_run_as_file() {
  local remote="$1"
  local destination="$2"
  adb -s "$DEVICE" exec-out run-as "$HOST_PACKAGE" cat "$remote" > "$destination" 2>/dev/null
}

inspect_control_plane_db() {
  : > "$CONTROL_PLANE_REPORT"
  if ! run_as_available; then
    warn "Harness private DB: run-as unavailable; persisted control-plane rows cannot be inspected from adb on this build."
    printf 'run-as unavailable; direct persisted control-plane inspection: N/A\n' > "$CONTROL_PLANE_REPORT"
    return 0
  fi

  local db="$TMP_DIR/$CONTROL_PLANE_DB"
  copy_run_as_file "databases/$CONTROL_PLANE_DB" "$db" || {
    warn "Harness private DB: base database could not be copied with run-as."
    printf 'run-as available but database copy failed\n' > "$CONTROL_PLANE_REPORT"
    return 0
  }
  copy_run_as_file "databases/$CONTROL_PLANE_DB-wal" "$db-wal" || rm -f "$db-wal"
  copy_run_as_file "databases/$CONTROL_PLANE_DB-shm" "$db-shm" || rm -f "$db-shm"

  DB_PATH="$db" EXPECTED_APPLICATION_ID="$EXPECTED_APPLICATION_ID" EXPECTED_USE_CASE_ID="$EXPECTED_USE_CASE_ID" \
    python3 <<'PY' | tee "$CONTROL_PLANE_REPORT" || warn "Harness private DB query failed; treating direct DB evidence as inconclusive."
import os
import sqlite3
from pathlib import Path

path = Path(os.environ["DB_PATH"])
app_id = os.environ["EXPECTED_APPLICATION_ID"]
use_case_id = os.environ["EXPECTED_USE_CASE_ID"]

try:
    conn = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
except sqlite3.Error as exc:
    print(f"DB_OPEN_ERROR={type(exc).__name__}")
    raise SystemExit(0)

conn.row_factory = sqlite3.Row
try:
    user_version = conn.execute("PRAGMA user_version").fetchone()[0]
except sqlite3.Error as exc:
    print(f"DB_QUERY_ERROR={type(exc).__name__}")
    conn.close()
    raise SystemExit(0)
print(f"user_version={user_version}")
expected_tables = [
    "hcp_applications",
    "hcp_use_case_revisions",
    "hcp_preset_revisions",
    "hcp_binding_revisions",
    "hcp_preset_exposures",
]
existing = {
    row[0] for row in conn.execute("SELECT name FROM sqlite_master WHERE type='table'")
}
for table in expected_tables:
    if table not in existing:
        print(f"table.{table}=MISSING")
        continue
    count = conn.execute(f'SELECT COUNT(*) FROM "{table}"').fetchone()[0]
    print(f"table.{table}.count={count}")

if "hcp_applications" in existing:
    rows = conn.execute(
        "SELECT application_id, package_name, state FROM hcp_applications WHERE application_id = ?",
        (app_id,),
    ).fetchall()
    if not rows:
        print("redactguard.application=MISSING")
    for row in rows:
        print(
            "redactguard.application="
            f"id:{row['application_id']},package:{row['package_name']},state:{row['state']}"
        )

if "hcp_use_case_revisions" in existing:
    rows = conn.execute(
        "SELECT use_case_id, revision, state FROM hcp_use_case_revisions "
        "WHERE use_case_id = ? ORDER BY revision DESC",
        (use_case_id,),
    ).fetchall()
    if not rows:
        print("redactguard.use_case=MISSING")
    for row in rows:
        print(
            "redactguard.use_case="
            f"id:{row['use_case_id']},revision:{row['revision']},state:{row['state']}"
        )

if "hcp_binding_revisions" in existing:
    rows = conn.execute(
        "SELECT binding_id, revision, application_id, use_case_id, enabled, is_default "
        "FROM hcp_binding_revisions WHERE application_id = ? AND use_case_id = ? "
        "ORDER BY revision DESC",
        (app_id, use_case_id),
    ).fetchall()
    if not rows:
        print("redactguard.binding=MISSING")
    for row in rows:
        print(
            "redactguard.binding="
            f"id:{row['binding_id']},revision:{row['revision']},enabled:{row['enabled']},default:{row['is_default']}"
        )

if "hcp_preset_revisions" in existing:
    rows = conn.execute(
        "SELECT preset_id, revision, state, inference_preset_id, inference_preset_version "
        "FROM hcp_preset_revisions WHERE use_case_id = ? ORDER BY preset_id, revision DESC",
        (use_case_id,),
    ).fetchall()
    if not rows:
        print("redactguard.presets=MISSING")
    for row in rows:
        print(
            "redactguard.preset="
            f"id:{row['preset_id']},revision:{row['revision']},state:{row['state']},"
            f"runtimePreset:{row['inference_preset_id']}@{row['inference_preset_version']}"
        )

if "hcp_preset_exposures" in existing and "hcp_binding_revisions" in existing:
    rows = conn.execute(
        "SELECT e.binding_id, e.binding_revision, e.preset_id, e.preset_revision, e.is_default "
        "FROM hcp_preset_exposures e "
        "JOIN hcp_binding_revisions b "
        "ON b.binding_id = e.binding_id AND b.revision = e.binding_revision "
        "WHERE b.application_id = ? AND b.use_case_id = ? "
        "ORDER BY e.binding_revision DESC, e.preset_id",
        (app_id, use_case_id),
    ).fetchall()
    if not rows:
        print("redactguard.exposures=MISSING")
    for row in rows:
        print(
            "redactguard.exposure="
            f"binding:{row['binding_id']}@{row['binding_revision']},"
            f"preset:{row['preset_id']}@{row['preset_revision']},default:{row['is_default']}"
        )
conn.close()
PY

  if grep -Eq 'redactguard\.(application|use_case|binding|presets|exposures)=MISSING|table\..*=MISSING|DB_(OPEN|QUERY)_ERROR=|Traceback|sqlite3\.' "$CONTROL_PLANE_REPORT"; then
    warn "Harness private DB: control-plane snapshot contains missing or unreadable required state; see $CONTROL_PLANE_REPORT"
  else
    pass "Harness private DB: RedactGuard application/use-case/binding/preset exposure rows are present."
  fi
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
    | grep -E "ServiceRecord|$HOST_SERVICE_CLASS|$HOST_PACKAGE|$APP_PACKAGE|permission|Connections|binding" \
    | head -n 160 > "$SERVICE_STATE" || true

  if grep -Fq "$HOST_SERVICE_CLASS" "$SERVICE_STATE"; then
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

capture_process_technical_log() {
  local package="$1"
  local pid="$2"
  local start_epoch="$3"
  [[ -n "$pid" ]] || return 0
  adb -s "$DEVICE" logcat -d -v epoch --pid="$pid" 2>/dev/null \
    | awk -v start="$start_epoch" '$1 ~ /^[0-9]+\.[0-9]+$/ && ($1 + 0) >= (start + 0)' \
    | grep -Ei 'AndroidRuntime|RuntimeException|Exception|Binder|Parcel|Parcelable|ControlPlane|assigned|discoverUseCases|SharedRuntime|LocalAi|SecurityException|DeadObject|RemoteException|Illegal(State|Argument)|ClassNotFound|NoSuch(Method|Field)|transaction|permission' \
    | sed -E 's/(document|prompt|output|text|content)=([^ ,;]+)/\1=<redacted>/Ig' \
    | sed "s/^/[$package] /" || true
}

print_package_summary() {
  local label="$1"
  local package="$2"
  local sha
  echo
  echo "== $label installed artifact =="
  echo "Package:        $package"
  echo "versionCode:    $(package_version_code "$package")"
  echo "versionName:    $(package_version_name "$package")"
  echo "lastUpdateTime: $(package_update_time "$package")"
  echo "uid:            $(package_uid "$package")"
  sha="$(installed_apk_sha256 "$package")"
  echo "APK SHA-256:    ${sha:-unavailable}"
}

run_snapshot() {
  echo "RedactGuard Local AI connected-device diagnostics"
  echo "Run ID: $RUN_ID"
  echo "Mode:   $MODE"
  echo "Device: $DEVICE"
  echo "Output: $OUTPUT_DIR"
  echo
  echo "Privacy boundary: no UI dump, document text, prompts, model output, or Binder payload capture."

  if [[ -x "$ROOT_DIR/scripts/test-redactguard-connected-device.sh" || -f "$ROOT_DIR/scripts/test-redactguard-connected-device.sh" ]]; then
    echo
    echo "== Existing installed-build inspection =="
    local inspect_args=(inspect --device "$DEVICE")
    if [[ "$RELEASE" == false ]]; then inspect_args+=(--debug); fi
    bash "$ROOT_DIR/scripts/test-redactguard-connected-device.sh" "${inspect_args[@]}" || warn "Existing inspect helper returned non-zero."
  fi

  package_installed "$HOST_PACKAGE" || { fail "Harness package is not installed: $HOST_PACKAGE"; exit 4; }
  package_installed "$APP_PACKAGE" || { fail "RedactGuard package is not installed: $APP_PACKAGE"; exit 4; }
  print_package_summary "Harness" "$HOST_PACKAGE"
  print_package_summary "RedactGuard" "$APP_PACKAGE"

  echo
  echo "== Signature permission / service contract =="
  if service_declared; then
    pass "Harness declares $HOST_SERVICE_CLASS"
  else
    fail "Harness service declaration not found in installed package dump: $HOST_SERVICE_CLASS"
  fi
  if permission_granted "$APP_PACKAGE"; then
    pass "RedactGuard has $SHARED_RUNTIME_PERMISSION granted."
  else
    fail "RedactGuard does not show $SHARED_RUNTIME_PERMISSION as granted."
  fi
  if permission_granted "$HOST_PACKAGE"; then
    pass "Harness has its shared-runtime permission granted."
  else
    warn "Harness package dump does not show its own shared-runtime permission as granted."
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
  echo "== Persisted Harness control plane =="
  inspect_control_plane_db

  echo
  echo "Snapshot files:"
  echo "- report:        $REPORT"
  echo "- service state: $SERVICE_STATE"
  echo "- control plane: $CONTROL_PLANE_REPORT"
  echo
  info "ADB alone cannot directly invoke the signature-gated Consumer SDK assignedUseCases() call."
  info "The reproduce mode exercises that call through RedactGuard itself and captures surrounding technical evidence."
}

run_reproduce() {
  run_snapshot
  local start_epoch host_pid_before app_pid_before host_pid_after app_pid_after
  start_epoch="$(adb -s "$DEVICE" shell date +%s | tr -d '\r')"
  host_pid_before="$(package_pid "$HOST_PACKAGE")"
  app_pid_before="$(package_pid "$APP_PACKAGE")"

  echo
  echo "== Reproduce control-plane.assigned-use-cases =="
  echo "On the phone, use synthetic/non-sensitive input if possible."
  echo "Open RedactGuard and trigger the failing analysis (or tap 'Riprova')."
  echo "When the error is visible again, return here and press Enter."
  read -r _
  sleep 1

  host_pid_after="$(package_pid "$HOST_PACKAGE")"
  app_pid_after="$(package_pid "$APP_PACKAGE")"
  : > "$TECHNICAL_LOG"
  {
    capture_process_technical_log "$APP_PACKAGE" "$app_pid_before" "$start_epoch"
    if [[ "$app_pid_after" != "$app_pid_before" ]]; then
      capture_process_technical_log "$APP_PACKAGE" "$app_pid_after" "$start_epoch"
    fi
    capture_process_technical_log "$HOST_PACKAGE" "$host_pid_before" "$start_epoch"
    if [[ "$host_pid_after" != "$host_pid_before" ]]; then
      capture_process_technical_log "$HOST_PACKAGE" "$host_pid_after" "$start_epoch"
    fi
  } | awk '!seen[$0]++' | tee "$TECHNICAL_LOG"

  echo
  if [[ -s "$TECHNICAL_LOG" ]]; then
    pass "Technical target-process log evidence captured."
  else
    warn "No matching technical log lines were emitted by the target processes."
  fi
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
  echo "Reproduction files:"
  echo "- report:        $REPORT"
  echo "- technical log: $TECHNICAL_LOG"
  echo "- service state: $SERVICE_STATE"
  echo "- control plane: $CONTROL_PLANE_REPORT"
  echo
  echo "Paste report.txt + technical-log.txt here; they contain only the technical diagnostic surface collected by this script."
}

case "$MODE" in
  snapshot) run_snapshot ;;
  reproduce) run_reproduce ;;
esac
