# PII product domain

Status: migration in progress
Owner: RedactGuard

The built-in PII definition contract is product-owned and remains version 1 during extraction from Harness. Its stable type IDs are `full-name`, `email`, `telephone`, `postal-address`, `italian-tax-code` and `iban`.

Custom definitions are process-local, bounded and assigned content-free ordinal IDs. User-provided labels/definitions/examples are redacted from default `toString()` output.

Model output is treated as untrusted input. RedactGuard owns a small bounded strict JSON parser that rejects duplicate keys, unsupported numbers, malformed Unicode, excess depth/container/string sizes and trailing prose. It does not repair malformed model JSON silently.

## Migration provenance

This RG-2 slice is replayed directly on the integrated RedactGuard bootstrap baseline and contains only product-owned PII definition and strict-JSON behavior. Harness runtime, model and transport types remain outside this pure domain boundary.
