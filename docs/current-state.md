# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-08-28

## Integrated product state

RedactGuard is a standalone Android document-protection product consuming the Harness Consumer Android SDK over Binder; Harness retains model/runtime/GGUF/residency ownership.

Implemented capabilities include PDF/pasted-text ingestion, deterministic built-in/custom PII selection, Consumer SDK `0.1.0-alpha.6` task metadata, runtime readiness, bounded sequential analysis, atomic result validation, privacy-safe diagnostics, masked review, fail-closed redaction/export, adaptive Review, SAF PDF export and process-local sensitive state. OCR/VLM and cloud fallback remain out of scope.

## Engineering baseline

`dev` carries `repo-template-sw` 0.7.0 L1 with `android` + `product-ui`. `.engineering/*`, local skills and CI own the operating contract.

## Mobile product experience

PR #96 integrated the first visual wave into `dev`: branded surfaces, adaptive launcher identity, emulator product journeys, seven Visual Evidence surfaces and 14 asserted E2E UI checkpoints.

Direct comparison with the user-approved target found that this gate was too weak: current UI is functionally/semantically aligned but still too far from target composition, hierarchy, graphics and product polish. `docs/workstreams/android-visual-reference-convergence.md` therefore owns an active fidelity-correction wave.

Original VUI-1..6 evidence remains valid for semantics/build/E2E, but no longer closes visual fidelity. VUI-8..18 now own target identity, shared visual system/graphics, truthful summary projection, per-surface fidelity, target-comparison evidence and exact-head integration. VUI-7 physical evidence is BLOCKED until corrected UI reaches `dev`.

Launcher identity already uses `@mipmap/ic_launcher` / `ic_launcher_round` with legacy + API26 adaptive resources.

Still excluded even when shown in the illustrative target: persisted History/bottom nav, fabricated progress/metrics, OCR/VLM, exact PDF-coordinate preview, cloud fallback and unowned Options/Settings.

## Automated evidence

Integrated `dev` is `467d5e0901f283b82382641648b2c87688f1703f` (PR #96 merge). Exact-head post-merge Validate FULL and identity-bearing debug/release packaging passed, including Lint, AndroidTest APK and minified release/R8. Pre-merge E2E passed the required product journeys + 14 UI checkpoints; Visual Evidence retained seven screenshots with source/build identity.

This proves behavior/build/evidence plumbing, not sufficient target fidelity. The correction wave must produce a new target-vs-actual artifact before visual convergence is claimed.

## Active visual-fidelity plan

Current executable slice: `VUI-8`.

Execution:
1. canonical target + MATCH/ADAPT/EXCLUDE rubric;
2. shared visual system + graphics + truthful summary projection in parallel;
3. Document/Analysis + Protection + Review/adaptive + Outcome/Recovery in parallel;
4. Visual Evidence v2 + E2E reconvergence in parallel;
5. exact-head integration to `dev`;
6. VUI-7 physical evidence from corrected `dev`.

## Remaining evidence

1. Complete VUI-8..18 and integrate the corrected target-fidelity candidate into `dev`.
2. Run VUI-7 on a named physical device: TalkBack, large text, compact landscape/adaptive behavior and OEM launcher rendering.
3. Run same-signer Harness + RedactGuard on real ARM64 with pasted text, text PDF, request-time PII definitions, readiness, review, cancellation/recovery, Host absence/death/reconnect, export/reopen and cleanup.
4. Verify live GitHub branch/default-branch/required-check protection; desired policy alone is not enforcement evidence.

Active workstreams: `android-visual-reference-convergence.md`, `document-ingestion-v2.md`, `failure-diagnostics-hardening.md`, `ombra-to-redactguard-migration.md`, `harness-control-plane-consumer-cutover.md`.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard through these workstreams.