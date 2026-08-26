# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-08-26

## Integrated product state

RedactGuard is a standalone Android document-protection product. It consumes the published Harness Consumer Android SDK over Binder; model/runtime/GGUF/residency ownership remains in Harness.

The product includes:

- PDF and pasted-text ingestion converging on canonical `DocumentSegment`; image-only PDFs fail explicitly and OCR/VLM is out of scope;
- product-owned PII definitions with stable semantic categories, process-local selection and deterministic General, Healthcare, Financial and Legal profiles;
- Consumer SDK `0.1.0-alpha.5` `TaskDefinition` integration: selected PII IDs/descriptions/examples travel as bounded task metadata for Harness-owned system-prompt composition, while document payloads contain only selected type IDs and segments;
- bounded local analysis, strict result validation and atomic no-partial-findings behavior;
- privacy-safe diagnostics and actionable stable failure codes;
- hidden-by-default review, explicit reveal, deterministic redact/ignore decisions and fail-closed export eligibility;
- source-backed masked review context that fails closed on source mismatch or overlap;
- compact single-focus review and medium/expanded context-and-decision panes with large-text-safe independent scrolling;
- deterministic SAF PDF export with partial-output cleanup;
- process-local document text/findings/reveal/review state and no silent cloud fallback;
- task-first local-AI language rather than normal-surface Harness/Binder vocabulary;
- RedactGuard light/dark semantic tokens plus byte-identical canonical desktop brand assets stored with their content-correct JPEG extension for Android packaging;
- accessibility semantics, Compose product-experience tests, zero-residue smoke/two-APK helpers and reproducible build identity/evidence.

## repo-template-sw baseline

Canonical `dev` contains the `repo-template-sw` 0.5.0 L1 baseline with `android` and `product-ui` profiles. It owns `.engineering/baseline.json`, `.engineering/commands.json`, desired repository/documentation policy, validation/workstream/product-experience skills, repository-health CI and the machine-verifiable governance runbook.

`docs/workstreams/repo-template-sw-alignment.md` remains the alignment owner.

## Mobile product experience

PR #95 completed the mobile product-experience foundations: task hierarchy, desktop-brand token convergence, expanded PII profiles, masked context-first review, adaptive review layouts and the Consumer SDK task-definition boundary are implemented and covered by repository validation.

The visual product layer is **not complete**. A user-approved Android visual reference was established on 2026-08-26 for the five-step Document -> Protection -> Analysis -> Review -> Outcome journey and adaptive review. `docs/workstreams/android-visual-reference-convergence.md` is now the active owner for converging the Compose implementation on that reference while preserving the UX contract. Do not claim end-to-end mobile UX/UI completion until its visual and physical-device evidence gates are DONE.

The convergence implementation separates shared shell/primitives, Document/Analysis, Protection and Review/Outcome into explicit source owners so independent work can proceed without a single UI monolith. The approved reference bottom navigation/history, fabricated progress and unsupported placeholder metrics are explicitly excluded from the current wave.

Harness PR #441 is integrated and immutable `consumer-android:0.1.0-alpha.5` is published. RedactGuard maps selected PII descriptors to `TaskDefinition`; Harness composes them into its host-owned system prompt without allowing a consumer system-prompt override. RedactGuard PII profiles remain separate from Host-published inference presets.

Persisted History/bottom navigation, OCR/VLM, exact PDF coordinate preview and cloud fallback remain intentionally out of scope for this wave.

## Remaining real-environment evidence

Repository implementation is ahead of external evidence in three bounded areas:

1. Product UX: complete the active visual-reference-convergence workstream, then run instrumentation on an explicit Android target and record representative screenshot, TalkBack, large-text and adaptive physical-device checks with synthetic data.
2. Two-APK integration: execute same-signer Harness + RedactGuard pasted-text/text-PDF analysis with request-time PII definitions, review, cancellation/recovery, Host absence/death/reconnect, export, reopen and cleanup.
3. GitHub governance: apply and verify live branch/default-branch/required-check protection; desired-state validation is not proof of live enforcement.

Do not claim visual, physical-device or live-governance completeness until the corresponding gates are recorded.

Relevant active workstreams: `android-visual-reference-convergence.md`, `document-ingestion-v2.md`, `failure-diagnostics-hardening.md`, `ombra-to-redactguard-migration.md` and `harness-control-plane-consumer-cutover.md`.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard through these workstreams.
