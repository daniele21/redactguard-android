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

The approved visual identity remains anchored by SHA-256 `21b55331634fb0aafeeafdef971d8b43489f5eedbda30bc21e3fdade92371b5a`; `design/reference/approved-target.png` and `target-provenance.json` retain equivalence evidence. Launcher identity uses legacy + API26 adaptive resources. Persisted History, fabricated progress/metrics, OCR/VLM, exact PDF-coordinate preview, cloud fallback and fake Share remain excluded. Top-level `Analizza / AI locale / Impostazioni` navigation and an owned read-only Local AI setup/readiness surface are planned by the active LAS workstream and are not yet integrated on `dev`.

## Automated evidence

VUI-18 is integrated on `dev` as merge `583f7a58dcbd55be5611d1f7125e7e90d4f38c76`. Its validated head `4536c302d7d528b01045499e3cf2a10d422f643d` passed FULL remote preflight `33271498163`, Emulator E2E v2 `33271488297` and Visual Evidence v2 `33271488301`.

The exhaustive `CHUNK_FAILED` regression suite is validated on exact head `6aec3906f206421dde01bd5694f44eb2b0841efb`: Validate `33399004284`, Emulator E2E `33399004372`, Repository health `33399004482`, Visual evidence `33399004674` and Two-APK emulator E2E `33399005355` passed.

Failure-diagnostics hardening PR #142 is merged on `dev` as `6627d082ed1f1b8b63b1759662a1b05b0138e5e2`. Its exact feature head `0157d4f206da763e556b57474459f4ded23fc818` passed Validate `33405404519`, Repository health `33405404520`, Emulator E2E `33405404486`, Visual evidence `33405404542` and Two-APK emulator E2E `33405406165`. Post-merge exact-commit Validate `33407219310`, Repository health `33407219235`, Package RedactGuard Artifacts `33407219340` and Publish Play Internal `33407219202` also passed; the signed release AAB was verified and published to Google Play Internal Testing.

## Active Local AI blocker

Earlier physical installed-pair evidence proved same signer, granted Local AI permission and a live RedactGuard-to-Harness Binder service while `RG-AI-002 / HOST_UNAVAILABLE` reproduced. The latest reported physical reproduction advances into analysis and surfaces `RG-AI-008 / CHUNK_FAILED`.

PR #142 does not claim that physical runtime failure is fixed. It keeps stable product mapping while preserving the safe boundary step plus `Consumer:<ENUM>` in `AnalysisRuntimeDiagnostic`; free-form `ConsumerFailure.message` is discarded. The remaining REAL_ENVIRONMENT reproduction must capture that typed Consumer identity on the representative ARM64/JNI/GGUF path so the functional owner can be identified without guessing.

Separately, the active `local-ai-setup-readiness.md` workstream now carries the Harnex consumer-safe resolved setup projection and RedactGuard fresh fail-closed preflight on the LAS candidate branch. Harnex durable logical jobs and emulator fault control are merged in Harnex `dev` through `6621dc1977b8a23cf73037c830094850cbef1c15`; RedactGuard logical-job cutover and lifecycle evidence are integrated in the LAS branch through `d144687a3d0b0179bb92b92994363e459de7163d`. Final exact-head two-APK execution remains pending on the parent RedactGuard PR to `dev`; configuration and runtime ownership remain in Harnex.

## Remaining evidence

1. Run VUI-7 physical accessibility/adaptive evidence on a named Android device.
2. Run the same-signer Harness + RedactGuard ARM64 journey, including the current `RG-AI-008` reproduction, and capture stable code/stage/operation plus safe `Consumer:<ENUM>` and boundary step.
3. Verify live GitHub branch/default-branch/required-check protection; desired policy alone is not enforcement evidence.
4. Complete the remaining LAS product-navigation/readiness UX slices, run the parent-PR exact-head two-APK lifecycle journey, and then capture the separate representative ARM64/JNI/GGUF/OEM evidence required for physical-runtime claims.

Active workstreams: `android-visual-reference-convergence.md`, `document-ingestion-v2.md`, `failure-diagnostics-hardening.md`, `ombra-to-redactguard-migration.md`, `harness-control-plane-consumer-cutover.md`, `local-ai-setup-readiness.md`.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard through these workstreams. LAS may expose only the versioned consumer-safe read-only resolved execution setup published by Harness; RedactGuard must never reconstruct or mutate Harness-owned model/configuration state.
