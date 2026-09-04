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

PR #143 (`feature/local-ai-setup-readiness`) is the active LAS integration candidate. The complete executable checkpoint proven by the current automated matrix is RedactGuard `764851a1ac410add5a0d47b9ce16823e559dbdad`, targeting `dev@3916de75cfa9aa8c64def97b22b72c06a09d80a8`, with exact Harnex Host + Consumer SDK candidate PR #527 at `e3fbf74663a50f02bf75a637b46c9a87bc3289a7`.

The candidate includes top-level `Analizza / AI locale / Impostazioni` navigation, passive consumer-safe setup inspection, fresh fail-closed Analyze preflight, privacy-safe `AnalysisSetupSnapshot`, typed setup/product failure identity, cause-specific recovery, ProductViewModel-owned setup observation/refresh, process-local RedactGuard analysis ownership and Harnex durable logical-job reattachment.

LAS-10 through LAS-14 are implemented in the candidate: typed setup classification selected the canonical owners; setup stage, product problem, recovery action and technical identity were separated; cause-specific accessible recovery was added; setup orchestration moved behind `RedactGuardProductViewModel`.

## Automated lifecycle evidence

On executable RedactGuard HEAD `764851a1ac410add5a0d47b9ce16823e559dbdad`:

- Repository health #414: passed;
- Validate #935: passed with `FULL / iteration`;
- Two-APK emulator E2E #137: passed on API 35 using exact Harnex Host + Consumer SDK candidate `e3fbf74663a50f02bf75a637b46c9a87bc3289a7`.

Two-APK #137 proves the exercised Android/Binder/job semantics for Host absence, the same-signer cross-process product journey, ViewModel reattach/Home-switch continuity, explicit cancel, Binder loss/reconnect, Harnex process loss/restart, `RUNNING_CRITICAL` interruption, RedactGuard process loss and multi-consumer deterministic queue/serialization.

The serialization journey uses two independent Binder Consumer registrations. With the first job blocked inside Host generation, the second Consumer completes its own connect/capabilities/prepare/submit lifecycle and remains accepted in `PREPARING` with no error while Host generation waiters remain exactly `1`. After releasing the first job, the second reaches `SUCCEEDED` and waiters return to `0`. This closes LAS-08C item 8 without changing RedactGuard production ownership or Harnex runtime semantics.

## LAS status

LAS-08C is complete. The complete currently defined automated lifecycle/fault matrix is green.

LAS-07 is now active and owns the remaining representative real-environment evidence only:

1. physical Android ARM64 execution through the production llama.cpp/JNI backend with real GGUF bytes;
2. real model residency/decode lifecycle and cleanup on device;
3. physical memory pressure/reclamation evidence where claimed;
4. thermal and OEM-specific background/process behavior where claimed;
5. final representative-device accessibility/usability confirmation where required by the affected journey.

This real-environment work is deliberately separate from emulator evidence. The emulator result must not be promoted to ARM64/JNI/GGUF, physical memory, thermal or OEM claims.

## Integration readiness

The executable candidate and both target `dev` bases are unchanged from the validated identities above. PR #143 and Harnex PR #527 remain draft while LAS-07 physical-device evidence and final integration bookkeeping are completed.

A documentation-only reconciliation after the executable checkpoint does not invalidate the already-proven executable behavior, but the current branch head still requires the repository-selected lightweight documentation/preflight checks before any final merge-readiness claim.

## Boundary

Do not move Harnex model/runtime administration into RedactGuard, persist sensitive document/prompt/finding/output content for recovery, add cloud fallback, or map generic product incompatibility to an assumed Harnex bug. Product behavior must use typed failure identity; normal UI must express user-task problems and real recovery actions rather than Binder/Harnex internals.

Active workstreams include `local-ai-setup-readiness.md`; other repository workstreams remain independently owned.
