#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" == "--" ]]; then
  shift
fi
if [[ "$#" -eq 0 ]]; then
  echo "usage: $0 -- <command> [args...]" >&2
  exit 2
fi

: "${ANDROID_SDK_ROOT:=${ANDROID_HOME:-}}"
if [[ -z "$ANDROID_SDK_ROOT" ]]; then
  echo "ANDROID_SDK_ROOT or ANDROID_HOME must be set" >&2
  exit 2
fi

api_level="${ANDROID_EMULATOR_API_LEVEL:-35}"
arch="${ANDROID_EMULATOR_ARCH:-x86_64}"
avd_name="${ANDROID_EMULATOR_AVD_NAME:-redactguard-ci}"
device_profile="${ANDROID_EMULATOR_DEVICE_PROFILE:-pixel_6}"
serial="${ANDROID_EMULATOR_SERIAL:-emulator-5554}"
boot_timeout_seconds="${ANDROID_EMULATOR_BOOT_TIMEOUT_SECONDS:-600}"
log_path="${ANDROID_EMULATOR_LOG:-emulator-ci/emulator.log}"
system_image="system-images;android-${api_level};default;${arch}"

mkdir -p "$(dirname "$log_path")"

cleanup() {
  local status=$?
  if adb -s "$serial" get-state >/dev/null 2>&1; then
    adb -s "$serial" emu kill >/dev/null 2>&1 || true
  fi
  for _ in $(seq 1 20); do
    if ! adb devices | awk 'NR > 1 {print $1}' | grep -Fxq "$serial"; then
      break
    fi
    sleep 1
  done
  if adb devices | awk 'NR > 1 {print $1}' | grep -Fxq "$serial"; then
    echo "Emulator $serial remained visible after cleanup" >&2
    [[ "$status" -ne 0 ]] || status=1
  fi
  return "$status"
}
trap cleanup EXIT

adb start-server >/dev/null
printf 'y\n%.0s' {1..20} | sdkmanager --licenses >/dev/null
sdkmanager --install \
  "platforms;android-${api_level}" \
  platform-tools \
  emulator \
  "$system_image" >/dev/null

echo no | avdmanager create avd \
  --force \
  --name "$avd_name" \
  --package "$system_image" \
  --device "$device_profile" >/dev/null

"$ANDROID_SDK_ROOT/emulator/emulator" \
  -port 5554 \
  -avd "$avd_name" \
  -no-window \
  -gpu swiftshader_indirect \
  -no-snapshot \
  -noaudio \
  -no-boot-anim \
  -accel off \
  >"$log_path" 2>&1 &

export ANDROID_SERIAL="$serial"
adb wait-for-device

boot_started_at="$(date +%s)"
while true; do
  boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  [[ "$boot_completed" == "1" ]] && break
  if (( $(date +%s) - boot_started_at >= boot_timeout_seconds )); then
    echo "Android emulator did not finish booting within ${boot_timeout_seconds}s" >&2
    tail -n 200 "$log_path" >&2 || true
    exit 1
  fi
  sleep 2
done

# Instrumentation does not require an interactive unlock. Animation changes are best-effort only.
adb shell settings put global window_animation_scale 0 >/dev/null 2>&1 || true
adb shell settings put global transition_animation_scale 0 >/dev/null 2>&1 || true
adb shell settings put global animator_duration_scale 0 >/dev/null 2>&1 || true
sleep 5

"$@"
