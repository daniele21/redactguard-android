# Feature documentation

Files in this directory describe durable, non-obvious RedactGuard behavior as it exists now. They are not implementation plans or PR history.

Use a feature document when future maintainers need a stable product/domain contract that is too detailed for `docs/architecture.md` but should not be reconstructed from code every time.

Prefer executable truth in tests for deterministic invariants. Keep each fact in one canonical owner and update/remove stale feature docs when behavior changes.
