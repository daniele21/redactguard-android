# Local AI Setup & Readiness (LAS)

Status: active
Owner: RedactGuard + Android Local LLM Harness
Read when: coordinating setup/readiness, execution identity, background execution, model residency, or Local AI recovery

## Goal

Make Local AI setup explicit before analysis and keep a valid user-initiated analysis alive across ordinary app switching/UI detachment.

Readiness:

`Connected -> Configured -> Compatible -> Runtime Ready -> Ready to Analyze`

Execution:

`Ready -> Analysis Job Active -> UI/observer attached or detached -> Completed | Cancelled | Interrupted`

User-visible setup:

`RedactGuard -> use case -> preset -> resolved model -> effective generation configuration -> Harness runtime`

The setup shown before analysis must match the privacy-safe immutable execution snapshot recorded for that run.

## Non-goals and invariants

- Harness owns model/configuration, activation, preparation, inference, model residency and native/runtime cleanup.
- RedactGuard owns document ingestion, analysis/chunk orchestration, validation, review/export and product job state.
- Setup inspection is read-only: opening Local AI/Settings never activates, prepares, loads or retains a model.
- RedactGuard never guesses Harness-owned model/configuration state or exposes paths, digests, prompts, outputs or raw Host telemetry.
- Document text, pasted text, prompts, findings and raw outputs are not durably persisted merely for recovery without an explicit privacy/security ADR.
- Binder/UI observer attachment is not execution ownership. Ordinary app-switch, Activity recreation or temporary observer detach must not implicitly cancel valid work.
- Active execution protects the model/runtime it needs; ordinary `UI_HIDDEN`/`BACKGROUND` must not release resources protected by an active execution/lease.
- Idle models may still unload through bounded warm-retention/resource policy.
- Real Harness process death, force-stop, reboot or critical pressure cannot preserve native model/session/decode RAM. Report interruption truthfully and recover only from a documented safe boundary.
- Real RedactGuard process death does not imply transparent recovery of process-local sensitive state; source-class-aware recovery must respect the privacy boundary.
- Existing Consumer/Binder compatibility remains additive/versioned.

## Lifecycle contract

| Event | Target behavior |
| --- | --- |
| Home/app switch | Same analysis continues; active model remains protected; UI reattaches to the same job on return |
| Activity recreation | Same job is observed; no duplicate inference |
| Temporary Binder detach/rebind | Durable Harness job continues and can be reobserved/cancelled by the same authorized consumer |
| RedactGuard process death | Harness-owned active inference may finish; product recovery is source/privacy aware and never invents lost sensitive state |
| Harness process death | Native state is lost; affected job becomes structured `Interrupted/ProcessLost`; rediscover/reload/retry only from a safe boundary |
| Critical memory pressure | System-health policy may cancel/release; surface a structured interruption reason and valid recovery |
| Explicit cancel | Exact job becomes `Cancelled`; resources clean up; idle/warm policy resumes |
| Idle/no job | Model may unload normally |

## Why the current implementation fails this contract

Checkpoint: RedactGuard PR `#143` at `f56be55a0a6a5b517df8cd696231490a8e6e94f9`; Harness `dev` at `d198bbaf08b58f92c206206e49b1276d7f04ce03`.

- Harness `HarnessSharedRuntimeService` is explicitly bound-only. Current connection cleanup owns client request/session resources, so connection death can cancel/close work.
- Harness `RuntimeMemoryPolicy` unloads a loaded **idle** model on ordinary `UI_HIDDEN`/`BACKGROUND`; `LOW_MEMORY` may cancel/release active work.
- Existing warm-idle/resolved warm-retention improves idle reuse but is not a durable execution owner.
- RedactGuard keeps `AnalysisOperationId`, chunk cursor/results, activation/session and callbacks in process-local `ViewModel`/analyzer/runtime state. `onCleared()` cancels active analysis and closes the Consumer runtime.
- `MainActivity` does not cancel from `onPause`/`onStop`; therefore a normal app switch is not itself a `viewModelScope` cancellation. Device evidence must distinguish process reclamation, Binder loss, Host teardown and critical pressure.

## Work graph

| ID | Work | Owns/writes | Depends on | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| LAS-00 | Freeze setup/readiness/background UX contract | RedactGuard design/workstream/boundary docs | — | yes | ACTIVE |
| LAS-01 | Add consumer-safe execution-setup introspection | Harness contracts/Binder/Host/tests/docs | LAS-00 | yes | BLOCKED |
| LAS-02 | Add `LocalAiSetupState` projection | RedactGuard Local AI domain/infrastructure/tests | LAS-00; LAS-01 for model/config | yes | BLOCKED |
| LAS-03 | Side-effect-free inspection + fresh fail-closed Analyze preflight | RedactGuard control-plane/runtime/tests | LAS-02 | yes | BLOCKED |
| LAS-04 | Add `Analizza / AI locale / Impostazioni` navigation | RedactGuard Compose/navigation/tests | LAS-00 | yes | BLOCKED |
| LAS-05 | Build Local AI setup/readiness/recovery UX | RedactGuard UI/accessibility/visual evidence | LAS-02/03/04 | no | BLOCKED |
| LAS-06 | Add immutable privacy-safe `AnalysisSetupSnapshot` | RedactGuard orchestration/diagnostics/tests | LAS-03 | yes | BLOCKED |
| LAS-08A | Decouple Harness job lifetime from Binder/UI lifetime; protect active residency | Harness runtime-core, Service/Host, Consumer/Binder job API, Manifest/tests/docs | LAS-01 | no | BLOCKED |
| LAS-08B | Move active RedactGuard analysis ownership out of Activity/ViewModel lifetime; add reattach/recovery | RedactGuard execution owner/product state/tests/docs | LAS-03/06/08A | no | BLOCKED |
| LAS-08C | Add lifecycle fault-injection/two-APK evidence | both repos E2E/scripts/workflows | LAS-08A/08B | no | BLOCKED |
| LAS-07 | Final exact-head automated + physical evidence | both repos | LAS-01..06, LAS-08A..08C | no | BLOCKED |

LAS-01 and LAS-04 may run in parallel. LAS-08A follows LAS-01 because both change Consumer/Binder contracts. LAS-08B consumes the canonical preflight/snapshot so background execution cannot drift from the setup shown to the user.

## LAS-08 acceptance

### LAS-08A — Harness

- Stable consumer-safe execution job identity with start/status-observe/cancel/terminal semantics.
- Callback/Binder observer death no longer implicitly cancels a valid durable active job.
- Active jobs protect required model/runtime resources from ordinary background/UI-hidden release.
- Preserve the current one-resident-model/one-production-decode default; competing jobs queue/serialize deterministically.
- Critical pressure can still protect system health with structured terminal reason.
- Harness process death becomes `Interrupted/ProcessLost`, never fake-running state.
- Terminal metadata is bounded/privacy-safe.
- Choose and document an Android-compliant started/foreground execution mechanism. Foreground-service type, permissions, notification and Play declaration must match the real use case; do not misuse `dataSync`/`mediaProcessing`. Evaluate `specialUse` only if current Android/Play policy supports and justifies it.

### LAS-08B — RedactGuard

- `RedactGuardProductViewModel` becomes observer/controller, not owner of running multi-chunk analysis lifetime.
- One stable `AnalysisJobId`; app-switch/recreation returns to the same job and never duplicates generation.
- Distinct outcomes for explicit cancel, setup invalidation, Harness process loss, RedactGuard process loss and critical pressure.
- No new durable sensitive payload persistence without privacy/security review.
- Pasted text is not claimed transparently resumable after RedactGuard process death under the current process-local invariant.
- Choose one coordinated background/foreground ownership design; avoid accidental duplicate notifications across packages.

### LAS-08C — Evidence

Automate at minimum:
1. analysis -> Home/app switch -> work progresses -> return -> same job/result;
2. Activity recreation -> same job;
3. Binder disconnect/rebind -> no implicit durable-job cancellation;
4. explicit cancel -> exact terminal state + cleanup;
5. Harness process-loss injection -> `Interrupted/ProcessLost` + safe recovery;
6. RedactGuard process-loss injection -> privacy/source-aware recovery;
7. critical-pressure injection -> structured interruption;
8. multiple jobs/consumers -> deterministic queue/serialization.

Two-APK emulator proves Android/Binder/job semantics. Physical ARM64/JNI/GGUF evidence separately owns real model residency, memory reclamation, thermal and OEM lifecycle claims.

## Validation and documentation

Overall depth: `STRONG` because this crosses public Consumer/Binder contracts, Android Service/Manifest behavior, runtime/model residency and product execution ownership.

- LAS-00: LEAN docs/design guards.
- LAS-01/08A: STRONG Harness contract/Binder/runtime/Service gates.
- LAS-02/03/06/08B: STRONG RedactGuard contract/lifecycle/privacy gates.
- LAS-04: SCOPED UI/navigation unless blast radius expands.
- LAS-05/08C/07: STRONG integrated/two-APK evidence plus separate REAL_ENVIRONMENT where required.

Deterministic compile/unit/static/lint/Manifest/Binder/emulator gates are AGENT_LOCAL when equivalent tooling exists, otherwise REMOTE_AUTOMATED. They are not delegated to the user merely because the agent lacks Android tooling.

Durable destinations:
- `design/ux-contract.json`: readiness, background continuity, interruption/recovery UX.
- Harness shared-runtime/control-plane docs + ADR: job lifetime, residency lease, Service/foreground policy, process loss.
- RedactGuard Local AI/analysis docs: setup/preflight/snapshot, `AnalysisJobId`, reattachment and source-aware recovery.
- both `.engineering/e2e.json`: lifecycle journeys and emulator-vs-physical evidence gaps.
- `docs/current-state.md`: active/final delivered state.

## Completion

Complete only when code/contracts/consumers/tests/docs/UX/evidence agree and exact-head validation proves:

- setup is inspected/readied without passive activation/loading;
- Analyze uses fresh fail-closed setup and records matching execution identity;
- ordinary app-switch/UI detach does not cancel valid active analysis or unload protected model resources;
- return/rebind observes the same job without duplicate inference;
- Binder connection lifetime is not the implicit owner of durable Harness work;
- process/critical-pressure loss is structured and recoverable without pretending native RAM survived;
- sensitive process-local state is not silently persisted for recovery;
- STRONG deterministic gates pass on both exact heads, including two-APK lifecycle evidence;
- required ARM64/JNI/GGUF lifecycle/model-memory behavior is recorded separately as REAL_ENVIRONMENT evidence.

After handoff, update durable current-state/feature docs and delete this coordination file by default.