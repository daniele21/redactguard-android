# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-09-02

## Integrated state

RedactGuard is a standalone Android document-protection product consuming the Harnex Consumer Android SDK over Binder; Harnex owns model selection/configuration, GGUF/runtime, activation and residency.

`dev` includes text/PDF ingestion, PII selection, bounded sequential analysis, atomic validation, privacy-safe diagnostics, masked review, fail-closed redaction/export, adaptive product UI and process-local sensitive state. OCR/VLM, cloud fallback, persisted History and fabricated progress/metrics remain out of scope.

The repository baseline is `repo-template-sw` 0.8.0 with local Android/product-UI customizations. `.engineering/*`, local skills and CI are the operating-contract owners.

## Active Local AI candidate

PR #143 (`feature/local-ai-setup-readiness`) adds:

- `Analizza / AI locale / Impostazioni` top-level navigation;
- passive consumer-safe setup inspection plus fresh fail-closed Analyze preflight;
- Local AI readiness/recovery UI and privacy-safe `AnalysisSetupSnapshot`;
- process-local RedactGuard analysis ownership with Harnex logical-job reattachment;
- Consumer SDK `0.1.0-alpha.9` and Harnex setup-resolution forwarding.

LAS-09 diagnostics hardening is merged into the candidate at `0e35bda0f85435f80f9b87bec296f6193fd91807`. Its exact implementation head `ce87f6f8b3dea9990753dae84e8105c2a097eb06` passed STRONG remote preflight `33621121871`.

LAS-10 is the current executable gate: Two-APK emulator run `33627728348` reruns the exact setup journey so the first typed setup-resolution outcome can select the canonical LAS-11 owner. No Harnex model/runtime/configuration patch is authorized before that typed evidence.

The previous run `33594812860` proved same-signer installation, Binder protocol minor 6, `consumer-setup-resolution-v1`, one matching assigned use case and one validated preset, then stopped at generic `INCOMPATIBLE`. LAS-09 removed the RedactGuard diagnostic-masking path that could hide the original typed Consumer failure.

## Remaining LAS sequence

1. LAS-10: classify the first typed setup-resolution outcome.
2. LAS-11 and LAS-12: after classification, fix only the confirmed canonical owner and separate setup stage, product problem, recovery action and technical identity; run in parallel only when write boundaries are disjoint.
3. LAS-13: cause-specific, accessible Local AI recovery UX.
4. LAS-14: move setup observation/refresh ownership behind `RedactGuardProductViewModel`; Activity remains composition boundary.
5. LAS-08C: exact Two-APK lifecycle journeys for app switch, Activity recreation, Binder rebind, cancel, process loss, pressure and queue/serialization.
6. LAS-07: exact-head automated handoff plus separate representative ARM64/JNI/GGUF/OEM/model-residency evidence.

## Evidence still external

Representative physical-device evidence remains required for ARM64 llama.cpp/JNI/GGUF execution, real model residency/memory reclamation, thermal/OEM lifecycle behavior and final accessibility checks. Emulator evidence must not be promoted to those claims.

Live GitHub branch/default-branch protection also remains external enforcement evidence.

## Boundary

Do not move Harnex model/runtime administration into RedactGuard, persist sensitive document/prompt/finding/output content for recovery, add cloud fallback, or map generic product `INCOMPATIBLE` to an assumed Harnex bug. Product behavior must use typed failure identity, while normal UI must express user-task problems and real recovery actions rather than Binder/Harness internals.

Active workstreams include `local-ai-setup-readiness.md`; other repository workstreams remain independently owned.