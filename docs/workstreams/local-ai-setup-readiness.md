# Local AI Setup & Readiness (LAS)

Status: active
Owner: RedactGuard + Harnex integration boundary
Read when: coordinating Local AI setup/readiness, execution identity, recovery UX, background execution or lifecycle evidence
Last reviewed: 2026-09-02

## Goal

Make Local AI setup explicit, truthful and actionable before analysis, and keep a valid user-initiated analysis alive across ordinary app switching/UI detachment without blurring product readiness, transport health, configuration validity or runtime availability.

The product contract remains:

`Connected -> Configured -> Compatible -> Runtime Ready -> Ready to Analyze`

`Ready to Analyze` is granted only by a fresh fail-closed preflight immediately before inference. Passive Local AI inspection may report setup/configuration compatibility, but never activates/prepares/loads/retains a model merely to make the page look ready.

Execution remains:

`Ready -> Analysis Job Active -> UI/observer attached or detached -> Completed | Cancelled | Interrupted`

The setup shown before analysis must match the privacy-safe immutable execution snapshot recorded for that run.

## Non-goals

- Do not move model selection/configuration, activation, preparation, inference, model residency or native/runtime cleanup from Harnex into RedactGuard.
- Do not make Local AI/Settings inspection create a residency lease or perform hidden model work.
- Do not persist document text, pasted text, prompts, findings or raw model output merely to improve recovery without a separate privacy/security decision.
- Do not infer physical ARM64/JNI/GGUF/OEM behavior from emulator evidence.
- Do not patch Harnex model fixtures/resolver/runtime in response to a generic RedactGuard `INCOMPATIBLE` state; first preserve and classify the typed Consumer failure.
- Do not expose Binder/Harness/SDK implementation vocabulary in normal product copy when a user-task explanation is available.

## Invariants

- Harnex owns application/use-case/preset/model/runtime/residency truth and publishes only versioned consumer-safe state.
- RedactGuard owns document ingestion, analysis orchestration, validation, review/export, product job state and user-facing recovery semantics.
- Known failure identity remains stable/actionable across the Harnex -> Binder -> Consumer SDK -> RedactGuard boundary.
- Diagnostics are observational: constructing, formatting or emitting a technical event must never throw into or change the domain outcome being observed.
- Product logic branches on typed failure identity, never on free-form failure messages or diagnostic strings.
- `INCOMPATIBLE` is reserved for actual compatibility/protocol/capability incompatibility; missing configuration, unavailable model, stale revision and transient runtime/transport problems are distinct product blockers.
- Setup progression, product blocker/recovery and technical diagnostic identity are separate dimensions; one must not be overloaded to represent the others.
- Binder/UI observer attachment is not execution ownership. Ordinary app-switch, Activity recreation or temporary observer detach must not implicitly cancel valid work.
- Active execution protects the model/runtime it needs; idle/warm retention remains separately bounded by Harnex resource policy.
- Real Harnex process death, force-stop, reboot or critical pressure is an interruption boundary; native state is never reported as having survived when it cannot.
- Real RedactGuard process death does not imply transparent recovery of process-local sensitive state.
- Existing Consumer/Binder compatibility remains additive/versioned.

## Current failure checkpoint

RedactGuard LAS parent PR #143 currently has head `0c7aa5dd70043c95db7aca9c5c1e3035300b09ff` and consumes Consumer Android SDK `0.1.0-alpha.9`. Harnex `dev` is `f86b53ad29d2396660f095d5eaadd41c19bda8c7`, which includes PR #511 forwarding concrete Consumer setup resolution through Binder.

Exact RedactGuard Two-APK emulator run `33594812860` (run #82) is a current candidate integration failure, not an environment/bootstrap failure:

- exact Harnex Host artifact build passed;
- Host-absent fail-closed scenario passed;
- same-signer installation and signature permission passed;
- Binder negotiation reached protocol minor 6 and included `consumer-setup-resolution-v1`;
- assigned use-case discovery matched one assignment;
- published preset discovery validated one preset;
- product readiness then timed out at generic `INCOMPATIBLE` / `Configurazione AI non disponibile`;
- diagnostics stop after `control-plane.published-presets result=VALIDATED`; no setup-resolution diagnostic event is emitted.

Source inspection identifies a RedactGuard masking hazard: setup-rejection diagnostics currently combine the typed code with a human message, while the technical-event reason contract accepts only a compact token. A diagnostic construction failure can therefore hide the original `ConsumerControlPlaneFailure` and be remapped to generic incompatibility. The next corrective slice must prove this hypothesis directly before any functional owner is changed.

Failure classification for this checkpoint: `CURRENT_REGRESSION` / candidate integration blocker. Canonical functional owner of the underlying setup rejection is **not yet known** because the typed rejection is currently masked.

## Target setup/readiness model

The implementation should converge on four separate concerns rather than one catch-all discovery state:

1. **Setup stage** — how far passive setup verification progressed, e.g. disconnected/connected/configured/compatible. Runtime-ready and final Analyze readiness stay separate from passive inspection.
2. **Product problem** — the actionable blocker, e.g. Host unavailable, authorization/use-case/preset problem, stale configuration, configuration required, model unavailable, true incompatibility, runtime/transport unavailable or unexpected failure.
3. **Recovery action** — a typed action that RedactGuard can genuinely execute or route, such as refresh/retry or an explicitly supported Local AI management/update path. Never render a CTA that has no real owner/operation.
4. **Technical identity** — the original typed Consumer/Binder/runtime failure code plus bounded privacy-safe context for diagnostics/tests; never parsed by product UI.

The final enum/type names remain implementation details to resolve against existing owners and consumers. The semantic separation above is the invariant.

## UX state and recovery contract

Normal `AI locale` copy should explain the user task, not the transport implementation:

| Condition | Product meaning | Preferred user-facing state | Recovery principle |
| --- | --- | --- | --- |
| Host/service unreachable | Local AI cannot currently be contacted | `AI locale non disponibile` | retry connection/refresh |
| Setup not configured | Consumer setup is incomplete | `Configurazione richiesta` | refresh or supported setup route |
| Revision/preset changed | Previously selected setup is stale | `Configurazione cambiata` | refresh and show replacement when available |
| Required model unavailable | Setup exists but required local resource is unavailable | `Modello locale non disponibile` | only offer a real Harnex-owned management route if supported |
| Actual capability/protocol incompatibility | Consumer and Host cannot satisfy the required contract | `Versione AI locale non compatibile` | supported update/version recovery |
| Transient transport/runtime problem | Valid setup cannot currently execute | `AI locale temporaneamente non disponibile` | retry without claiming incompatibility |
| Passive setup compatible | Published setup is coherent | `Configurazione compatibile` | no final Ready claim |
| Fresh Analyze preflight succeeds | Immediate execution prerequisites are valid | `Pronta per l'analisi` | proceed with inference |

Accessibility requirements: state meaning must not rely on color alone; every error/recovery pair needs a text label and reachable action; refreshed/replaced setup must produce understandable feedback; advanced technical detail remains progressively disclosed and privacy-safe.

## Lifecycle contract

| Event | Target behavior |
| --- | --- |
| Home/app switch | Same analysis continues; active model remains protected; UI reattaches to the same job on return |
| Activity recreation | Same job is observed; no duplicate inference |
| Temporary Binder detach/rebind | Durable Harnex job continues and can be reobserved/cancelled by the same authorized consumer |
| RedactGuard process death | Harnex-owned active inference may finish; product recovery is source/privacy aware and never invents lost sensitive state |
| Harnex process death | Native state is lost; affected job becomes structured interrupted/process-lost state; retry only from a safe boundary |
| Critical memory pressure | System-health policy may cancel/release; surface a structured interruption reason and valid recovery |
| Explicit cancel | Exact job becomes cancelled; resources clean up; idle/warm policy resumes |
| Idle/no job | Model may unload normally |

## Work graph

| ID | Work | Owns/writes | Depends on | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| LAS-00 | Freeze setup/readiness/background UX contract | RedactGuard design/workstream/boundary docs | — | yes | DONE |
| LAS-01 | Add consumer-safe execution-setup introspection | Harnex contracts/Binder/Host/tests/docs | LAS-00 | yes | DONE |
| LAS-02 | Add `LocalAiSetupState` projection | RedactGuard Local AI domain/infrastructure/tests | LAS-00, LAS-01 | yes | DONE |
| LAS-03 | Side-effect-free inspection + fresh fail-closed Analyze preflight | RedactGuard control-plane/runtime/tests | LAS-02 | yes | DONE |
| LAS-04 | Add `Analizza / AI locale / Impostazioni` navigation | RedactGuard Compose/navigation/tests | LAS-00 | yes | DONE |
| LAS-05 | Build Local AI setup/readiness/recovery surface | RedactGuard UI/accessibility/product docs/tests | LAS-02, LAS-03, LAS-04 | no | DONE |
| LAS-06 | Add immutable privacy-safe `AnalysisSetupSnapshot` | RedactGuard orchestration/diagnostics/tests | LAS-03 | yes | DONE |
| LAS-08A | Decouple Harnex durable job lifetime from Binder/UI lifetime and protect active residency | Harnex runtime/service/Consumer logical-job boundary/tests/docs | LAS-01 | no | DONE |
| LAS-08B | Move active RedactGuard analysis ownership out of Activity/ViewModel lifetime and add job reattachment | RedactGuard execution owner/product state/tests/docs | LAS-03, LAS-06, LAS-08A | no | DONE |
| LAS-09 | Make setup diagnostics non-interfering and preserve typed rejection identity | RedactGuard Local AI technical-event contract/coordinator/sink/unit tests | LAS-03 | no | ACTIVE |
| LAS-10 | Re-run exact Two-APK setup journey and classify the first typed setup-resolution failure | RedactGuard E2E evidence only; no speculative functional patch | LAS-09 | no | BLOCKED |
| LAS-11 | Fix the confirmed canonical owner of the typed setup failure | Owner selected by LAS-10: RedactGuard request/mapping **or** Harnex control-plane/model/runtime; direct consumers/tests/docs | LAS-10 | no | BLOCKED |
| LAS-12 | Separate setup stage, product problem, recovery action and technical identity | RedactGuard Local AI domain/mapping/tests; no UI strings as policy | LAS-10 | yes, after owner classification | BLOCKED |
| LAS-13 | Make Local AI UX/recovery cause-specific, consistent and accessible | RedactGuard setup projector/Compose/accessibility/product-experience tests/docs | LAS-11, LAS-12 | no | BLOCKED |
| LAS-14 | Move setup observation/refresh ownership behind `RedactGuardProductViewModel` and leave Activity as composition boundary | RedactGuard Activity/ViewModel/setup state consumers/tests | LAS-12, LAS-13 | no | BLOCKED |
| LAS-08C | Prove app-switch/recreation/Binder-rebind/cancel/process-loss lifecycle journeys on exact Two-APK candidate | RedactGuard + Harnex E2E scripts/workflows/evidence | LAS-11, LAS-13 | no | BLOCKED |
| LAS-07 | Final exact-head automated + representative physical evidence and handoff | both repos | LAS-01..06, LAS-08A..08C, LAS-09..14 | no | BLOCKED |

`LAS-11` and `LAS-12` may proceed in parallel only after `LAS-10` has exposed the typed failure and the write boundaries are disjoint. No Harnex setup-resolution/model/runtime edit may begin merely from the generic `INCOMPATIBLE` symptom.

## Current executable slice

`LAS-09`

Acceptance:

- a rejected `ConsumerSetupResolutionResult` reaches RedactGuard as the same typed `ConsumerControlPlaneErrorCode` regardless of human-message whitespace, Unicode, length or formatting;
- technical-event creation/emission cannot throw into setup/readiness evaluation; a failing diagnostic sink cannot change the domain result;
- setup-resolution emits a bounded event with a stable machine-readable reason code and no raw Host/Binder payload, document/prompt/output content or sensitive filename;
- product readiness semantics and user copy are otherwise unchanged in this slice;
- regression tests cover messages containing spaces, Unicode and over-limit detail as well as a deliberately failing diagnostic sink.

Validation:

- use `python3 scripts/detect_ci_scope.py` with the repository default `auto` selector; expected depth is at least `SCOPED`, with automatic escalation if the technical-event contract reaches broader consumers;
- run affected unit/compile/static/lint gates plus repository guards; unavailable Android gates are `REMOTE_AUTOMATED`;
- after exact-head deterministic validation, run the Two-APK journey only as `LAS-10` evidence, not as a retry without a new hypothesis.

## LAS-10 classification gate

The Two-APK rerun must record the first setup-resolution outcome by typed code. Route the next owner from evidence rather than symptom:

| Typed outcome | Canonical owner to inspect first |
| --- | --- |
| `MODEL_UNAVAILABLE` / `MODEL_CONFLICT` | Harnex setup resolver/model-store/binding/runtime owner |
| `CONFIGURATION_REQUIRED`, `USE_CASE_NOT_ASSIGNED`, `PRESET_NOT_EXPOSED`, `STALE_REVISION` | Harnex control-plane state plus RedactGuard request/revision consumer; determine which side violates the published contract |
| `INVALID_REQUEST` | RedactGuard request construction first, then wire mapping if request is correct |
| `TRANSPORT_FAILURE` | Binder callback/epoch/timeout/transport boundary |
| `FEATURE_UNAVAILABLE` | negotiated feature/client delegation/version artifact identity |
| `RUNTIME_FAILURE` | Harnex setup/control-plane runtime owner after transport/request validity is proven |
| `Resolved` followed by RedactGuard failure | RedactGuard resolved-setup mapping/readiness observer |

If the rerun fails before producing a typed setup-resolution outcome, classify the new failure separately and revise the hypothesis; do not apply LAS-11 by assumption.

## LAS-12 semantic acceptance

- setup progress is not encoded by an error bucket;
- true incompatibility is distinct from missing configuration/model and transient runtime/transport failure;
- product recovery is represented by typed capabilities/actions, not inferred from error-message text;
- `COMPATIBLE` remains passive and does not imply final Analyze readiness;
- only the fresh Analyze preflight may produce `READY_TO_ANALYZE`;
- known technical failure identity remains available for diagnostics/support without leaking implementation jargon into normal UI.

## LAS-08C lifecycle acceptance

Automate at minimum:

1. analysis -> Home/app switch -> work progresses -> return -> same job/result;
2. Activity recreation -> same job;
3. Binder disconnect/rebind -> no implicit durable-job cancellation;
4. explicit cancel -> exact terminal state + cleanup;
5. Harnex process-loss injection -> structured interruption + safe recovery;
6. RedactGuard process-loss injection -> privacy/source-aware recovery;
7. critical-pressure injection -> structured interruption;
8. multiple jobs/consumers -> deterministic queue/serialization.

Two-APK emulator proves Android/Binder/job semantics only. Representative ARM64/JNI/GGUF evidence separately owns real model residency, memory reclamation, thermal and OEM lifecycle claims.

## Integration points

- Harnex PR #511 / Consumer SDK alpha.9 is the current concrete setup-resolution transport prerequisite; do not bypass it with copied Binder/runtime logic in RedactGuard.
- `LAS-09 -> LAS-10` is an observability gate: diagnostics become trustworthy before another functional hypothesis is tested.
- `LAS-10 -> LAS-11` is the canonical-owner gate: only a typed outcome authorizes a functional fix and determines its repository.
- `LAS-12 -> LAS-13` keeps product policy out of Compose: domain mapping owns problem/recovery semantics, UI owns presentation/accessibility.
- `LAS-14` consolidates setup observation/control after semantics stabilize so Activity does not become a second state owner.
- `LAS-08C` begins only after the initial setup path can reach the lifecycle journey; failure before that point is not lifecycle evidence.

## Durable documentation destinations

- `design/ux-contract.json`: durable readiness, background continuity, interruption/recovery semantics if observable behavior changes.
- `docs/features/product-ui.md`: durable Local AI user-facing states/actions after LAS-13.
- RedactGuard Local AI/analysis feature owners: setup/preflight/snapshot and job reattachment when behavior changes.
- Harnex shared-runtime/control-plane docs/ADR 0016: only if LAS-11 changes Harnex durable behavior.
- both `.engineering/e2e.json`: lifecycle journey/environment/fidelity truth; remove stale automation-gap wording when automation is demonstrably present.
- `docs/current-state.md`: integrated/candidate blocker truth.
- tests/contracts: executable failure identity, no-throw observability, mapping and lifecycle truth.

## Validation and evidence

Overall final depth remains `STRONG` because LAS crosses Consumer/Binder contracts, Android service/lifecycle behavior, runtime/model residency, product execution ownership and recovery semantics. Individual slices use the narrowest selector-approved depth:

- LAS-09: expected SCOPED unless shared diagnostics contract blast radius escalates;
- LAS-10: existing Two-APK `REMOTE_AUTOMATED` integration evidence;
- LAS-11: selector-driven, STRONG if Harnex/Binder/model/runtime/shared contract is touched;
- LAS-12/13/14: SCOPED or STRONG according to affected lifecycle/shared state owners;
- LAS-08C/07: STRONG exact-head integrated automation plus separate `REAL_ENVIRONMENT` evidence where required.

Deterministic compile/unit/static/lint/Manifest/Binder/emulator/package gates are `AGENT_LOCAL` only with equivalent tooling; otherwise they are `REMOTE_AUTOMATED`, never delegated to the user.

## Completion

The workstream is complete only when code/contracts/consumers/tests/docs/UX/evidence agree and exact-head evidence proves:

- passive setup inspection is side-effect free;
- typed setup failures survive transport, diagnostics and product mapping without accidental reclassification;
- user-facing state/recovery distinguishes real incompatibility from configuration/model/runtime/transport blockers;
- Analyze uses a fresh fail-closed preflight and records matching execution identity;
- ordinary app-switch/UI detach does not cancel valid active analysis or unload protected runtime resources;
- return/rebind observes the same job without duplicate inference;
- process/critical-pressure loss is structured and recoverable without pretending native RAM survived;
- sensitive process-local state is not silently persisted for recovery;
- required deterministic gates pass on both exact heads at the selected profiles;
- required ARM64/JNI/GGUF/model-memory/OEM claims are recorded separately as `REAL_ENVIRONMENT` evidence.

After durable truth is transferred, update `docs/current-state.md` and delete this coordination file by default.