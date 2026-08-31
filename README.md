# RedactGuard Android

Privacy-first Android document redaction application. RedactGuard imports a local PDF, detects selected PII using a separately installed Local AI Harness host, lets the user review each occurrence, and exports a new redacted PDF.

## Architecture boundary

RedactGuard owns the document product workflow. It does not own an LLM runtime, model store or GGUF artifacts. Local inference is consumed through the versioned Harness Consumer Android SDK over Binder.

See `docs/architecture.md`, `docs/current-state.md` and `docs/workstreams/ombra-to-redactguard-migration.md`.

## Release and Play internal testing

Release bundles are fail-closed unless the RedactGuard upload signing environment is complete. The local release helper intentionally reuses the existing Harness Play upload key without storing signing material in this repository:

```bash
bash scripts/build-redactguard-release.sh check
bash scripts/build-redactguard-release.sh build
```

Validated `dev` revisions can also be published automatically to Google Play Internal Testing through `.github/workflows/play-internal.yml`. The workflow is opt-in through repository variable `PLAY_INTERNAL_ENABLED=true`, reconstructs the upload key only inside the protected runner, resolves the next `versionCode` from Play itself, builds through the same canonical AAB entrypoint and authenticates to Google through OIDC/Workload Identity Federation rather than a stored service-account JSON key.

For the complete signing model, GitHub/Google configuration, Play Console registration and same-app-signing-key requirement, see `docs/release/play-internal-testing.md`. Physical cross-repository evidence remains governed by `docs/evidence/physical-two-apk.md`.

## Development state

The repository is being extracted from the previously integrated OMBRA consumer implementation. The Android shell uses a committed Gradle 9.5.0 wrapper, JDK 17 and pinned Android/Compose dependencies. Domain, document, UI and quality migration continue on independent workstream branches while the Harness Consumer SDK is hardened for cross-repository consumption.
