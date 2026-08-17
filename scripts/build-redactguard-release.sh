#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SHARED_KEYCHAIN_SERVICE="io.github.daniele21.localllm.phonetest.android-upload"
SHARED_KEYCHAIN_ACCOUNT="local-llm-phone-test-upload"
DEFAULT_STORE_FILE="${HOME}/.keystore/local-llm-phone-test-upload.jks"
DEFAULT_KEY_ALIAS="local-llm-phone-test-upload"
AAB_PATH="${ROOT_DIR}/app/build/outputs/bundle/release/app-release.aab"

usage() {
    cat <<'EOF'
Usage:
  bash scripts/build-redactguard-release.sh check
  bash scripts/build-redactguard-release.sh build

RedactGuard intentionally reuses the existing Harness Play upload key by default.
The upload keystore remains outside the repository and its password is read from
the same macOS Keychain item used by the Harness release helper.

Default shared Harness upload identity:
  keystore: ~/.keystore/local-llm-phone-test-upload.jks
  alias:    local-llm-phone-test-upload

Optional non-secret overrides:
  REDACTGUARD_ANDROID_UPLOAD_STORE_FILE
  REDACTGUARD_ANDROID_UPLOAD_KEY_ALIAS
  ANDROID_HOME

Optional secret overrides for non-Keychain environments:
  REDACTGUARD_ANDROID_UPLOAD_STORE_PASSWORD
  REDACTGUARD_ANDROID_UPLOAD_KEY_PASSWORD
EOF
}

require_macos_keychain() {
    if [[ "$(uname -s)" != "Darwin" ]] || ! command -v security >/dev/null 2>&1; then
        echo "macOS Keychain is unavailable." >&2
        echo "Set both REDACTGUARD_ANDROID_UPLOAD_*_PASSWORD variables explicitly in a secure environment." >&2
        return 1
    fi
}

read_shared_keychain_password() {
    require_macos_keychain >/dev/null
    security find-generic-password \
        -a "${SHARED_KEYCHAIN_ACCOUNT}" \
        -s "${SHARED_KEYCHAIN_SERVICE}" \
        -w 2>/dev/null
}

is_android_sdk() {
    local sdk_path="$1"
    [[ -n "${sdk_path}" && -d "${sdk_path}/platforms" && -d "${sdk_path}/build-tools" ]]
}

configure_android_sdk() {
    local properties_sdk=""
    local sdk_candidate=""

    if [[ -f "${ROOT_DIR}/local.properties" ]]; then
        properties_sdk="$(sed -n 's/^sdk\.dir=//p' "${ROOT_DIR}/local.properties" | tail -n 1)"
    fi

    for sdk_candidate in \
        "${ANDROID_HOME:-}" \
        "${ANDROID_SDK_ROOT:-}" \
        "${properties_sdk}" \
        "${HOME}/Library/Android/sdk"; do
        if is_android_sdk "${sdk_candidate}"; then
            export ANDROID_HOME="${sdk_candidate}"
            export ANDROID_SDK_ROOT="${sdk_candidate}"
            return
        fi
    done

    echo "Android SDK not found." >&2
    echo "Set ANDROID_HOME or sdk.dir in ${ROOT_DIR}/local.properties." >&2
    exit 1
}

load_signing_configuration() {
    STORE_FILE="${REDACTGUARD_ANDROID_UPLOAD_STORE_FILE:-${DEFAULT_STORE_FILE}}"
    KEY_ALIAS="${REDACTGUARD_ANDROID_UPLOAD_KEY_ALIAS:-${DEFAULT_KEY_ALIAS}}"

    if [[ ! -f "${STORE_FILE}" ]]; then
        echo "Shared Harness upload keystore not found at ${STORE_FILE}." >&2
        echo "Set REDACTGUARD_ANDROID_UPLOAD_STORE_FILE if the Harness keystore lives elsewhere." >&2
        exit 1
    fi

    local store_password="${REDACTGUARD_ANDROID_UPLOAD_STORE_PASSWORD:-}"
    local key_password="${REDACTGUARD_ANDROID_UPLOAD_KEY_PASSWORD:-}"

    if [[ -z "${store_password}" ]]; then
        if ! store_password="$(read_shared_keychain_password)" || [[ -z "${store_password}" ]]; then
            echo "Shared Harness upload-key password is not available in macOS Keychain." >&2
            echo "Run the Harness helper once: bash scripts/build-phone-test-release.sh setup" >&2
            echo "from the android-local-llm-harness checkout, or inject the RedactGuard password variables securely." >&2
            exit 1
        fi
    fi
    if [[ -z "${key_password}" ]]; then
        key_password="${store_password}"
    fi

    export REDACTGUARD_ANDROID_UPLOAD_STORE_FILE="${STORE_FILE}"
    export REDACTGUARD_ANDROID_UPLOAD_STORE_PASSWORD="${store_password}"
    export REDACTGUARD_ANDROID_UPLOAD_KEY_ALIAS="${KEY_ALIAS}"
    export REDACTGUARD_ANDROID_UPLOAD_KEY_PASSWORD="${key_password}"
    trap clear_signing_configuration EXIT
}

clear_signing_configuration() {
    unset STORE_FILE
    unset KEY_ALIAS
    unset REDACTGUARD_ANDROID_UPLOAD_STORE_FILE
    unset REDACTGUARD_ANDROID_UPLOAD_STORE_PASSWORD
    unset REDACTGUARD_ANDROID_UPLOAD_KEY_ALIAS
    unset REDACTGUARD_ANDROID_UPLOAD_KEY_PASSWORD
}

check_configuration() {
    load_signing_configuration
    configure_android_sdk

    echo "RedactGuard Play upload signing configuration is available."
    echo "Keystore: ${STORE_FILE}"
    echo "Alias:    ${KEY_ALIAS}"
    echo "Secret source: existing Harness macOS Keychain item or explicit secure environment override"
}

build_release() {
    load_signing_configuration
    configure_android_sdk

    cd "${ROOT_DIR}"
    ./gradlew :app:bundleRelease

    if [[ ! -f "${AAB_PATH}" ]]; then
        echo "Expected signed Android App Bundle not found: ${AAB_PATH}" >&2
        exit 1
    fi

    if ! command -v jarsigner >/dev/null 2>&1; then
        echo "jarsigner is required to verify the signed AAB." >&2
        exit 1
    fi
    jarsigner -verify "${AAB_PATH}" >/dev/null

    echo
    echo "Signed and verified RedactGuard Android App Bundle:"
    echo "${AAB_PATH}"
    echo
    echo "Bundle SHA-256:"
    shasum -a 256 "${AAB_PATH}"

    if command -v keytool >/dev/null 2>&1; then
        echo
        echo "Upload certificate fingerprint embedded in the AAB:"
        keytool -printcert -jarfile "${AAB_PATH}" | sed -n '/SHA256:/p'
    fi
}

case "${1:-help}" in
    check)
        check_configuration
        ;;
    build)
        build_release
        ;;
    help|-h|--help)
        usage
        ;;
    *)
        usage >&2
        exit 2
        ;;
esac
