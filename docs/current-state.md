# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-08-28

## Integrated product state

RedactGuard is a standalone Android document-protection product. It consumes the published Harness Consumer Android SDK over Binder; model/runtime/GGUF/residency ownership remains in Harness.

The product includes:

- PDF and pasted-text ingestion converging on canonical `DocumentSegment`; image-only PDFs fail explicitly and OCR/VLM is out of scope;
- product-owned PII definitions with stable semantic categories, process-local selection and deterministic General, Healthcare, Financial and Legal profiles;
- Consumer SDK `0.1.0-alpha.6` integration: selected PII IDs/descriptions/examples travel as bounded `TaskDefinition` metadata for Harness-owned system-prompt composition, while document payloads contain only selected type IDs and segments; source-backed Consumer runtime readiness remains model-identity-safe;
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

PR #96 is the active visual-reference convergence wave. It preserves the settled task model and alpha.6 runtime boundary while converging the five-step Document -> Protection -> Analysis -> Review -> Outcome journey, recovery states and adaptive review on the approved Android reference. `docs/workstreams/android-visual-reference-convergence.md` owns this work. Do not claim end-to-end mobile UX/UI completion until exact-head emulator visual/journey evidence and the named physical-device accessibility gate are complete.

The convergence implementation separates shared shell/primitives, Document/Analysis, Protection and Review/Outcome into explicit source owners. Persisted History/bottom navigation, fabricated progress, unsupported placeholder metrics, OCR/VLM, exact PDF-coordinate preview and cloud fallback remain excluded.

RedactGuard resolves `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.6`. The alpha.6 boundary adds consumer-safe runtime readiness without exposing model IDs, digests, paths or runtime tuning. RedactGuard maps selected PII descriptors to `TaskDefinition`; Harness composes them into its host-owned system prompt without allowing a consumer system-prompt override. RedactGuard PII profiles remain separate from Host-published inference presets.

## CRV automated candidate convergence

The pre-visual-wave Consumer Runtime Visibility candidate remains a frozen evidence point, not the final UX candidate:

- Harness physical candidate: `a30f67b21e24adc6efea838e9a9d65cc78446f28`, `versionCode=31`, `versionName=1.0.0`; Package Android Artifacts run `33159622580` passed package build and Android packaging verification on that exact source revision;
- RedactGuard pre-visual physical candidate: `4679c23a9a22e5242761fe52af97f4eb7432aec7`, `versionCode=11`, `versionName=0.1.4`; Package RedactGuard Artifacts run `33161250690` passed debug packaging, minified release-ci packaging, exact source-identity verification and bounded artifact upload;
- RedactGuard release-ci artifact SHA-256: `7494948bb3f707e1682923aace289d44b9d726f6314d88e3865a2c638e8f738f`;
- these CI artifacts prove deterministic package/source lineage only. PR #96 must produce a newer exact-head candidate before the physical UX/two-APK gate.

## Remaining real-environment evidence

Repository implementation is ahead of external evidence in three bounded areas:

1. Product UX: complete the active visual-reference-convergence automated emulator gates, then record representative TalkBack, large-text and adaptive physical-device checks with synthetic data.
2. Two-APK integration: after deterministic emulator coverage, execute the newer same-signer Harness + RedactGuard candidate on a real ARM64 device with pasted-text/text-PDF analysis, request-time PII definitions, source-backed runtime readiness, review, cancellation/recovery, Host absence/death/reconnect, export, reopen and cleanup.
3. GitHub governance: apply and verify live branch/default-branch/required-check protection; desired-state validation is not proof of live enforcement.

Do not claim visual, physical-device or live-governance completeness until the corresponding gates are recorded.

Relevant active workstreams: `android-visual-reference-convergence.md`, `document-ingestion-v2.md`, `failure-diagnostics-hardening.md`, `ombra-to-redactguard-migration.md` and `harness-control-plane-consumer-cutover.md`.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard through these workstreams.
