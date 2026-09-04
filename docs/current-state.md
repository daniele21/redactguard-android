# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-09-04

## Integrated state

RedactGuard is a standalone Android document-protection product consuming the Harnex Consumer Android SDK over Binder; Harnex owns model selection/configuration, GGUF/runtime, activation and residency.

`dev` includes text/PDF ingestion, PII selection, bounded sequential analysis, atomic validation, privacy-safe diagnostics, masked review, fail-closed redaction/export, adaptive product UI and process-local sensitive state. OCR/VLM, cloud fallback, persisted History and fabricated progress/metrics remain out of scope.

The repository baseline is `repo-template-sw` 0.9.1 with local Android/product-UI customizations. `.engineering/*`, local skills and CI are the operating-contract owners.

## Active Local AI candidate

PR #143 (`feature/local-ai-setup-readiness`) is the active LAS integration candidate. Its product behavior implements top-level `Analizza / AI locale / Impostazioni` navigation, passive consumer-safe setup inspection, fresh fail-closed Analyze preflight, privacy-safe `AnalysisSetupSnapshot`, typed setup/product failure identity, cause-specific recovery, ProductViewModel-owned setup observation/refresh, process-local RedactGuard analysis ownership and Harnex durable logical-job reattachment.

The publication-convergence slice now consumes the public Harnex Consumer SDK `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.10` and pins the canonical Two-APK emulator builder to integrated Harnex `dev@6b34fe9fcba70f6b8abd107fd58b61c418ac737d`.

Harnex `6b34fe9f...` is already published through the repository-owned release paths:

- Consumer SDK `0.1.0-alpha.10`: published and proven consumable without repository credentials;
- Harnex phone-test: published successfully to Google Play Internal Testing.

## Automated lifecycle evidence

The previously proven product-executable RedactGuard checkpoint is `764851a1ac410add5a0d47b9ce16823e559dbdad`. RedactGuard final closeout HEAD `effd57f1723cffb56c45224a09e87d3f454f7827` passed exact-head remote preflight #946 and Two-APK emulator E2E #144 against Harnex source `e3fbf74663a50f02bf75a637b46c9a87bc3289a7`.

That matrix proved Host absence, same-signer cross-process product flow, ViewModel/Home continuity, explicit cancel, Binder loss/reconnect, Host process loss/restart with structured interruption, critical-pressure interruption, RedactGuard process loss/privacy behavior and independent-consumer deterministic serialization.

The current alpha.10/integrated-Harnex convergence slice changes dependency/build identity and the Two-APK source pin, not RedactGuard runtime ownership. Because those are material integration inputs, the current branch HEAD must obtain fresh repository-selected exact-head validation and a fresh Two-APK run against Harnex `6b34fe9f...` before merge.

## LAS status

LAS-08C is complete. LAS-07 remains the only open LAS gate and owns representative real-environment evidence only:

1. physical Android ARM64 execution through production llama.cpp/JNI with a real compatible GGUF;
2. real model residency/decode/cancel/cleanup lifecycle;
3. physical memory pressure/reclamation where claimed;
4. thermal and OEM-specific background/process behavior where claimed;
5. representative-device accessibility/usability confirmation where required.

The canonical physical runbook is `docs/evidence/physical-two-apk.md`. Play Internal builds are useful for phone testing, but they do not by themselves establish the same-signer two-APK Binder claim or ARM64/GGUF/resource evidence.

## Integration readiness

Next sequence:

1. validate the current RedactGuard alpha.10 + Harnex `6b34fe9f...` convergence HEAD with repository-selected deterministic gates and Two-APK E2E;
2. integrate PR #143 to `dev` only after exact-head evidence is green;
3. verify the repository-owned RedactGuard Play Internal publication from integrated `dev`;
4. execute LAS-07 on representative physical hardware using exact identity-bearing artifacts.

Do not move Harnex model/runtime administration into RedactGuard, persist sensitive document/prompt/finding/output content for recovery, add cloud fallback, or map generic product incompatibility to an assumed Harnex bug. Product behavior must use typed failure identity; normal UI must express user-task problems and real recovery actions rather than Binder/Harnex internals.

Active workstreams include `local-ai-setup-readiness.md`; other repository workstreams remain independently owned.
