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

Canonical `dev` carries the `repo-template-sw` 0.7.0 L1 baseline with `android` and `product-ui` profiles. `.engineering/*`, repository/documentation policy, local skills and CI own the operating contract. `docs/workstreams/repo-template-sw-alignment.md` owns baseline alignment.

## Mobile product experience

PR #95 completed the mobile-experience foundations. PR #96 integrated the first visual-reference convergence wave into `dev`, including branded surfaces, adaptive launcher identity, emulator product journeys, seven-surface Visual Evidence and 14 asserted E2E UI checkpoints.

A subsequent direct comparison against the user-approved visual target found that the first visual gate was insufficiently strict: the current UI is functionally/semantically aligned but still too far from the target composition, hierarchy, illustration treatment and product polish. The target-fidelity correction wave is therefore active in `docs/workstreams/android-visual-reference-convergence.md`.

The original VUI-1 through VUI-6 automation work remains valid evidence for task semantics, build health, state hierarchy and emulator journeys, but it no longer closes the visual-fidelity claim by itself. New slices VUI-8 through VUI-18 own canonical target identity, shared visual primitives/graphics, truthful summary projection, per-surface fidelity, target-comparison evidence and exact-head integration. VUI-7 physical evidence is BLOCKED until the corrected visual candidate is integrated into `dev`, so the physical test is not spent on a known-stale visual implementation.

The Android launcher identity uses the canonical RedactGuard mark through `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`, with legacy fallback plus API 26+ adaptive resources using the brand background and Android safe-area foreground treatment.

Persisted History/bottom navigation, fabricated progress/metrics, OCR/VLM, exact PDF-coordinate preview and cloud fallback remain excluded even when visible in the illustrative target.

RedactGuard resolves `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.6`. Selected PII descriptors become bounded `TaskDefinition` metadata; Harness composes the system prompt and never exposes model IDs, digests, paths or runtime tuning to RedactGuard. PII profiles remain distinct from Host inference presets.

## Current automated evidence

Current integrated `dev` source revision is `467d5e0901f283b82382641648b2c87688f1703f` (PR #96 merge). On that exact post-merge identity:

- repository Validate passed the FULL profile, including repository guards, deterministic Android gates, Android Lint, AndroidTest APK assembly and minified release/R8 packaging;
- identity-bearing debug/release packaging passed and verified source identity;
- pre-merge Emulator E2E passed the required product journeys and 14 asserted production-Compose UI checkpoints;
- pre-merge Visual Evidence retained the seven required emulator screenshots with source/build identity;
- adaptive launcher resources compile/package correctly.

This evidence proves automated behavior/build/evidence plumbing. It does **not** prove the current UI is sufficiently faithful to the approved visual target. The correction wave must produce a new exact-head target-vs-actual fidelity artifact before visual convergence is claimed.

## Active visual-fidelity plan

Current executable slice: `VUI-8` in `docs/workstreams/android-visual-reference-convergence.md`.

Execution order is intentionally parallelized:

1. canonicalize the exact approved target + MATCH/ADAPT/EXCLUDE rubric;
2. in parallel: shared visual system, graphics/icons, truthful summary projection;
3. in parallel: Document/Analysis, Protection, Review/adaptive, Outcome/Recovery;
4. in parallel: target-comparison Visual Evidence v2 and true-journey E2E reconvergence;
5. exact-head selector/preflight/integration to `dev`;
6. VUI-7 physical accessibility/two-APK evidence from corrected `dev`.

## Remaining evidence

1. Complete VUI-8..VUI-18 visual-fidelity correction against the approved target and integrate the exact-head candidate into `dev`.
2. Record VUI-7 from corrected `dev` on a named physical device: TalkBack, large text, compact-landscape/adaptive behavior and representative OEM launcher rendering.
3. Execute the corrected same-signer Harness + RedactGuard candidate on real ARM64 hardware with pasted text, text PDF, request-time PII definitions, runtime readiness, review, cancellation/recovery, Host absence/death/reconnect, export/reopen and cleanup.
4. Apply and verify live GitHub branch/default-branch/required-check protection; desired-state validation alone is not enforcement evidence.

Active workstreams: `android-visual-reference-convergence.md`, `document-ingestion-v2.md`, `failure-diagnostics-hardening.md`, `ombra-to-redactguard-migration.md`, `harness-control-plane-consumer-cutover.md`.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard through these workstreams.