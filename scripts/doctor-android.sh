#!/usr/bin/env bash
set -euo pipefail

required_java_major=17

command -v java >/dev/null || { echo "java: missing"; exit 1; }
command -v adb >/dev/null || { echo "adb: missing"; exit 1; }

java_version="$(java -version 2>&1 | head -1)"
echo "java: ${java_version}"
echo "adb: $(adb version | head -1)"

if [[ -z "${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}" ]]; then
  echo "android sdk: ANDROID_SDK_ROOT/ANDROID_HOME missing"
  exit 1
fi

echo "android sdk: ${ANDROID_SDK_ROOT:-${ANDROID_HOME}}"
echo "expected: JDK ${required_java_major}, compile/target SDK 36"
