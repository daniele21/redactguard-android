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

PR #95 completed the mobile-experience foundations. The PR #96 visual-reference convergence wave is integrated into `dev`: it preserves the settled task model and alpha.6 runtime boundary while converging Document -> Protection -> Analysis -> Review -> Outcome, recovery states and adaptive review on the approved Android reference. `docs/workstreams/android-visual-reference-convergence.md` remains authoritative because the physical-device slice is still active.

The automated visual-convergence layers VUI-1 through VUI-6 are complete. Exact-head repository validation, seven-surface emulator visual evidence and the required deterministic emulator product journeys with asserted UI checkpoints passed on the reviewable automated candidate. The remaining VUI-7 gate is physical: named-device TalkBack, large-text, compact-landscape/adaptive behavior, representative OEM launcher rendering and the real two-APK Harness/RedactGuard flow still require representative-device evidence. Do not claim end-to-end UX/UI completion until VUI-7 is recorded.

The Android launcher identity now uses the canonical RedactGuard mark through `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`, with legacy fallback plus API 26+ adaptive resources using the brand background and Android safe-area foreground treatment.

Persisted History/bottom navigation, fabricated progress/metrics, OCR/VLM, exact PDF-coordinate preview and cloud fallback remain excluded.

RedactGuard resolves `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.6`. Selected PII descriptors become bounded `TaskDefinition` metadata; Harness composes the system prompt and never exposes model IDs, digests, paths or runtime tuning to RedactGuard. PII profiles remain distinct from Host inference presets.

## Candidate evidence

The current executable automated visual-convergence candidate is source revision `ac0f905a39e20510879d6862a1ed3cf2ea28a1a4`:

- Repository Health and repository formatting passed, with formatting leaving the exact head unchanged;
- repository Validate passed the FULL profile, including repository guards, deterministic Android gates, Android Lint, AndroidTest APK assembly and minified release/R8 packaging;
- Emulator E2E passed the required product journeys and the 14 asserted production-Compose UI checkpoints, with fail-closed PNG/metadata collection;
- Visual Evidence retained all seven required reference screenshots with source/build identity on API 35;
- the adaptive launcher resources compile and package through the same FULL validation evidence.

This is automated evidence only. Emulator rendering does not prove physical accessibility, representative OEM launcher appearance, real Binder/native/model inference, representative-device performance or usability.

The earlier pre-visual package candidate remains lineage evidence only:

- Harness: `a30f67b21e24adc6efea838e9a9d65cc78446f28`, v31/1.0.0, package run `33159622580` passed.
- RedactGuard: `4679c23a9a22e5242761fe52af97f4eb7432aec7`, v11/0.1.4, package run `33161250690` passed; release-ci SHA-256 `7494948bb3f707e1682923aace289d44b9d726f6314d88e3865a2c638e8f738f`.

## Remaining evidence

1. Record VUI-7 directly from current `dev` on a named physical device: TalkBack, large text, compact-landscape/adaptive behavior and representative OEM launcher rendering across the critical product surfaces.
2. Execute the current same-signer Harness + RedactGuard candidate from `dev` on real ARM64 hardware with pasted text, text PDF, request-time PII definitions, runtime readiness, review, cancellation/recovery, Host absence/death/reconnect, export/reopen and cleanup.
3. Apply and verify live GitHub branch/default-branch/required-check protection; desired-state validation alone is not enforcement evidence.

Active workstreams: `android-visual-reference-convergence.md`, `document-ingestion-v2.md`, `failure-diagnostics-hardening.md`, `ombra-to-redactguard-migration.md`, `harness-control-plane-consumer-cutover.md`.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard through these workstreams.