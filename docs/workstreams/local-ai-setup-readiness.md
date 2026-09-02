# Local AI Setup & Readiness (LAS)

Status: active
Owner: RedactGuard + Harnex integration boundary
Last reviewed: 2026-09-02

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

LAS parent PR #143 consumes Consumer SDK `0.1.0-alpha.9`. Harnex source `f86b53ad29d2396660f095d5eaadd41c19bda8c7` includes the concrete setup-resolution Binder forwarding required by the candidate.

Previous Two-APK run `33594812860` proved exact Host build, Host-absent fail-closed behavior, same-signer permission, Binder minor 6 with `consumer-setup-resolution-v1`, one assigned use case and one validated published preset. It then stopped at generic `INCOMPATIBLE` with no setup-resolution diagnostic.

LAS-09 fixed the RedactGuard masking hazard where diagnostic construction could throw before the original typed `ConsumerControlPlaneFailure` was preserved. PR #156 is merged into the LAS parent at `0e35bda0f85435f80f9b87bec296f6193fd91807`; its exact implementation head passed STRONG remote preflight `33621121871`.

LAS-10 is now the executable evidence gate through Two-APK run `33627728348`. No speculative model/runtime/configuration fix is allowed before its first typed setup-resolution outcome is classified.

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
| LAS-10 | Exact Two-APK typed setup classification | E2E evidence | 09 | ACTIVE |
| LAS-11 | Fix only confirmed canonical setup owner | selected by 10 | 10 | BLOCKED |
| LAS-12 | Separate stage/problem/recovery/technical identity | RedactGuard domain/mapping | 10 | BLOCKED |
| LAS-13 | Cause-specific accessible recovery UX | RedactGuard UI | 11,12 | BLOCKED |
| LAS-14 | Move setup observation/refresh behind ProductViewModel | RedactGuard | 12,13 | BLOCKED |
| LAS-08C | Complete lifecycle Two-APK journeys | both repos/E2E | 11,13 | BLOCKED |
| LAS-07 | Final automated + representative physical evidence | both repos | all above | BLOCKED |

LAS-11 and LAS-12 may run in parallel after LAS-10 only when their write boundaries are disjoint.

## LAS-10 owner classification

| Typed result | Inspect first |
| --- | --- |
| `MODEL_UNAVAILABLE` / `MODEL_CONFLICT` | Harnex resolver/model-store/runtime owner |
| `CONFIGURATION_REQUIRED`, `USE_CASE_NOT_ASSIGNED`, `PRESET_NOT_EXPOSED`, `STALE_REVISION` | Harnex published state plus RedactGuard request/revision consumer |
| `INVALID_REQUEST` | RedactGuard request construction, then wire mapping |
| `TRANSPORT_FAILURE` | Binder callback/epoch/timeout boundary |
| `FEATURE_UNAVAILABLE` | negotiated feature/client delegation/artifact identity |
| `RUNTIME_FAILURE` | Harnex control-plane runtime after request/transport validity |
| `Resolved` then RedactGuard failure | RedactGuard resolved-setup mapping/readiness observer |

If the rerun fails before a typed setup outcome, classify that new failure separately rather than applying LAS-11 by assumption.

## LAS-12 semantic contract

Target state carries four independent dimensions:

1. **Setup stage** — disconnected/connected/configured/compatible; runtime readiness and final Analyze readiness remain separate.
2. **Product problem** — e.g. Host unavailable, configuration required/changed, model unavailable, true incompatibility, transient runtime/transport failure, unexpected failure.
3. **Recovery action** — a typed action RedactGuard can genuinely perform or route; no fake CTA.
4. **Technical identity** — bounded typed Consumer/runtime identity for diagnostics/support only.

Acceptance:

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

Automate at minimum:

1. Home/app switch -> same job/result;
2. Activity recreation -> same job, no duplicate inference;
3. Binder disconnect/rebind -> no implicit durable-job cancellation;
4. explicit cancel -> exact terminal state + cleanup;
5. Harnex process loss -> structured interruption + safe recovery;
6. RedactGuard process loss -> source/privacy-aware recovery;
7. critical pressure -> structured interruption;
8. multiple jobs/consumers -> deterministic queue/serialization.

Two-APK emulator proves Android/Binder/job semantics only. Physical evidence separately owns real ARM64/JNI/GGUF residency, memory reclamation, thermal and OEM claims.

## Validation

Overall LAS depth is STRONG. Individual slices use repository `auto` and may escalate; silent downgrade is forbidden. Deterministic Android/Gradle/Binder/emulator gates unavailable locally are `REMOTE_AUTOMATED`, not user-delegated.

Before each merge: refresh base, review full diff, confirm documentation impact, run selected exact-head automated gates and keep physical/runtime fidelity gaps explicit.

Durable destinations after behavior stabilizes: `design/ux-contract.json`, `docs/features/product-ui.md`, Local AI/analysis feature owners, both `.engineering/e2e.json`, `docs/current-state.md`, and affected tests/contracts.