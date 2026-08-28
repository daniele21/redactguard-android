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
avd_home="${PWD}/emulator-ci/avd"
emulator_pid=""
accel_mode="${ANDROID_EMULATOR_ACCEL_MODE:-auto}"

mkdir -p "$(dirname "$log_path")"

cleanup() {
  local status=$?
  trap - EXIT

  if adb -s "$serial" get-state >/dev/null 2>&1; then
    adb -s "$serial" emu kill >/dev/null 2>&1 || true
  fi
  if [[ -n "$emulator_pid" ]] && kill -0 "$emulator_pid" >/dev/null 2>&1; then
    for _ in $(seq 1 20); do
      kill -0 "$emulator_pid" >/dev/null 2>&1 || break
      sleep 1
    done
    if kill -0 "$emulator_pid" >/dev/null 2>&1; then
      kill "$emulator_pid" >/dev/null 2>&1 || true
    fi
  fi
  if [[ -n "$emulator_pid" ]]; then
    wait "$emulator_pid" >/dev/null 2>&1 || true
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

  rm -rf "$avd_home"
  exit "$status"
}
trap cleanup EXIT

adb start-server >/dev/null

echo "Preparing Android ${api_level} ${arch} emulator image"
printf 'y\n%.0s' {1..20} | sdkmanager --licenses >/dev/null
sdkmanager --install \
  "platforms;android-${api_level}" \
  platform-tools \
  emulator \
  "$system_image" >/dev/null

export ANDROID_AVD_HOME="$avd_home"
rm -rf "$avd_home"
mkdir -p "$avd_home"

echo "Creating isolated AVD $avd_name in $ANDROID_AVD_HOME"
echo no | avdmanager create avd \
  --force \
  --name "$avd_name" \
  --package "$system_image" \
  --device "$device_profile" >/dev/null

emulator_bin="$ANDROID_SDK_ROOT/emulator/emulator"
if ! "$emulator_bin" -list-avds | grep -Fxq "$avd_name"; then
  echo "AVD $avd_name was not registered in $ANDROID_AVD_HOME" >&2
  find "$ANDROID_AVD_HOME" -maxdepth 2 -type f -print >&2 || true
  exit 1
fi

if [[ "$accel_mode" == "auto" ]]; then
  accel_mode="off"
  if [[ -e /dev/kvm ]]; then
    if [[ ! -r /dev/kvm || ! -w /dev/kvm ]]; then
      if command -v sudo >/dev/null 2>&1; then
        sudo -n chmod a+rw /dev/kvm >/dev/null 2>&1 || true
      fi
    fi
    if [[ -r /dev/kvm && -w /dev/kvm ]]; then
      accel_mode="on"
    fi
  fi
fi
if [[ "$accel_mode" != "on" && "$accel_mode" != "off" ]]; then
  echo "ANDROID_EMULATOR_ACCEL_MODE must be auto, on, or off" >&2
  exit 2
fi
if [[ "$accel_mode" == "on" && ( ! -r /dev/kvm || ! -w /dev/kvm ) ]]; then
  echo "KVM acceleration was requested but /dev/kvm is not readable and writable" >&2
  exit 1
fi

echo "Starting Android emulator $avd_name with acceleration=$accel_mode"
"$emulator_bin" \
  -port 5554 \
  -avd "$avd_name" \
  -no-window \
  -gpu swiftshader_indirect \
  -no-snapshot \
  -noaudio \
  -no-boot-anim \
  -accel "$accel_mode" \
  >"$log_path" 2>&1 &
emulator_pid=$!

export ANDROID_SERIAL="$serial"
echo "Waiting for $serial to finish booting"
boot_started_at="$(date +%s)"
while true; do
  if ! kill -0 "$emulator_pid" >/dev/null 2>&1; then
    echo "Android emulator exited before becoming ready" >&2
    wait "$emulator_pid" >/dev/null 2>&1 || true
    tail -n 200 "$log_path" >&2 || true
    exit 1
  fi

  device_state="$(adb -s "$serial" get-state 2>/dev/null || true)"
  if [[ "$device_state" == "device" ]]; then
    boot_completed="$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    [[ "$boot_completed" == "1" ]] && break
  fi

  if (( $(date +%s) - boot_started_at >= boot_timeout_seconds )); then
    echo "Android emulator did not finish booting within ${boot_timeout_seconds}s" >&2
    tail -n 200 "$log_path" >&2 || true
    exit 1
  fi
  sleep 2
done

# Instrumentation does not require an interactive unlock. Animation changes are best-effort only.
adb -s "$serial" shell settings put global window_animation_scale 0 >/dev/null 2>&1 || true
adb -s "$serial" shell settings put global transition_animation_scale 0 >/dev/null 2>&1 || true
adb -s "$serial" shell settings put global animator_duration_scale 0 >/dev/null 2>&1 || true
sleep 5

"$@"
