#!/usr/bin/env bash
set -euo pipefail

class_prefix='io.github.daniele21.redactguard.ui'
evidence_root='visual-evidence'
additional_output_root='app/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected'
target_source='design/reference/approved-target.jpg'
target_provenance='design/reference/target-provenance.json'

reset_display() {
  adb shell wm size reset >/dev/null 2>&1 || true
  adb shell wm density reset >/dev/null 2>&1 || true
}

clear_host_additional_output() {
  rm -rf "$additional_output_root"
}

collect_host_evidence() {
  local destination="$1"
  local expected_png_count="$2"
  local png_count

  if [[ ! -d "$additional_output_root" ]]; then
    echo "Gradle did not publish connected Android additional test output" >&2
    exit 1
  fi

  mkdir -p "$destination"
  while IFS= read -r -d '' file; do
    cp "$file" "$destination/$(basename "$file")"
  done < <(
    find "$additional_output_root" -type f \
      \( -name '*.png' -o -name 'metadata.json' \) \
      -print0
  )

  png_count="$(find "$destination" -maxdepth 1 -type f -name '*.png' | wc -l | tr -d ' ')"
  if [[ "$png_count" != "$expected_png_count" ]]; then
    echo "Expected $expected_png_count visual screenshots in $destination, found $png_count" >&2
    find "$additional_output_root" -type f -print >&2 || true
    exit 1
  fi
  if [[ ! -f "$destination/metadata.json" ]]; then
    echo "Missing visual evidence metadata in $destination" >&2
    find "$additional_output_root" -type f -print >&2 || true
    exit 1
  fi
}

trap reset_display EXIT
rm -rf "$evidence_root"
mkdir -p "$evidence_root"

adb shell wm size 1080x2400
adb shell wm density 420
clear_host_additional_output
./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="$class_prefix.VisualReferenceCompactEvidenceInstrumentationTest"
collect_host_evidence "$evidence_root/compact" 6

# The approved adaptive target is explicitly tablet / landscape. Capture the
# expanded Review in that orientation instead of stretching a portrait window.
adb shell wm size 2560x1600
adb shell wm density 320
sleep 2
clear_host_additional_output
./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="$class_prefix.VisualReferenceExpandedEvidenceInstrumentationTest"
collect_host_evidence "$evidence_root/expanded" 1

screenshot_count="$(find "$evidence_root" -type f -name '*.png' | wc -l | tr -d ' ')"
if [[ "$screenshot_count" != '7' ]]; then
  echo "Expected 7 visual reference screenshots, found $screenshot_count" >&2
  find "$evidence_root" -maxdepth 3 -type f -print >&2 || true
  exit 1
fi

java scripts/MaterializeVisualTargetCrops.java "$target_source" "$target_provenance" "$evidence_root/target"
target_count="$(find "$evidence_root/target" -maxdepth 1 -type f -name '*.png' | wc -l | tr -d ' ')"
if [[ "$target_count" != '6' ]]; then
  echo "Expected 6 approved-target crops, found $target_count" >&2
  find "$evidence_root/target" -maxdepth 1 -type f -print >&2 || true
  exit 1
fi

python3 -m py_compile scripts/build-visual-comparison-report.py
python3 scripts/build-visual-comparison-report.py "$evidence_root" "$target_provenance"
[[ -f "$evidence_root/comparison/index.html" ]]
[[ -f "$evidence_root/comparison/manifest.json" ]]
