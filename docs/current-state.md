# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-08-28

## Integrated product state

RedactGuard is a standalone Android document-protection product consuming the published Harness Consumer Android SDK over Binder; Harness retains model/runtime/GGUF/residency ownership.

Implemented product capabilities include PDF and pasted-text ingestion to canonical `DocumentSegment`, deterministic built-in/custom PII selection, Consumer SDK `0.1.0-alpha.6` `TaskDefinition` metadata, source-backed consumer-safe runtime readiness, bounded sequential analysis, strict atomic result validation, privacy-safe diagnostics, masked review with fail-closed redaction/export, adaptive compact/expanded review, deterministic SAF PDF export and process-local sensitive state. Image-only PDF OCR/VLM and cloud fallback are out of scope.

## repo-template-sw baseline

Canonical `dev` carries the `repo-template-sw` 0.5.0 L1 baseline with `android` and `product-ui` profiles. `.engineering/*`, repository/documentation policy, local skills and CI own the operating contract. `docs/workstreams/repo-template-sw-alignment.md` owns baseline alignment.

## Mobile product experience

PR #95 completed the mobile-experience foundations. PR #96 is the active visual-reference convergence wave: it preserves the settled task model and alpha.6 runtime boundary while converging Document -> Protection -> Analysis -> Review -> Outcome, recovery states and adaptive review on the approved Android reference. `docs/workstreams/android-visual-reference-convergence.md` is authoritative.

Do not claim end-to-end UX/UI completion until exact-head emulator visual/journey evidence and named physical-device accessibility evidence are complete. Persisted History/bottom navigation, fabricated progress/metrics, OCR/VLM, exact PDF-coordinate preview and cloud fallback remain excluded.

RedactGuard resolves `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.6`. Selected PII descriptors become bounded `TaskDefinition` metadata; Harness composes the system prompt and never exposes model IDs, digests, paths or runtime tuning to RedactGuard. PII profiles remain distinct from Host inference presets.

## Candidate evidence

The pre-visual CRV candidate is frozen evidence, not the final UX candidate:

- Harness: `a30f67b21e24adc6efea838e9a9d65cc78446f28`, v31/1.0.0, package run `33159622580` passed.
- RedactGuard: `4679c23a9a22e5242761fe52af97f4eb7432aec7`, v11/0.1.4, package run `33161250690` passed; release-ci SHA-256 `7494948bb3f707e1682923aace289d44b9d726f6314d88e3865a2c638e8f738f`.

These prove package/source lineage only. PR #96 must produce a newer exact-head candidate before physical UX/two-APK validation.

## Remaining evidence

1. Complete exact-head emulator visual and deterministic product-journey gates; then record TalkBack, large-text and adaptive checks on the named physical device.
2. Execute the newer same-signer Harness + RedactGuard candidate on real ARM64 hardware with pasted text, text PDF, request-time PII definitions, runtime readiness, review, cancellation/recovery, Host absence/death/reconnect, export/reopen and cleanup.
3. Apply and verify live GitHub branch/default-branch/required-check protection; desired-state validation alone is not enforcement evidence.

Active workstreams: `android-visual-reference-convergence.md`, `document-ingestion-v2.md`, `failure-diagnostics-hardening.md`, `ombra-to-redactguard-migration.md`, `harness-control-plane-consumer-cutover.md`.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard through these workstreams.
