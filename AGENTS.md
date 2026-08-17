# RedactGuard agent guide

Start with `docs/current-state.md`, then the closest scoped document under `docs/` and the active workstream `docs/workstreams/ombra-to-redactguard-migration.md` when working on the cross-repository extraction.

Durable invariants:

- RedactGuard owns document import/extraction, PII product policy, review, redaction and export.
- Harness owns model/runtime/Binder host behavior and publishes the Consumer Android SDK.
- Do not add Harness source checkouts, composite builds, git submodules or copied Binder/runtime implementations.
- Do not bundle GGUF/GGML artifacts.
- Sensitive document text/findings remain process-local unless an explicit product feature says otherwise.
- No silent cloud fallback.
- Keep changes scoped to the owning workstream and migrate tests with behavior.
