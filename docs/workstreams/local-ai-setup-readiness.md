# Local AI Setup & Readiness (LAS)

Status: active — implementation, automated lifecycle and stable release promotion complete; LAS-07 physical evidence remains
Owner: RedactGuard + Harnex integration boundary
Last reviewed: 2026-09-04

## Goal

Make Local AI setup explicit, truthful and actionable before analysis, and keep accepted user-initiated work alive across ordinary UI/Binder detachment.

Passive inspection may reach `Compatible` but must never activate, prepare, load or retain a model. Only a fresh fail-closed preflight immediately before inference may produce `Ready to Analyze`.

Execution contract:

`Ready -> Analysis Job Active -> observer attached/detached -> Completed | Cancelled | Interrupted`

## Invariants

- Harnex owns application/use-case/preset/model/runtime/residency truth and exposes only versioned consumer-safe state.
- RedactGuard owns document ingestion, product analysis orchestration, review/export and user-facing recovery.
- Known failure identity remains stable across Harnex -> Binder -> Consumer SDK -> RedactGuard.
- Diagnostics are observational and cannot change a domain outcome.
- Product logic never parses free-form Host/Binder messages or diagnostic strings.
- Binder/UI attachment is not durable-job ownership.
- Harnex process death is a truthful interruption boundary; native state is never reported as surviving when it cannot.
- Sensitive document text, prompts, findings and raw output remain process-local unless a separate privacy decision changes that boundary.
- Emulator evidence does not establish ARM64/JNI/GGUF/OEM/thermal behavior.

## Integrated checkpoint

PR #143 is integrated from RedactGuard source identity `0e329c49e8ce5985b3677e9ca5566bc3cb6f3b96`.

The validated RedactGuard baseline consumes public Harnex Consumer SDK `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.10` and pins the canonical Two-APK host to Harnex source identity `6b34fe9fcba70f6b8abd107fd58b61c418ac737d`.

The final PR candidate passed FULL integration validation and the complete Two-APK API 35 lifecycle/fault/serialization matrix. The integrated source then passed `Validate` push run #949 and Google Play Internal Testing publication run #4.

The RedactGuard baseline subsequently passed Repository health #427 and RELEASE/FULL Validate #953, was promoted to stable `main` through PR #195, and the resulting promotion merge was synchronized back into `dev` through PR #196. Harnex completed the equivalent stable promotion and post-promotion synchronization through PRs #530 and #531.

A representative manual product run has also confirmed that the app works end to end on a real device. This closes the practical product-functionality question that motivated the latest fixes, while the formal LAS-07 evidence identity remains separate.

## Work graph

| ID | Work | State |
| --- | --- | --- |
| LAS-00..06 | Setup/readiness UX, projection, preflight, navigation, snapshot | DONE |
| LAS-08A | Durable Harnex logical-job lifetime | DONE |
| LAS-08B | RedactGuard product job ownership + reattachment | DONE |
| LAS-09..14 | Typed diagnostics, owner classification, semantic split, recovery UX, ProductViewModel ownership | DONE |
| LAS-08C | Complete lifecycle Two-APK journeys | DONE |
| LAS-07 | Representative physical evidence | ACTIVE |

No additional automated lifecycle/runtime patch is indicated by the current evidence. LAS-07 is now the only open LAS gate.

## Stable semantic and UX contract

Setup stage, product problem, recovery action and technical identity remain separate. Missing configuration/model and transient failures are not incompatibility; recovery uses typed actions rather than message parsing; passive `COMPATIBLE` never means final readiness; only fresh Analyze preflight may produce `READY_TO_ANALYZE`.

## Automated lifecycle contract

The complete emulator matrix covers:

1. Home/app switch -> same job/result;
2. Activity/ViewModel recreation -> same job, no duplicate inference;
3. Binder disconnect/rebind -> no implicit durable-job cancellation;
4. explicit cancel -> exact terminal state + cleanup;
5. Harnex process loss -> structured interruption + safe recovery;
6. RedactGuard process loss -> source/privacy-aware recovery;
7. critical pressure -> structured interruption;
8. multiple jobs/consumers -> deterministic queue/serialization.

This matrix is green against Harnex source `6b34fe9f...` and the alpha.10 Consumer SDK identity used by the promoted RedactGuard baseline.

## LAS-07 representative physical evidence

Remaining evidence is `REAL_ENVIRONMENT`, not a missing deterministic product gate. The canonical runbook is `docs/evidence/physical-two-apk.md` and combines:

1. Harnex native ARM64 evidence with a real compatible GGUF through production JNI/llama.cpp;
2. RedactGuard + Harnex exact same-signer release APK evidence through the real Consumer SDK/Binder/product journey.

Physical evidence owns ARM64/JNI/GGUF execution, real model lifecycle, physical Home/return behavior, memory reclamation where asserted, thermal/OEM observations where asserted and representative accessibility/usability where required. One device is representative evidence, not universal OEM coverage.

A normal successful product run is valuable manual acceptance evidence, but LAS-07 becomes complete only when the required source/APK/model/device identities and scenario attestations are captured by the canonical runbook.

## Release closure

The implementation and stable release work is complete. The remaining sequence is now:

1. retain this workstream only while LAS-07 remains open;
2. capture LAS-07 only for the representative real-environment claims it genuinely owns;
3. after LAS-07 is captured and durable evidence/state docs are updated, delete this completed workstream by default according to repository documentation policy.
