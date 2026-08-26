# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-08-26

## Integrated product state

RedactGuard is a standalone Android document-protection product. It consumes the published Harness Consumer Android SDK over Binder while model/runtime/GGUF/residency ownership remains in Harness.

The integrated product includes:

- PDF and pasted-text ingestion converging on canonical `DocumentSegment` analysis input; image-only PDFs fail explicitly and OCR/VLM remains out of scope;
- product-owned PII definitions with stable semantic categories, process-local selection and deterministic General, Healthcare, Financial and Legal protection profiles;
- Consumer SDK `0.1.0-alpha.5` request-time `TaskDefinition` integration: selected PII IDs/descriptions/examples travel as bounded structured task metadata for Harness-owned system-prompt composition, while the document payload contains only selected type IDs and document segments;
- bounded structured local analysis with strict result validation and atomic no-partial-findings semantics;
- stable product failure codes with actionable recovery and progressively disclosed privacy-safe diagnostics;
- hidden-by-default finding review, explicit reveal, deterministic redact/ignore decisions and fail-closed export eligibility;
- source-backed masked review context: known PII spans remain masked in contextual excerpts and source mismatches/overlaps fail closed before reaching UI;
- compact single-focus review plus medium/expanded context-and-decision panes using stable Android window breakpoints and independent scrolling for large text;
- deterministic SAF PDF export with partial-output cleanup on failure;
- process-local sensitive document text, findings, reveal state and review decisions; no silent cloud fallback;
- task-first local-AI language rather than normal-surface Harness/Binder implementation vocabulary;
- semantic RedactGuard light/dark theme/tokens and repository-owned canonical desktop brand assets whose Git blob identities match the approved desktop sources;
- accessibility semantics and native Compose product-experience instrumentation tests that compile/package in CI;
- executable zero-residue Android smoke and guided physical two-APK E2E helpers;
- build identity distinct from product version, source revision/dirty identity, immutable promoted artifacts, manifest/SHA-256/build delta and bounded retention.

## repo-template-sw baseline

Canonical `dev` contains the `repo-template-sw` 0.5.0 L1 baseline with `android` and `product-ui` profiles. PR #53 was merged as squash commit `371aec4242f23c45e428559dc62ad4c2862476a1` after exact-head `Validate` and `Repository health` passed on `fc468ee35d1c4bf72b7be61ae9c5c8129ec78be2`.

The integrated baseline includes:

- `.engineering/baseline.json`, `.engineering/commands.json` and documentation/repository desired-state policy;
- local structured-change, validation, workstream-finalization and product-experience skills;
- repository, operations, governance, documentation, agent-context and product-experience verifiers;
- `Repository health` CI and PR evidence template;
- desired-state branch governance with a machine-verifiable policy/runbook.

The validated convergence covered repository structure, operating contract, desired governance policy, product-experience contract, documentation lifecycle, instruction-context budget, Android formatting/helper syntax/failure contract, app/test compilation, JVM tests, native UI-test APK packaging, Android Lint, debug APK and minified-release APK assembly.

Active alignment workstream:

`docs/workstreams/repo-template-sw-alignment.md`

## Active product-experience convergence

`docs/workstreams/redactguard-mobile-experience.md` coordinates the final validation/evidence boundary for the desktop-brand-to-Android convergence, expanded product-owned PII taxonomy/profiles, privacy-safe context-first review, adaptive workspace and the cross-repo Consumer SDK request descriptor.

Harness PR #441 is integrated into Harness `dev` and immutable `consumer-android:0.1.0-alpha.5` is published. RedactGuard now maps each selected product-owned PII descriptor to `TaskDefinition`; Harness composes those bounded values into the effective host-owned system prompt without granting the consumer a free-form system-prompt override. RedactGuard PII profiles remain separate from Host-published inference presets.

The product-experience work intentionally does not add persisted History/bottom navigation, OCR/VLM, exact PDF coordinate preview or cloud fallback.

## Remaining real-environment evidence

Repository implementation is ahead of external evidence in three bounded areas.

1. Product experience: run native instrumentation on an explicit Android target and record representative TalkBack, large-text and compact/medium/expanded physical-device checks using synthetic data only.
2. Two-APK integration: execute the same-signer Harness + RedactGuard physical flow covering pasted text/text PDF, local analysis with request-time PII definitions, review, cancellation/recovery, Host absence/death/reconnect, export, independent reopen and cleanup.
3. GitHub governance: apply and then verify the documented live branch/default-branch/required-check protection policy. The repository owns and verifies desired state; it must not be reported as live enforcement until GitHub settings are actually changed.

Until those gates are recorded, do not claim physical-device completeness or live branch-governance enforcement solely from green CI.

Relevant active product workstreams remain:

- `docs/workstreams/document-ingestion-v2.md` — implementation green; physical ingestion evidence remains;
- `docs/workstreams/failure-diagnostics-hardening.md` — repository failure contract implemented; representative physical failure/recovery evidence remains;
- `docs/workstreams/ombra-to-redactguard-migration.md` — repository extraction complete; final physical cutover and Harness cleanup remain;
- `docs/workstreams/harness-control-plane-consumer-cutover.md` — multi-preset tolerance integrated; assigned-use-case/activation lifecycle remains dependent on corresponding Harness SDK/control-plane work;
- `docs/workstreams/redactguard-mobile-experience.md` — software implementation complete pending exact-head CI and bounded physical product/two-APK evidence.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard as part of alignment work. Those require separate owning capabilities/workstreams.
