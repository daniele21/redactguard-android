#!/usr/bin/env bash
set -euo pipefail

package='io.github.daniele21.redactguard.debug'
class_prefix='io.github.daniele21.redactguard.ui'
evidence_root='visual-evidence'

reset_display() {
  adb shell wm size reset >/dev/null 2>&1 || true
  adb shell wm density reset >/dev/null 2>&1 || true
}

clear_device_evidence() {
  adb shell "run-as $package rm -rf files/visual-evidence" >/dev/null 2>&1 || true
}

pull_evidence() {
  local destination="$1"
  mkdir -p "$destination"
  adb shell "run-as $package ls files/visual-evidence" \
    | tr -d '\r' \
    | while IFS= read -r file; do
        [[ -n "$file" ]] || continue
        adb exec-out run-as "$package" cat "files/visual-evidence/$file" > "$destination/$file"
      done
}

trap reset_display EXIT
rm -rf "$evidence_root"
mkdir -p "$evidence_root"

adb shell wm size 1080x2400
adb shell wm density 420
clear_device_evidence
./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="$class_prefix.VisualReferenceCompactEvidenceInstrumentationTest"
pull_evidence "$evidence_root/compact"

clear_device_evidence
adb shell wm size 1600x2560
adb shell wm density 320
sleep 2
./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="$class_prefix.VisualReferenceExpandedEvidenceInstrumentationTest"
pull_evidence "$evidence_root/expanded"

screenshot_count="$(find "$evidence_root" -type f -name '*.png' | wc -l | tr -d ' ')"
if [[ "$screenshot_count" != '7' ]]; then
  echo "Expected 7 visual reference screenshots, found $screenshot_count" >&2
  find "$evidence_root" -maxdepth 3 -type f -print >&2 || true
  exit 1
fi
