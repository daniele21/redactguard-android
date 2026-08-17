# RedactGuard Android

Privacy-first Android document redaction application. RedactGuard imports a local PDF, detects selected PII using a separately installed Local AI Harness host, lets the user review each occurrence, and exports a new redacted PDF.

## Architecture boundary

RedactGuard owns the document product workflow. It does not own an LLM runtime, model store or GGUF artifacts. Local inference is consumed through the versioned Harness Consumer Android SDK over Binder.

See `docs/architecture.md`, `docs/current-state.md` and `docs/workstreams/ombra-to-redactguard-migration.md`.

## Development state

The repository is being extracted from the previously integrated OMBRA consumer implementation. The Android shell uses a committed Gradle 9.5.0 wrapper, JDK 17 and pinned Android/Compose dependencies. Domain, document, UI and quality migration continue on independent workstream branches while the Harness Consumer SDK is hardened for cross-repository consumption.
