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

LAS parent PR #143 is on `feature/local-ai-setup-readiness`. The complete executable RedactGuard checkpoint proven by the current automated matrix is `764851a1ac410add5a0d47b9ce16823e559dbdad`, targeting `dev@3916de75cfa9aa8c64def97b22b72c06a09d80a8`.

The exact cross-repository Harnex candidate is PR #527 at `e3fbf74663a50f02bf75a637b46c9a87bc3289a7`, targeting `dev@d4f2d40685e3f7b18f733f53c47c302bb5bbebe1`. The Two-APK candidate builder exports both the Host APK and Consumer SDK Maven candidate from that same Harnex revision; RedactGuard CI compiles against that exact candidate.

Exact executable automated evidence is green:

- RedactGuard Repository health #414;
- RedactGuard Validate #935 (`FULL / iteration`);
- Harnex Repository health #887;
- Harnex Validate #3817;
- Harnex Consumer SDK validation #352;
- Harnex phone cold-start API 35 emulator evidence #293;
- RedactGuard Two-APK emulator E2E #137.

Two-APK #137 passed Host-absent discovery, the same-signer cross-process journey, ViewModel reattach/Home-switch continuity, explicit cancel, Binder loss/reconnect, Host process loss/restart, `RUNNING_CRITICAL` interruption, the two-phase RedactGuard process-loss journey and independent-consumer deterministic queue/serialization.

For serialization, two distinct Binder Consumer registrations are used. While job 1 is blocked inside Host generation, job 2 completes an independent connect/capabilities/prepare/submit lifecycle and remains accepted in `PREPARING` with no error while Host generation waiters remain exactly `1`. After job 1 is released, job 2 reaches `SUCCEEDED` and waiters return to `0`. This is the executable evidence that closes LAS-08C item 8.

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

All currently defined automated lifecycle/fault claims are complete. LAS-07 is the remaining gate and is intentionally limited to representative real-environment evidence that the emulator cannot establish.

## Typed setup owner classification

The LAS-10 classification gate is complete, but these routing rules remain the canonical diagnostic ownership map:

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

## LAS-12 semantic contract

The implemented state keeps four independent dimensions:

1. **Setup stage** — disconnected/connected/configured/compatible; runtime readiness and final Analyze readiness remain separate.
2. **Product problem** — e.g. Host unavailable, configuration required/changed, model unavailable, true incompatibility, transient runtime/transport failure, unexpected failure.
3. **Recovery action** — a typed action RedactGuard can genuinely perform or route; no fake CTA.
4. **Technical identity** — bounded typed Consumer/runtime identity for diagnostics/support only.

Acceptance remains:

- setup progress is not encoded by an error bucket;
- missing configuration/model and transient failures are not represented as incompatibility;
- product recovery uses typed capabilities/actions, never message parsing;
- `COMPATIBLE` stays passive;
- only fresh Analyze preflight may produce `READY_TO_ANALYZE`;
- technical identity remains available without leaking transport jargon into normal UI.

## LAS-13 UX contract

Normal Local AI copy describes the user task:

| Condition | User-facing state | Recovery principle |
| --- | --- | --- |
| Host unreachable | `AI locale non disponibile` | retry connection/refresh |
| Setup incomplete | `Configurazione richiesta` | refresh or supported setup route |
| Revision/preset changed | `Configurazione cambiata` | refresh and show replacement |
| Required model unavailable | `Modello locale non disponibile` | only expose a real Harnex-owned management route |
| True incompatibility | `Versione AI locale non compatibile` | supported update/version recovery |
| Transient runtime/transport | `AI locale temporaneamente non disponibile` | retry without incompatibility claim |
| Passive setup coherent | `Configurazione compatibile` | no final Ready claim |
| Fresh preflight succeeds | `Pronta per l'analisi` | proceed |

State meaning cannot rely on color alone. Error/recovery pairs need reachable text actions. Technical details remain progressively disclosed and privacy-safe.

## LAS-08C lifecycle evidence

Required emulator claims:

1. Home/app switch -> same job/result — **green #137**;
2. Activity/ViewModel recreation -> same job, no duplicate inference — **green #137**;
3. Binder disconnect/rebind -> no implicit durable-job cancellation — **green #137**;
4. explicit cancel -> exact terminal state + cleanup — **green #137**;
5. Harnex process loss -> structured interruption + safe recovery — **green #137**;
6. RedactGuard process loss -> source/privacy-aware recovery — **green #137**;
7. critical pressure -> structured interruption — **green #137**;
8. multiple jobs/consumers -> deterministic queue/serialization — **green #137**.

LAS-08C is DONE. Two-APK emulator proves Android/Binder/job semantics only. Physical evidence separately owns real ARM64/JNI/GGUF residency, memory reclamation, thermal and OEM claims.

## LAS-07 representative physical evidence

Remaining evidence is `REAL_ENVIRONMENT`, not a missing deterministic CI gate. The declared target is a representative physical Android ARM64 device using exact identity-bearing Harnex and RedactGuard artifacts.

Required claims are limited to the fidelity gaps that cannot be established by the x86_64 emulator:

- production ARM64 `llama.cpp`/JNI execution with real GGUF bytes;
- real model prepare/residency/decode/cancel/cleanup lifecycle;
- real cross-process Consumer/Binder behavior while that native path is active;
- physical memory pressure/reclamation where asserted;
- thermal and OEM background/process policy where asserted;
- representative-device accessibility/usability checks where the product journey requires them.

One physical device remains representative evidence, not proof of every supported OEM/API/device combination.

## Validation and remaining closure

Overall LAS automated depth is STRONG and the complete currently defined deterministic matrix is green on executable RedactGuard `764851a1ac410add5a0d47b9ce16823e559dbdad` plus exact Harnex `e3fbf74663a50f02bf75a637b46c9a87bc3289a7`.

The current documentation reconciliation is non-executable. It does not retroactively invalidate the executable E2E result, but its resulting branch HEAD must receive the repository-selected lightweight documentation/preflight checks before any final merge-readiness claim.

Before merge readiness: refresh both PR bases/heads, review the complete diffs, confirm durable documentation is current, reuse sufficient exact executable evidence, execute only missing deterministic checks selected for the docs-only head, and keep LAS-07 physical/runtime fidelity gaps explicit. PR #143 and Harnex PR #527 remain draft until that reconciliation and required real-environment evidence are complete.

Durable destinations after LAS fully closes: `design/ux-contract.json`, `docs/features/product-ui.md`, Local AI/analysis feature owners, both `.engineering/e2e.json`, `docs/current-state.md`, and affected tests/contracts. The active workstream should then be finalized according to repository policy.
