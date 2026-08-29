# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-08-29

## Integrated product state

RedactGuard is a standalone Android document-protection product consuming the Harness Consumer Android SDK over Binder; Harness retains model/runtime/GGUF/residency ownership.

Implemented capabilities include PDF/pasted-text ingestion, deterministic built-in/custom PII selection, Consumer SDK `0.1.0-alpha.6` task metadata, runtime readiness, bounded sequential analysis, atomic result validation, privacy-safe diagnostics, masked review, fail-closed redaction/export, adaptive Review, SAF PDF export and process-local sensitive state. OCR/VLM and cloud fallback remain out of scope.

## Engineering baseline

`dev` carries `repo-template-sw` 0.8.0 with the repository-owned Android/product-UI customizations preserved. `.engineering/*`, local skills and CI own the operating contract.

## Mobile product experience

The first visual wave established branded surfaces, adaptive launcher identity, emulator product journeys, seven Visual Evidence surfaces and 14 asserted E2E UI checkpoints, but direct comparison with the approved target showed that screenshot-exists evidence was too weak to close visual fidelity.

The active fidelity-correction candidate now contains the target-derived visual system and graphics, truthful process-local summary projection, corrected Document/Analysis/Protection/Review/Outcome/Recovery surfaces, a compact hidden-by-default Review value treatment, genuine selected-profile evidence, coherent completed Outcome fixtures, true landscape expanded Review, target-comparison Visual Evidence v2 and reconverged 14-checkpoint E2E evidence.

The original approved attachment identity remains SHA-256 `21b55331634fb0aafeeafdef971d8b43489f5eedbda30bc21e3fdade92371b5a`. The repository uses `design/reference/approved-target.png` as a canonical rendered raster with independent SHA/equivalence evidence in `target-provenance.json`; this preserves approval provenance while avoiding byte-encoding differences being mistaken for visual-design drift.

The light task-header shield is now materialized repository-side directly from the canonical rendered target as the declared 54x67 crop `(8,18)-(62,85)`. The previous malformed transferred PNG was replaced after it caused both AAPT2 release compilation and Compose resource-loading failures; the one-shot materialization workflow self-deleted after committing the verified derived asset.

Launcher identity continues to use `@mipmap/ic_launcher` / `ic_launcher_round` with legacy + API26 adaptive resources.

Still excluded even when shown in the illustrative target: persisted History/bottom nav, fabricated progress/metrics, OCR/VLM, exact PDF-coordinate preview, cloud fallback, fake Share and unowned Options/Settings.

## Automated evidence

Current `dev` base for the VUI-18 candidate is `8a937d8542f255562bd7e8effb5f3f7b5a8a0618` (repo-template-sw 0.8.0 migration). The candidate is based directly on that tip with no behind commits.

Earlier exact-head validation proved FULL Android gates and the 14-checkpoint Emulator E2E on a prior candidate, but material fidelity/provenance and shield-resource changes invalidate that evidence for merge readiness. VUI-18 must therefore rerun FULL preflight, Visual Evidence v2 and Emulator E2E v2 on the final exact head before integration.

The approved-target upload/re-encoding equivalence probe is recorded by GitHub Actions run `33261668673`: 384-bit average-hash Hamming distance `2/384` and maximum approved-region channel-mean delta `0.945/255` versus the original approved attachment. This evidence establishes that the repository PNG is a rendered encoding of the same board, not a replacement visual target.

## Active visual-fidelity plan

Current executable slice: `VUI-18`.

Completed implementation slices: VUI-8, VUI-9, VUI-10, VUI-15, VUI-11, VUI-12, VUI-13 and VUI-14. VUI-16/17 implementations are present in PR #132 and are READY for their final exact-head workflow evidence. VUI-18 owns freshness, complete diff review, final automated evidence and integration to `dev`.

Execution remaining:
1. run FULL remote preflight + Visual Evidence v2 + Emulator E2E v2 on the final user-authored exact head of PR #132;
2. explicitly review the target-vs-actual visual artifact;
3. merge VUI-18 to `dev` if all automated evidence agrees;
4. close the incorporated VUI-16/VUI-17 slice PRs;
5. run VUI-7 physical evidence from corrected `dev`.

## Remaining evidence

1. Close VUI-18 automated integration on a fresh exact head and merge corrected UI into `dev`.
2. Run VUI-7 on a named physical device: TalkBack, large text, compact landscape/adaptive behavior and OEM launcher rendering.
3. Run same-signer Harness + RedactGuard on real ARM64 with pasted text, text PDF, request-time PII definitions, readiness, review, cancellation/recovery, Host absence/death/reconnect, export/reopen and cleanup.
4. Verify live GitHub branch/default-branch/required-check protection; desired policy alone is not enforcement evidence.

Active workstreams: `android-visual-reference-convergence.md`, `document-ingestion-v2.md`, `failure-diagnostics-hardening.md`, `ombra-to-redactguard-migration.md`, `harness-control-plane-consumer-cutover.md`.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard through these workstreams.
