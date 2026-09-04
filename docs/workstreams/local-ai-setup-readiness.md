# Local AI Setup & Readiness (LAS)

Status: active
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

## Current checkpoint

LAS parent PR #143 remains on `feature/local-ai-setup-readiness`, targeting `dev@3916de75cfa9aa8c64def97b22b72c06a09d80a8` until integration.

The final pre-publication RedactGuard closeout HEAD `effd57f1723cffb56c45224a09e87d3f454f7827` passed exact-head remote preflight #946 and Two-APK emulator E2E #144. That Two-APK run used Harnex source `e3fbf74663a50f02bf75a637b46c9a87bc3289a7` and proved the complete lifecycle/fault/serialization matrix.

Harnex PR #527 has since been integrated to `dev@6b34fe9fcba70f6b8abd107fd58b61c418ac737d`. Repository-owned publication is green for:

- public Consumer SDK `0.1.0-alpha.10`, including unauthenticated external consumption;
- Harnex phone-test Google Play Internal Testing release from the same integrated source.

The current RedactGuard convergence slice therefore moves the normal production dependency from alpha.9 to alpha.10 and pins the Two-APK builder to integrated Harnex `6b34fe9f...`. This changes integration identity, so fresh exact-head deterministic and Two-APK evidence is required before RedactGuard merge.

## Work graph

| ID | Work | State |
| --- | --- | --- |
| LAS-00..06 | Setup/readiness UX, projection, preflight, navigation, snapshot | DONE |
| LAS-08A | Durable Harnex logical-job lifetime | DONE |
| LAS-08B | RedactGuard product job ownership + reattachment | DONE |
| LAS-09..14 | Typed diagnostics, owner classification, semantic split, recovery UX, ProductViewModel ownership | DONE |
| LAS-08C | Complete lifecycle Two-APK journeys | DONE |
| LAS-07 | Representative physical evidence | ACTIVE |

LAS-08C is complete. LAS-07 is the only remaining product-fidelity gate after the current publication-convergence HEAD re-establishes automated exact-head evidence.

## Stable semantic and UX contract

Setup stage, product problem, recovery action and technical identity remain separate. Missing configuration/model and transient failures are not incompatibility; recovery uses typed actions rather than message parsing; passive `COMPATIBLE` never means final readiness; only fresh Analyze preflight may produce `READY_TO_ANALYZE`.

## Automated lifecycle contract

The complete emulator matrix already covers:

1. Home/app switch -> same job/result;
2. Activity/ViewModel recreation -> same job, no duplicate inference;
3. Binder disconnect/rebind -> no implicit durable-job cancellation;
4. explicit cancel -> exact terminal state + cleanup;
5. Harnex process loss -> structured interruption + safe recovery;
6. RedactGuard process loss -> source/privacy-aware recovery;
7. critical pressure -> structured interruption;
8. multiple jobs/consumers -> deterministic queue/serialization.

The current convergence run must replay this matrix against integrated Harnex `6b34fe9f...` and the current RedactGuard exact HEAD because the published Consumer SDK/dependency identity changed to alpha.10.

## LAS-07 representative physical evidence

Remaining evidence is `REAL_ENVIRONMENT`, not a missing deterministic product gate. The canonical runbook is `docs/evidence/physical-two-apk.md` and combines:

1. Harnex native ARM64 evidence with a real compatible GGUF through production JNI/llama.cpp;
2. RedactGuard + Harnex exact same-signer release APK evidence through the real Consumer SDK/Binder/product journey, including `BACKGROUND_OK` during active analysis.

Physical evidence owns ARM64/JNI/GGUF execution, real model lifecycle, physical Home/return behavior, memory reclamation where asserted, thermal/OEM observations where asserted and representative accessibility/usability where required. One device is representative evidence, not universal OEM coverage.

## Validation and closure

Before RedactGuard integration:

1. refresh PR #143 head/base and review the complete diff;
2. obtain repository-selected exact-head preflight for the alpha.10/Harnex `6b34fe9f...` candidate;
3. obtain fresh Two-APK evidence against that exact Harnex identity;
4. merge to `dev` only when deterministic integration evidence is green;
5. verify RedactGuard Play Internal publication from the integrated source.

After the phone candidate is available, execute LAS-07 separately. Play Internal availability does not by itself prove same-signer Binder access or physical ARM64/GGUF/resource claims.
