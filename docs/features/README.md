# Feature documentation

Files in this directory describe durable, non-obvious RedactGuard behavior as it exists now. They are not implementation plans or PR history.

Use a feature document when future maintainers need a stable product/domain contract that is too detailed for `docs/architecture.md` but should not be reconstructed from code every time.

Prefer executable truth in tests for deterministic invariants. Keep each fact in one canonical owner. When a change alters behavior already described here, update that feature owner in the same change. Create a new feature document only when durable non-obvious behavior is not sufficiently discoverable from public contracts, tests, code or architecture; do not create one file per small feature.
