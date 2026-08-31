#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_SERIAL:?ANDROID_SERIAL must identify the running emulator}"

evidence_root="${REDACTGUARD_E2E_EVIDENCE_DIR:-emulator-e2e-evidence}"
screenshot_root="$evidence_root/screenshots"
video_root="$evidence_root/videos"
additional_output_root='app/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected'
ui_test_class='io.github.daniele21.redactguard.ui.ProductJourneyUiEvidenceInstrumentationTest'
mkdir -p "$screenshot_root" "$video_root"

copy_additional_output() {
  if [[ -d "$additional_output_root" ]]; then
    while IFS= read -r -d '' file; do
      cp "$file" "$screenshot_root/$(basename "$file")"
    done < <(find "$additional_output_root" -type f \( -name '*.png' -o -name '*.json' \) -print0)
  fi
}

stop_and_pull_recording() {
  local remote_video="$1"
  local local_video="$2"
  local record_pid
  record_pid="$(adb -s "$ANDROID_SERIAL" shell pidof screenrecord 2>/dev/null | tr -d '\r' || true)"
  if [[ -n "$record_pid" ]]; then
    adb -s "$ANDROID_SERIAL" shell kill -2 "$record_pid" >/dev/null 2>&1 || true
    sleep 2
  fi
  adb -s "$ANDROID_SERIAL" pull "$remote_video" "$local_video" >/dev/null 2>&1 || true
  adb -s "$ANDROID_SERIAL" shell rm -f "$remote_video" >/dev/null 2>&1 || true
}

run_ui_journey() {
  local journey="$1"
  local method="$2"
  local remote_video="/sdcard/redactguard-${journey}.mp4"
  local local_video="$video_root/${journey}.mp4"

  echo "== UI E2E journey: $journey ($method) =="
  rm -rf "$additional_output_root"
  adb -s "$ANDROID_SERIAL" shell rm -f "$remote_video" || true
  adb -s "$ANDROID_SERIAL" shell "screenrecord --bit-rate 4000000 --time-limit 180 '$remote_video' >/dev/null 2>&1 &"

  for _ in $(seq 1 20); do
    if [[ -n "$(adb -s "$ANDROID_SERIAL" shell pidof screenrecord 2>/dev/null | tr -d '\r' || true)" ]]; then
      break
    fi
    sleep 0.25
  done
  if [[ -z "$(adb -s "$ANDROID_SERIAL" shell pidof screenrecord 2>/dev/null | tr -d '\r' || true)" ]]; then
    echo "E2E_EVIDENCE_INCOMPLETE: screenrecord did not start for $journey" >&2
    return 1
  fi

  set +e
  ./gradlew --no-daemon :app:connectedDebugAndroidTest \
    "-Pandroid.testInstrumentationRunnerArguments.class=${ui_test_class}#${method}"
  local test_status=$?
  set -e

  stop_and_pull_recording "$remote_video" "$local_video"
  copy_additional_output

  if [[ $test_status -ne 0 ]]; then
    echo "UI E2E journey failed: $journey" >&2
    return "$test_status"
  fi
  if [[ ! -s "$local_video" ]]; then
    echo "E2E_EVIDENCE_INCOMPLETE: missing video for $journey" >&2
    return 1
  fi
}

# Keep deterministic orchestration/domain coverage separate from the three UI evidence journeys.
./gradlew --no-daemon :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.github.daniele21.redactguard.ProductJourneyInstrumentationTest

run_ui_journey protect-text-document capturePastedTextJourneyCheckpoints
run_ui_journey protect-text-pdf captureTextPdfJourneyCheckpoints
run_ui_journey recover-local-ai captureLocalAiRecoveryJourneyCheckpoints

expected_screenshots=(
  01-text-import
  02-text-protection
  03-text-analysis
  04-text-review-pending
  05-text-review-redacted
  06-text-outcome
  07-pdf-importing
  08-pdf-protection
  09-pdf-analysis
  10-pdf-review-pending
  11-pdf-outcome
  12-recovery-unavailable
  13-recovery-retry-analysis
  14-recovery-review-after-retry
)
for checkpoint in "${expected_screenshots[@]}"; do
  [[ -s "$screenshot_root/$checkpoint.png" ]] || {
    echo "E2E_EVIDENCE_INCOMPLETE: missing checkpoint screenshot $checkpoint.png" >&2
    exit 1
  }
  [[ -s "$screenshot_root/$checkpoint.json" ]] || {
    echo "E2E_EVIDENCE_INCOMPLETE: missing checkpoint metadata $checkpoint.json" >&2
    exit 1
  }
done
for journey in protect-text-document protect-text-pdf recover-local-ai; do
  [[ -s "$video_root/$journey.mp4" ]] || {
    echo "E2E_EVIDENCE_INCOMPLETE: missing video $journey.mp4" >&2
    exit 1
  }
done

cat > "$evidence_root/ui-media-manifest.txt" <<EOF
schema_version=1
evidence_kind=redactguard_android_ui_e2e_media_v1
source_revision=${REDACTGUARD_SOURCE_REVISION:-$(git rev-parse HEAD)}
workflow_run=${GITHUB_RUN_ID:-local}
execution_environment=emulator-product-journeys
fidelity_class=simulated_or_emulated
journeys=protect-text-document,protect-text-pdf,recover-local-ai
screenshots=14
videos=3
EOF

echo "RedactGuard UI E2E media evidence complete: 14 screenshots + 3 videos"
