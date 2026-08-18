# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Read when: determining what is implemented, active or blocked in RedactGuard
Last reviewed: 2026-08-18

## Repository state

The OMBRA product implementation has been extracted into `daniele21/redactguard-android` and the repository-side migration is complete on `dev` through RedactGuard commit `343ab5f4ac26438aac0f1212a66022e1689f9274`.

Implemented in `dev`:

- Android/Compose repository shell derived from `repo-template-sw` with package `io.github.daniele21.redactguard` and debug package suffix `.debug`;
- pinned Gradle/JDK/Android build contract, Spotless, JVM tests, Android Lint and debug APK assembly gates;
- product-owned built-in/custom PII definitions and process-local definition selection;
- isolated PDF extraction with stable canonical document segments and bounded parsing;
- structured `document-pii-detection` prompt/schema, app-owned limits, deterministic Unicode-safe chunk planning and strict JSON parsing;
- deterministic fragment-to-canonical source mapping and exact finding validation without repair or heuristic source search;
- externally published Harness Consumer Android SDK consumption using `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.1` from the token-free public Maven branch;
- strict Consumer API/Binder adapter behind an app-owned runtime port, with explicit Host package/service identity, capability negotiation, JSON schema, stateless sessions, no reasoning, cancellation and session cleanup;
- sequential multi-chunk analysis with one atomic result boundary: no partial findings are exposed when any later chunk fails;
- hidden-by-default Review projection, explicit single-occurrence reveal, deterministic redaction decisions and placeholder planning;
- normalized PDF export to an explicit SAF destination with fail-closed partial-output cleanup;
- complete Android product flow: SAF import -> definitions -> analysis -> Review -> export -> success/error states;
- process-local sensitive state: document text, findings, review decisions and reveal state are not backed by `SavedStateHandle`, preferences, database or application files;
- physical two-APK preflight/runbook that verifies signer/package identity before device evidence.

Harness already authorizes the RedactGuard release/debug identities for the `document-pii-detection` use case. Runtime/model ownership, GGUF lifecycle, llama.cpp, scheduling and host telemetry remain in Harness.

## Validation state

PR #29 passed the complete exact-head repository gate before merge on head `98908d0048409f6ac1c1e43b5c7a1620d9faf7ba`:

```text
Spotless                 PASS
Compile app Kotlin       PASS
Compile JVM unit tests   PASS
Run JVM unit tests       PASS
Android Lint             PASS
Assemble debug APK       PASS
```

The final merge commit is `343ab5f4ac26438aac0f1212a66022e1689f9274`.

## Active hardening gap — failure diagnostics and recovery

Physical testing on 2026-08-18 exposed a product-quality gap in failure handling: document extraction already classifies specific causes such as encrypted, malformed, limit-exceeded, empty and image-only PDFs, but the current product projection collapses several of those known causes into one generic `IMPORT_UNSUPPORTED` error. Similar information-loss patterns must be audited across Harness connection, analysis, review and export before RedactGuard can be considered production-ready against the adopted `repo-template-sw` failure/observability/product-experience expectations.

This is now an active blocking workstream:

```text
docs/workstreams/failure-diagnostics-hardening.md
```

Required outcome:

- stable product-owned failure codes;
- cause identity preserved end to end;
- actionable user-facing recovery copy;
- progressive technical diagnostics;
- privacy-safe structured diagnostic events;
- explicit retry semantics;
- automated failure/cancellation/cleanup coverage;
- representative physical-device failure/recovery evidence.

OCR is not part of this workstream. Image-only PDFs must first fail with an accurate, diagnosable and actionable reason rather than a generic unsupported-file message.

## Remaining critical path

Repository-side migration is complete, but production hardening is not. The remaining gates are:

```text
RedactGuard repository-side migration complete
              |
              v
failure diagnostics / recovery hardening
              |
              v
physical same-signer Harness + RedactGuard E2E
              |
              v
independent exported-PDF verification
              |
              v
Harness legacy OMBRA cleanup / cutover
```

The physical gate must cover Host absent/recovery, import, multi-chunk inference, hidden/reveal Review, accept/ignore, cancellation, Host death/restart, export, independent PDF reopen, write-failure cleanup and process recreation. It must additionally record representative classified failure/recovery cases and verify that diagnostic evidence is identity-bearing and privacy-safe.

Do **not** remove `apps/local-llm-console` or the temporary legacy OMBRA Consumer identity from Harness until both the failure-diagnostics hardening gate and the physical two-APK gate are recorded green against exact APK/device/build identities.
