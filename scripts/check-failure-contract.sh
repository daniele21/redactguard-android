#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_DIR="$ROOT_DIR/app/src/main/kotlin"
CANONICAL_FAILURE_FILE="$SOURCE_DIR/io/github/daniele21/redactguard/domain/failure/ProductFailure.kt"

fail() {
  echo "failure-contract: $*" >&2
  exit 1
}

[[ -f "$CANONICAL_FAILURE_FILE" ]] || fail "canonical ProductFailure contract is missing"

mapfile -t product_failure_owners < <(
  grep -R -l --include='*.kt' 'enum class ProductFailureKind' "$SOURCE_DIR" || true
)
if [[ ${#product_failure_owners[@]} -ne 1 || "${product_failure_owners[0]}" != "$CANONICAL_FAILURE_FILE" ]]; then
  printf 'failure-contract: ProductFailureKind owners:\n' >&2
  printf '  %s\n' "${product_failure_owners[@]:-<none>}" >&2
  fail "ProductFailureKind must have exactly one owner: domain/failure/ProductFailure.kt"
fi

legacy_pattern='IMPORT_UNSUPPORTED|ANALYSIS_FAILED|REVIEW_INVALID|EXPORT_FAILED'
if grep -R -n -E --include='*.kt' "$legacy_pattern" "$SOURCE_DIR"; then
  fail "legacy many-to-one failure buckets are forbidden in production Kotlin"
fi

if ! grep -q 'IMAGE_ONLY_PDF("RG-PDF-008"' "$CANONICAL_FAILURE_FILE"; then
  fail "IMAGE_ONLY_PDF must retain stable code RG-PDF-008"
fi

if ! grep -q '"RG-AI-012"' "$CANONICAL_FAILURE_FILE"; then
  fail "LOCAL_AI_INTERNAL must retain stable code RG-AI-012"
fi

if ! grep -q 'UNKNOWN_INTERNAL("RG-SYS-001"' "$CANONICAL_FAILURE_FILE"; then
  fail "the explicit unknown/internal fallback RG-SYS-001 is missing"
fi

echo "failure-contract: PASS"
