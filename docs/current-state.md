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

PR #143 (`feature/local-ai-setup-readiness`) is the active LAS integration candidate. The executable checkpoint proven by the complete Two-APK matrix is RedactGuard `57b3b3bcc1c65e2ffd3302931cfa863356e624e2`, targeting `dev@3916de75cfa9aa8c64def97b22b72c06a09d80a8`, with exact Harnex Host + Consumer SDK candidate PR #527 at `e3fbf74663a50f02bf75a637b46c9a87bc3289a7`.

The candidate includes top-level `Analizza / AI locale / Impostazioni` navigation, passive consumer-safe setup inspection, fresh fail-closed Analyze preflight, privacy-safe `AnalysisSetupSnapshot`, typed setup/product failure identity, cause-specific recovery, ProductViewModel-owned setup observation/refresh, process-local RedactGuard analysis ownership and Harnex durable logical-job reattachment.

LAS-10 through LAS-14 are implemented in the candidate: typed setup classification selected the canonical owners; setup stage, product problem, recovery action and technical identity were separated; cause-specific accessible recovery was added; setup orchestration moved behind `RedactGuardProductViewModel`.

## Automated lifecycle evidence

On executable RedactGuard HEAD `57b3b3bcc1c65e2ffd3302931cfa863356e624e2`:

- Repository health #408: passed;
- Validate #929: passed with `FULL / iteration`;
- Two-APK emulator E2E #131: passed on API 35 using exact Harnex Host + Consumer SDK candidate `e3fbf74663a50f02bf75a637b46c9a87bc3289a7`.

Two-APK #131 proves the exercised Android/Binder/job semantics for Host absence, the same-signer cross-process product journey, ViewModel reattach/Home-switch continuity, explicit cancel, Binder loss/reconnect, Harnex process loss/restart, `RUNNING_CRITICAL` interruption and RedactGuard process loss. The evidence artifact digest is `sha256:172af566a669e73c4f2ab424f521f680052a89d512ee07c52bcca6799291f706`.

Two-APK #130 established that the API 35 emulator `RUNNING_CRITICAL` test command reaches the Host service: Harnex moved the accepted logical job `RUNNING rev2 -> FAILED_FINAL rev3` and cleaned the generation waiter. The residual timeout was a RedactGuard E2E observation race because the ProductViewModel had already consumed the terminal product snapshot. The current test subscribes before fault injection; no runtime semantic or timeout change was required. #131 validates that correction end to end.

## Remaining LAS work

The complete exercised lifecycle fault matrix is no longer the blocker. Remaining work is narrower:

1. add/record the still-unproven LAS-08C multi-job/consumer deterministic queue/serialization E2E claim;
2. reconcile final durable documentation/preflight after that executable evidence;
3. collect separate representative real-environment evidence for ARM64 llama.cpp/JNI/GGUF execution, real model residency/memory reclamation, thermal/OEM lifecycle behavior and final accessibility/usability checks;
4. then reassess PR #143 and Harnex PR #527 merge readiness against fresh base/head identity.

PR #143 and Harnex PR #527 remain draft. Emulator evidence must not be promoted to representative physical-device claims.

## Boundary

Do not move Harnex model/runtime administration into RedactGuard, persist sensitive document/prompt/finding/output content for recovery, add cloud fallback, or map generic product incompatibility to an assumed Harnex bug. Product behavior must use typed failure identity; normal UI must express user-task problems and real recovery actions rather than Binder/Harnex internals.

Active workstreams include `local-ai-setup-readiness.md`; other repository workstreams remain independently owned.
