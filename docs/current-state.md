# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-09-04

## Integrated state

RedactGuard is a standalone Android document-protection product consuming the Harnex Consumer Android SDK over Binder; Harnex owns model selection/configuration, GGUF/runtime, activation and residency.

The Local AI setup/readiness and lifecycle work from PR #143 is integrated together with text/PDF ingestion, PII selection, bounded sequential analysis, atomic validation, privacy-safe diagnostics, masked review, fail-closed redaction/export, adaptive product UI and process-local sensitive state. OCR/VLM, cloud fallback, persisted History and fabricated progress/metrics remain out of scope.

The validated product baseline from source identity `0e329c49e8ce5985b3677e9ca5566bc3cb6f3b96` has been promoted to the stable `main` line. The resulting promotion merge was synchronized back into `dev` through PR #196, restoring explicit shared ancestry for the next development cycle.

The repository baseline is `repo-template-sw` 0.9.1 with local Android/product-UI customizations. `.engineering/*`, local skills and CI are the operating-contract owners.

## Local AI release baseline

The production dependency is `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.10` and the canonical Harnex integration source identity is `6b34fe9fcba70f6b8abd107fd58b61c418ac737d`.

PR #143 is integrated. Its product behavior includes:

- top-level `Analizza / AI locale / Impostazioni` navigation;
- passive consumer-safe setup inspection without activation side effects;
- fresh fail-closed Analyze preflight;
- privacy-safe `AnalysisSetupSnapshot`;
- typed setup/product failure identity and cause-specific recovery;
- ProductViewModel-owned setup observation/refresh;
- process-local RedactGuard analysis ownership;
- Harnex durable logical-job reattachment across ordinary UI/Binder detachment.

## Automated, publication and release evidence

The final PR candidate passed repository health, FULL integration validation and the complete Two-APK emulator lifecycle/fault/serialization matrix against Harnex `6b34fe9f...`.

After integration, exact source `0e329c49...` passed repository `Validate` push run #949 and Google Play Internal Testing publication run #4. The subsequent direct `dev -> main` promotion passed Repository health #427 and RELEASE/FULL Validate #953 before PR #195 was merged to `main`.

The automated matrix proves Host absence, same-signer cross-process product flow, ViewModel/Home continuity, Binder loss/reconnect without implicit cancellation, explicit cancellation, Host process loss/restart with structured interruption, critical-pressure interruption, RedactGuard process-loss privacy behavior and independent-consumer deterministic serialization on API 35.

A representative manual product run has additionally confirmed that the app works end to end on a real Android device. That is useful product acceptance evidence, but it is not silently promoted into the formal LAS-07 ARM64/GGUF/memory/thermal/OEM evidence bundle unless the canonical runbook identity and scenario requirements are captured.

## LAS status

LAS-00..06, LAS-08A/B/C and LAS-09..14 are complete. The automated setup/readiness/background/process lifecycle work is therefore closed as an implementation and deterministic-validation outcome.

LAS-07 remains the only formal representative real-environment gate and owns claims that cannot be established by emulator CI or an unrecorded manual smoke/product run:

1. physical Android ARM64 execution through production llama.cpp/JNI with a real compatible GGUF;
2. real model residency/decode/cancel/cleanup lifecycle;
3. physical memory pressure/reclamation where claimed;
4. thermal and OEM-specific background/process behavior where claimed;
5. representative-device accessibility/usability confirmation where required.

The canonical runbook is `docs/evidence/physical-two-apk.md`.

## Stable release state

The validated RedactGuard baseline is now on `main`. The promotion preserved the prior main-only hotfix history, passed the repository RELEASE/FULL gate on the exact candidate, and was synchronized back into `dev` after merge according to the repository integration policy.

Harnex has completed the equivalent stable-line promotion and post-promotion synchronization. RedactGuard and Harnex therefore now share a stable, promoted cross-repository baseline for further product/evidence work.

## Immediate next block

1. keep LAS-07 as separate representative-device evidence rather than blocking truthful automated/product-functionality claims already established;
2. continue OMBRA/product-quality and independent UX/product work from the stable promoted baseline;
3. preserve the existing RedactGuard/Harnex ownership boundary and exact evidence identity in future runtime/Consumer SDK changes.

Do not move Harnex model/runtime administration into RedactGuard, persist sensitive document/prompt/finding/output content for recovery, add cloud fallback, or map generic product incompatibility to an assumed Harnex bug. Product behavior must use typed failure identity; normal UI must express user-task problems and real recovery actions rather than Binder/Harnex internals.
