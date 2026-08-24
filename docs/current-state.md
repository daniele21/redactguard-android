# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-08-24

## Integrated state

`dev` contains the standalone RedactGuard Android product and no longer depends on Harness source ownership. RedactGuard consumes the published Harness Consumer Android SDK over Binder and keeps runtime/model/GGUF/residency ownership in Harness.

Implemented repository behavior includes:

- Android/Compose app shell with pinned Gradle/JDK/Android build contract, Spotless, JVM tests, Android Lint and debug/minified-release assembly gates;
- PDF and pasted-text ingestion converging on canonical `DocumentSegment` analysis input; image-only PDFs fail explicitly and OCR/VLM remains out of scope;
- product-owned PII definitions, custom definition entry and process-local selection;
- structured local analysis through the Consumer SDK with bounded chunking, strict structured-result validation and atomic no-partial-findings semantics;
- stable product failure codes across import, local-AI analysis, review and export, with actionable user recovery and progressively disclosed privacy-safe diagnostics;
- hidden-by-default finding review, explicit reveal, deterministic redact/ignore decisions and fail-closed export eligibility;
- deterministic PDF export to an explicit SAF destination with partial-output cleanup on failure;
- process-local sensitive document text, findings, reveal state and review decisions; no silent cloud fallback;
- consumer capability handling that no longer rejects a valid Harness capability merely because multiple host-published presets exist;
- physical two-APK preflight/runbook with signer/package identity checks.

## Current integration head

The repo-template-sw alignment workstream was added to `dev` at commit `1b6efb0e23f997b7c23cdacc8546465779f7d4eb`.

Active alignment plan:

`docs/workstreams/repo-template-sw-alignment.md`

Its first wave parallelizes engineering baseline/verifiers, documentation governance, build/artifact lifecycle and the product-experience contract. UI implementation follows only after the UX contract is settled.

## Remaining product evidence

Repository-side implementation is ahead of real-device evidence. The strongest remaining gate is the same-signer Harness + RedactGuard physical workflow covering representative pasted text/text PDF input, local analysis, review, cancellation/recovery, Host absence/death/reconnect, export, independent PDF reopen and failure cleanup.

Until that evidence is recorded, do not claim physical-device completeness from JVM/CI/emulator results and do not remove legacy Harness cutover compatibility solely because repository tests are green.

Relevant active workstreams:

- `docs/workstreams/document-ingestion-v2.md` — implementation green; physical ingestion smoke evidence remains;
- `docs/workstreams/failure-diagnostics-hardening.md` — repository failure contract implemented; representative physical failure/recovery evidence remains;
- `docs/workstreams/ombra-to-redactguard-migration.md` — repository extraction complete; final physical cutover and Harness cleanup remain;
- `docs/workstreams/harness-control-plane-consumer-cutover.md` — multi-preset tolerance integrated; assigned-use-case/activation lifecycle remains dependent on corresponding Harness SDK/control-plane work.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard as part of alignment work. Those require separate owning capabilities/workstreams.
