# Local AI Setup & Readiness (LAS)

Status: active
Owner: RedactGuard + Harnex integration boundary
Last reviewed: 2026-09-04

## Goal

Make Local AI setup explicit, truthful and actionable before analysis, and keep accepted user-initiated work alive across ordinary UI/Binder detachment.

Passive inspection may reach `Compatible` but must never activate, prepare, load or retain a model. Only a fresh fail-closed preflight immediately before inference may produce `Ready to Analyze`.

Execution contract:

`Ready -> Analysis Job Active -> observer attached/detached -> Completed | Cancelled | Interrupted`

The setup shown to the user must match the privacy-safe immutable execution snapshot recorded for that analysis.

## Invariants

- Harnex owns application/use-case/preset/model/runtime/residency truth and exposes only versioned consumer-safe state.
- RedactGuard owns document ingestion, product analysis orchestration, review/export and user-facing recovery.
- Known failure identity remains stable across Harnex -> Binder -> Consumer SDK -> RedactGuard.
- Diagnostics are observational and cannot change a domain outcome.
- Product logic never parses free-form Host/Binder messages or diagnostic strings.
- `INCOMPATIBLE` is reserved for actual protocol/capability incompatibility.
- Setup stage, product problem, recovery action and technical identity are separate concerns.
- Binder/UI attachment is not durable-job ownership.
- Harnex process death is a truthful interruption boundary; native state is never reported as surviving when it cannot.
- Sensitive document text, prompts, findings and raw output remain process-local unless a separate privacy decision changes that boundary.
- Emulator evidence does not establish ARM64/JNI/GGUF/OEM/thermal behavior.

## Current checkpoint

LAS parent PR #143 is on `feature/local-ai-setup-readiness`. The complete product-executable RedactGuard checkpoint proven by the automated matrix is `764851a1ac410add5a0d47b9ce16823e559dbdad`, targeting `dev@3916de75cfa9aa8c64def97b22b72c06a09d80a8`.

The exact Harnex candidate is PR #527 at `e3fbf74663a50f02bf75a637b46c9a87bc3289a7`, targeting `dev@d4f2d40685e3f7b18f733f53c47c302bb5bbebe1`. The Two-APK builder exports Host APK and Consumer SDK from that same revision.

Exact executable automated evidence is green:

- RedactGuard Repository health #414;
- RedactGuard Validate #935 (`FULL / iteration`);
- Harnex Repository health #887;
- Harnex Validate #3817;
- Harnex Consumer SDK validation #352;
- Harnex phone cold-start API 35 emulator #293;
- RedactGuard Two-APK emulator E2E #137.

Two-APK #137 passed Host absence, cross-process product flow, ViewModel/Home continuity, explicit cancel, Binder loss/reconnect, Host process loss/restart, `RUNNING_CRITICAL`, RedactGuard process loss and independent-consumer deterministic queue/serialization.

For serialization, two distinct Binder Consumer registrations are used. While job 1 is blocked in Host generation, job 2 completes its own connect/capabilities/prepare/submit lifecycle and remains `PREPARING` with no error while Host waiters stay `1`. After job 1 is released, job 2 reaches `SUCCEEDED` and waiters return to `0`.

## Work graph

| ID | Work | Owner | Depends on | State |
| --- | --- | --- | --- | --- |
| LAS-00 | Setup/readiness/background UX contract | RedactGuard docs/design | — | DONE |
| LAS-01 | Consumer-safe setup introspection | Harnex | 00 | DONE |
| LAS-02 | `LocalAiSetupState` projection | RedactGuard | 00,01 | DONE |
| LAS-03 | Passive inspection + fresh Analyze preflight | RedactGuard | 02 | DONE |
| LAS-04 | Top-level navigation | RedactGuard | 00 | DONE |
| LAS-05 | Setup/readiness/recovery surface | RedactGuard | 02,03,04 | DONE |
| LAS-06 | `AnalysisSetupSnapshot` | RedactGuard | 03 | DONE |
| LAS-08A | Durable Harnex logical-job lifetime | Harnex | 01 | DONE |
| LAS-08B | Product job ownership + reattachment | RedactGuard | 03,06,08A | DONE |
| LAS-09 | Non-interfering typed diagnostics | RedactGuard | 03 | DONE |
| LAS-10 | Exact Two-APK typed setup classification | E2E evidence | 09 | DONE |
| LAS-11 | Fix only confirmed canonical setup owner | selected by 10 | 10 | DONE |
| LAS-12 | Separate stage/problem/recovery/technical identity | RedactGuard domain/mapping | 10 | DONE |
| LAS-13 | Cause-specific accessible recovery UX | RedactGuard UI | 11,12 | DONE |
| LAS-14 | Move setup observation/refresh behind ProductViewModel | RedactGuard | 12,13 | DONE |
| LAS-08C | Complete lifecycle Two-APK journeys | both repos/E2E | 11,13 | DONE |
| LAS-07 | Final automated + representative physical evidence | both repos | all above | ACTIVE |

LAS-08C is complete. LAS-07 is the only remaining gate and is limited to representative real-environment evidence the emulator cannot establish.

## Typed setup owner classification

The LAS-10 gate is complete; these routing rules remain canonical:

| Typed result | Inspect first |
| --- | --- |
| `MODEL_UNAVAILABLE` / `MODEL_CONFLICT` | Harnex resolver/model-store/runtime owner |
| `CONFIGURATION_REQUIRED`, `USE_CASE_NOT_ASSIGNED`, `PRESET_NOT_EXPOSED`, `STALE_REVISION` | Harnex published state plus RedactGuard request/revision consumer |
| `INVALID_REQUEST` | RedactGuard request construction, then wire mapping |
| `TRANSPORT_FAILURE` | Binder callback/epoch/timeout boundary |
| `FEATURE_UNAVAILABLE` | negotiated feature/client delegation/artifact identity |
| `RUNTIME_FAILURE` | Harnex control-plane runtime after request/transport validity |
| `Resolved` then RedactGuard failure | RedactGuard resolved-setup mapping/readiness observer |

A future failure must be classified from typed evidence before changing model/runtime/configuration ownership.

## Stable semantic and UX contract

LAS-12/13 keep setup stage, product problem, recovery action and technical identity separate. Missing configuration/model and transient failures are not incompatibility; product recovery uses typed actions rather than message parsing; passive `COMPATIBLE` never means final readiness; only fresh Analyze preflight may produce `READY_TO_ANALYZE`. Normal UI uses user-task language and progressively discloses privacy-safe technical identity. Stable details are owned by the Local AI/product feature and UX contracts.

## LAS-08C lifecycle evidence

Required emulator claims are all **green #137**:

1. Home/app switch -> same job/result;
2. Activity/ViewModel recreation -> same job, no duplicate inference;
3. Binder disconnect/rebind -> no implicit durable-job cancellation;
4. explicit cancel -> exact terminal state + cleanup;
5. Harnex process loss -> structured interruption + safe recovery;
6. RedactGuard process loss -> source/privacy-aware recovery;
7. critical pressure -> structured interruption;
8. multiple jobs/consumers -> deterministic queue/serialization.

LAS-08C is DONE. Physical evidence separately owns real ARM64/JNI/GGUF residency, memory reclamation, thermal and OEM claims.

## LAS-07 representative physical evidence

Remaining evidence is `REAL_ENVIRONMENT`, not a missing deterministic product gate. The target is a representative physical Android ARM64 device using exact identity-bearing Harnex and RedactGuard artifacts.

The canonical runbook is `docs/evidence/physical-two-apk.md`. LAS-07 combines:

1. Harnex `capture-device-e2e-evidence.sh` on `arm64-v8a` with a real compatible GGUF, proving production JNI/llama.cpp lifecycle, generation, cancellation, PSS and available thermal evidence.
2. RedactGuard `e2e-redactguard-device.sh` with exact same-signer Harnex + RedactGuard release APKs, proving the real Consumer/Binder/product journey. The runner requires `BACKGROUND_OK` for Android Home/return during active real-Harnex-backed analysis.

The remaining fidelity claims are production ARM64/JNI/GGUF execution; real prepare/residency/decode/cancel/cleanup; cross-process behavior with that native path active; physical Home/app-switch continuity; memory reclamation where asserted; thermal/OEM behavior where asserted; and representative accessibility/usability where required.

One physical device is representative evidence, not universal OEM/API coverage.

## Validation and closure

The product-executable deterministic matrix is green on RedactGuard `764851a1ac410add5a0d47b9ce16823e559dbdad` plus Harnex `e3fbf74663a50f02bf75a637b46c9a87bc3289a7`.

The closeout slice after that checkpoint changes durable docs and the physical evidence runner only; it does not change RedactGuard production/runtime/SDK/dependency behavior. The runner is still executable validation tooling, so the repository selector owns current exact-head deterministic gates and any escalation caused by cumulative PR scope.

Before merge readiness: refresh both PR bases/heads, review complete diffs, keep durable docs/runbook current, obtain exact-head remote preflight for RedactGuard, reuse Harnex exact-head evidence while its source/base remain unchanged, and complete LAS-07 REAL_ENVIRONMENT evidence. Both PRs remain draft until then.

After LAS closes, finalize the active workstream according to repository policy and reconcile the stable feature/UX/E2E owners.
