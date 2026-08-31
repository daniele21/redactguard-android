# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-08-31

## Integrated product state

RedactGuard is a standalone Android document-protection product consuming the Harness Consumer Android SDK over Binder; Harness retains model/runtime/GGUF/residency ownership.

Implemented capabilities include PDF/pasted-text ingestion, built-in/custom PII selection, Consumer SDK `0.1.0-alpha.6` task metadata, runtime readiness, bounded sequential analysis, atomic validation, privacy-safe diagnostics, masked review, fail-closed redaction/export, adaptive Review, SAF PDF export and process-local sensitive state. OCR/VLM and cloud fallback remain out of scope.

## Engineering baseline

`dev` carries `repo-template-sw` 0.8.0 with repository-owned Android/product-UI customizations preserved. `.engineering/*`, local skills and CI own the operating contract.

## Mobile product experience

`dev` contains the target-derived visual system and graphics, process-local summary projection, corrected Document/Analysis/Protection/Review/Outcome/Recovery surfaces, hidden-by-default Review values, selected-profile evidence, landscape expanded Review, target-comparison Visual Evidence v2 and 14-checkpoint E2E evidence.

The approved visual identity remains anchored by SHA-256 `21b55331634fb0aafeeafdef971d8b43489f5eedbda30bc21e3fdade92371b5a`; `design/reference/approved-target.png` and `target-provenance.json` retain equivalence evidence. Launcher identity uses legacy + API26 adaptive resources. Persisted History/bottom nav, fabricated progress/metrics, OCR/VLM, exact PDF-coordinate preview, cloud fallback, fake Share and unowned Options/Settings remain excluded.

## Automated evidence

VUI-18 is integrated on `dev` as merge `583f7a58dcbd55be5611d1f7125e7e90d4f38c76`. Its validated head `4536c302d7d528b01045499e3cf2a10d422f643d` passed FULL remote preflight `33271498163`, Emulator E2E v2 `33271488297` and Visual Evidence v2 `33271488301`.

The exhaustive `CHUNK_FAILED` regression suite is validated on exact head `6aec3906f206421dde01bd5694f44eb2b0841efb`: Validate `33399004284`, Emulator E2E `33399004372`, Repository health `33399004482`, Visual evidence `33399004674` and Two-APK emulator E2E `33399005355` passed. That exact head is the base of the current diagnostics candidate; draft #141 was closed as superseded by non-draft #142 after the connected GitHub ready-for-review mutation failed.

## Active Local AI blocker

Earlier physical installed-pair evidence proved same signer, granted Local AI permission and a live RedactGuard-to-Harness Binder service while `RG-AI-002 / HOST_UNAVAILABLE` reproduced. The latest reported physical reproduction advances into analysis and surfaces `RG-AI-008 / CHUNK_FAILED`.

Before the current candidate, connected `RUNTIME_FAILURE`, `PREPARE_FAILED` and `SESSION_NOT_FOUND` all collapsed into `GENERATION_FAILED` and lost their original `ConsumerErrorCode`. PR #142 keeps stable product mapping but preserves only the safe boundary step plus `Consumer:<ENUM>` in `AnalysisRuntimeDiagnostic`; free-form `ConsumerFailure.message` is discarded.

The current candidate requires STRONG automated validation. After that, REAL_ENVIRONMENT evidence must reproduce `RG-AI-008` on the exact same-signer build without clearing Harness model/configuration state and capture whether the safe identity points to preparation, session lifecycle or generation/runtime. That evidence chooses the functional owner/fix.

## Remaining evidence

1. Run VUI-7 physical accessibility/adaptive evidence on a named Android device.
2. Run the same-signer Harness + RedactGuard ARM64 journey, including the current `RG-AI-008` reproduction, and capture stable code/stage/operation plus safe `Consumer:<ENUM>` and boundary step.
3. Verify live GitHub branch/default-branch/required-check protection; desired policy alone is not enforcement evidence.

Active workstreams: `android-visual-reference-convergence.md`, `document-ingestion-v2.md`, `failure-diagnostics-hardening.md`, `ombra-to-redactguard-migration.md`, `harness-control-plane-consumer-cutover.md`.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard through these workstreams.
