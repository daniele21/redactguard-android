# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-08-31

## Integrated product state

RedactGuard is a standalone Android document-protection product consuming the Harness Consumer Android SDK over Binder; Harness retains model/runtime/GGUF/residency ownership.

Implemented capabilities include PDF/pasted-text ingestion, deterministic built-in/custom PII selection, Consumer SDK `0.1.0-alpha.6` task metadata, runtime readiness, bounded sequential analysis, atomic result validation, privacy-safe diagnostics, masked review, fail-closed redaction/export, adaptive Review, SAF PDF export and process-local sensitive state. OCR/VLM and cloud fallback remain out of scope.

## Engineering baseline

`dev` carries `repo-template-sw` 0.8.0 with the repository-owned Android/product-UI customizations preserved. `.engineering/*`, local skills and CI own the operating contract.

## Mobile product experience

The first visual wave established branded surfaces, adaptive launcher identity, emulator product journeys, seven Visual Evidence surfaces and 14 asserted E2E UI checkpoints, but direct comparison with the approved target showed that screenshot-exists evidence was too weak to close visual fidelity.

`dev` now contains the integrated target-derived visual system and graphics, truthful process-local summary projection, corrected Document/Analysis/Protection/Review/Outcome/Recovery surfaces, a compact hidden-by-default Review value treatment, genuine selected-profile evidence, coherent completed Outcome fixtures, true landscape expanded Review, target-comparison Visual Evidence v2 and reconverged 14-checkpoint E2E evidence.

The original approved attachment identity remains SHA-256 `21b55331634fb0aafeeafdef971d8b43489f5eedbda30bc21e3fdade92371b5a`. The repository uses `design/reference/approved-target.png` as a canonical rendered raster with independent SHA/equivalence evidence in `target-provenance.json`; this preserves approval provenance while avoiding byte-encoding differences being mistaken for visual-design drift.

The light task-header shield is now materialized repository-side directly from the canonical rendered target as the declared 54x67 crop `(8,18)-(62,85)`. The previous malformed transferred PNG was replaced after it caused both AAPT2 release compilation and Compose resource-loading failures; the one-shot materialization workflow self-deleted after committing the verified derived asset.

Launcher identity continues to use `@mipmap/ic_launcher` / `ic_launcher_round` with legacy + API26 adaptive resources.

Still excluded even when shown in the illustrative target: persisted History/bottom nav, fabricated progress/metrics, OCR/VLM, exact PDF-coordinate preview, cloud fallback, fake Share and unowned Options/Settings.

## Automated evidence

VUI-18 is integrated on `dev` as merge `583f7a58dcbd55be5611d1f7125e7e90d4f38c76`. The final validated product head was `4536c302d7d528b01045499e3cf2a10d422f643d`, based directly on `dev@8a937d8542f255562bd7e8effb5f3f7b5a8a0618` with zero behind commits.

On that exact head, FULL remote preflight `33271498163` passed deterministic Android gates, AndroidTest APK, minified release/R8 and repository validation; Emulator E2E v2 `33271488297` and Visual Evidence v2 `33271488301` also passed. The generated target-vs-actual artifact was explicitly reviewed after the final Protection spacing correction before merge.

The approved-target upload/re-encoding equivalence probe is recorded by GitHub Actions run `33261668673`: 384-bit average-hash Hamming distance `2/384` and maximum approved-region channel-mean delta `0.945/255` versus the original approved attachment. This evidence establishes that the repository PNG is a rendered encoding of the same board, not a replacement visual target.

The exhaustive `CHUNK_FAILED` regression suite is validated on exact test head `6aec3906f206421dde01bd5694f44eb2b0841efb`. `Validate` run `33399004284`, Emulator E2E `33399004372`, Repository health `33399004482`, Visual evidence `33399004674` and Two-APK emulator E2E `33399005355` all passed. That test head remains the base of the current diagnostics candidate because PR #141 could not be transitioned out of draft through the connected GitHub mutation path; no test evidence was discarded or recreated on a different source base.

## Active Local AI blocker

Earlier physical release installed-pair evidence proved same signer, granted Local AI permission and a live RedactGuard-to-Harness Binder service while `RG-AI-002 / HOST_UNAVAILABLE` reproduced. The latest reported physical reproduction advances further and surfaces `RG-AI-008 / CHUNK_FAILED` during analysis.

Before the current candidate, `ConsumerAnalysisRuntime` collapsed connected `RUNTIME_FAILURE`, `PREPARE_FAILED` and `SESSION_NOT_FOUND` into the common `GENERATION_FAILED` family and discarded the original typed `ConsumerErrorCode`. The candidate keeps the existing stable product mapping but preserves only the safe boundary step plus `Consumer:<ENUM>` in `AnalysisRuntimeDiagnostic`; free-form `ConsumerFailure.message` remains outside the app-owned diagnostic path.

The next automated gate is STRONG validation of the exact diagnostics candidate. After that, the remaining root-cause step is REAL_ENVIRONMENT evidence: install the exact same-signer candidate without clearing Harness model/configuration state, reproduce `RG-AI-008`, expand technical details and record whether the underlying identity is preparation, session lifecycle or generation/runtime. Only that evidence should choose the functional owner/fix.

## Active visual-fidelity plan

Current executable slice: `VUI-7`.

Completed visual-convergence slices: VUI-1..6, VUI-8, VUI-9, VUI-10, VUI-15, VUI-11, VUI-12, VUI-13, VUI-14, VUI-16, VUI-17 and VUI-18. The cumulative automated wave merged to `dev` via #134; slice PRs #129/#130 were closed as superseded by the integrated tree.

Execution remaining:
1. run VUI-7 physical accessibility/adaptive evidence on a named Android device;
2. run the real same-signer Harness + RedactGuard ARM64 journey including Host absence/death/reconnect, review, export/reopen and cleanup;
3. record bounded physical evidence without reclassifying emulator results as REAL_ENVIRONMENT proof.

## Remaining evidence

1. Run VUI-7 on a named physical device: TalkBack, large text, compact landscape/adaptive behavior and OEM launcher rendering.
2. Run same-signer Harness + RedactGuard on real ARM64 with pasted text, text PDF, request-time PII definitions, readiness, review, cancellation/recovery, Host absence/death/reconnect, export/reopen and cleanup. For the current `RG-AI-008` reproduction, capture stable code/stage/operation plus the safe `Consumer:<ENUM>` identity and boundary step.
3. Verify live GitHub branch/default-branch/required-check protection; desired policy alone is not enforcement evidence.

Active workstreams: `android-visual-reference-convergence.md`, `document-ingestion-v2.md`, `failure-diagnostics-hardening.md`, `ombra-to-redactguard-migration.md`, `harness-control-plane-consumer-cutover.md`.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard through these workstreams.
