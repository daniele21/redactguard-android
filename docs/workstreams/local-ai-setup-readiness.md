# Local AI Setup & Readiness (LAS)

Status: active
Owner: RedactGuard + Android Local LLM Harness
Read when: implementing or coordinating consumer-visible Local AI setup inspection, preflight readiness, execution identity, background execution, runtime/model residency, or setup-aware diagnostics

## Goal

Make Local AI readiness explicit and verifiable before document analysis, so RedactGuard can show the consumer-safe setup that Harness will execute and can fail closed when that setup is incomplete, stale, incompatible, or not runtime-ready.

A user-initiated analysis must also survive ordinary Android UI/background transitions. Switching to another app, hiding either UI, recreating an Activity, or temporarily detaching an observer must not by itself cancel a valid active analysis or voluntarily unload the model required by that analysis.

Target readiness progression:

`Connected -> Configured -> Compatible -> Runtime Ready -> Ready to Analyze`

Target execution progression:

`Ready to Analyze -> Analysis Job Active -> Background/Foreground Observers Attached or Detached -> Completed | Cancelled | Interrupted`

The user-facing setup chain is:

`RedactGuard -> use case -> preset -> resolved model -> effective generation configuration -> Harness runtime`

The setup shown before analysis must match the immutable setup identity recorded for the run.

## Non-goals

- Do not move model installation, model selection, preset editing, runtime tuning, residency, or inference ownership from Harness into RedactGuard.
- Do not expose model paths, file digests, prompts, generated content, raw Host telemetry, Binder internals, or mutable Host administration state to product UI.
- Do not activate, prepare, load, or retain a model merely because the user opens the Local AI or Settings surface.
- Do not keep a model resident forever merely because either app has been opened. Residency must be justified by active work, an explicit activation/lease, bounded warm retention, or an explicit product policy.
- Do not claim that Android process death, force-stop, reboot, or critical memory pressure can preserve native model memory or an in-flight llama.cpp decode. Those events require explicit interruption/recovery semantics.
- Do not persist document text, prompts, raw model output, pasted text, or review findings merely to make process-death recovery transparent. Any durable sensitive-state design requires an explicit privacy/security decision and ADR; process-local sensitive state remains the default.
- Do not attribute `INVALID_FINDINGS` or another product failure to setup/configuration without evidence; setup identity is correlation evidence, not a root-cause assumption.
- Do not replace the existing HCP physical validation slice; `RG-HCP-8` remains separate REAL_ENVIRONMENT evidence for the established Consumer control plane.

## Invariants

- Harness remains the source of truth for application/use-case publication, preset publication, binding, resolved model identity, effective generation configuration, activation, runtime preparation, model residency, inference jobs, and native/runtime cleanup.
- RedactGuard remains the source of truth for document ingestion, privacy policy, analysis/chunk orchestration, finding validation, review/export, and product-level analysis job state.
- RedactGuard consumes only a consumer-safe, read-only published projection of execution setup; it never reconstructs or guesses Harness-owned model/configuration state.
- Sensitive document text, prompts, findings, and generated output stay outside setup telemetry and setup snapshots.
- Setup inspection is side-effect free with respect to activation/runtime preparation/model loading.
- `Analyze` revalidates the relevant revisions and setup immediately before document content can enter inference; stale or incompatible setup fails closed.
- A run records an immutable privacy-safe `AnalysisSetupSnapshot` sufficient to correlate diagnostics with the setup actually used.
- The existing Consumer SDK contract remains backwards compatible; any Harness extension is additive and feature-gated/protocol-versioned as required by the repository contract.
- Consumer-visible model/configuration information is deliberately narrower than Harness internal execution state. This workstream supersedes the older blanket visibility rule that consumer surfaces receive no model/configuration identity, but does not transfer ownership or expose paths/digests/runtime internals.
- Activity/ViewModel visibility and Binder observer attachment are not execution-lifetime ownership signals. Ordinary observer/UI detachment must not cancel a durable active job.
- Active user-initiated execution holds the runtime/model residency it needs. `UI_HIDDEN` or ordinary `BACKGROUND` alone must not release resources protected by an active execution/residency lease.
- Critical low-memory pressure may still cancel/release active work when required for system health, but the resulting terminal state must be structured, truthful, observable, and recoverable at a documented safe boundary.
- A real Harness process death necessarily loses in-memory native model/session/decode state. The system must report the affected job as interrupted/process-lost rather than pretending it is still running.
- A real RedactGuard process death must not silently reconstruct or persist sensitive document state. Recovery depends on the source class and approved privacy policy: re-readable SAF-backed sources may be reconstructed from a privacy-safe source reference if explicitly designed; pasted text cannot be transparently resumed under the current process-local invariant.

## Lifecycle behavior contract

| Event | Harness target behavior | RedactGuard target behavior |
| --- | --- | --- |
| User presses Home or switches app while analysis is active | Keep the active inference job and required model residency; UI visibility is irrelevant to job ownership | Keep the same analysis job active; UI becomes an observer and reattaches on return |
| Activity recreation/configuration change | No runtime/session cancellation caused by UI recreation | Reobserve the same analysis job; never start a duplicate analysis |
| Temporary Binder observer disconnect/rebind | Durable job continues; same authorized consumer can query/reobserve/cancel by stable job identity | Reconnect and reobserve the same job identity; surface temporary connectivity separately from job terminal state |
| RedactGuard process death | A Harness-owned active inference job may finish independently of the dead observer; bounded terminal metadata remains queryable by the authorized owner | Process-local document/orchestration state is lost unless explicitly rehydratable; show `Interrupted`/recovery and resume only from an approved safe source boundary |
| Harness process death | Native model/session/decode state is lost; affected jobs become structured `Interrupted/ProcessLost` | Reconnect, rediscover setup and retry only from a documented safe product boundary; never assume the old session survived |
| `LOW_MEMORY` / critical system pressure | Policy may cancel and release active runtime; record exact reason before/while cleaning up where possible | Surface structured interruption and a valid retry/recovery path; do not map it to a generic connection error |
| Explicit user cancel | Cancel the exact job, release session/request resources, then apply normal idle/warm-retention policy | Move the same analysis job to terminal `Cancelled`; no implicit retry |
| Job completion | Retain only bounded consumer-safe terminal metadata; model residency follows resolved bounded warm-retention policy | Validate/merge results and expose review state; no duplicate generation on UI return |
| Idle app with no job/lease | Model may be unloaded according to memory/warm-retention policy | No background execution claim |
| Force-stop/reboot | No survival guarantee; in-memory runtime is lost | No transparent continuation guarantee; recover truthfully on next launch |

## Work graph

| ID | Work | Owns/writes | Depends on | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| LAS-00 | Freeze product/UX/contract semantics for setup inspection, readiness and lifecycle continuity | RedactGuard `design/ux-contract.json`, this workstream, durable boundary docs | — | yes | ACTIVE |
| LAS-01 | Add consumer-safe read-only execution setup introspection to Harness | Harness `core/contracts`, Binder contract/client, Android service Host, Consumer fixture/tests/docs | LAS-00 | yes | BLOCKED |
| LAS-02 | Add RedactGuard setup projection using existing Consumer state plus LAS-01 when available | RedactGuard local-AI infrastructure/domain/UI models/tests | LAS-00; LAS-01 only for model/config fields | yes | BLOCKED |
| LAS-03 | Add side-effect-free setup inspection and fail-closed analysis preflight/revalidation | RedactGuard control-plane runtime/coordinator/tests | LAS-02 | yes | BLOCKED |
| LAS-04 | Introduce top-level `Analizza / AI locale / Impostazioni` information architecture | RedactGuard Compose navigation, state restoration, adaptive navigation, UI tests | LAS-00 | yes | BLOCKED |
| LAS-05 | Build Local AI readiness/setup surface and pre-analysis summary/recovery | RedactGuard Compose UI + accessibility/visual evidence | LAS-02, LAS-03, LAS-04 | no | BLOCKED |
| LAS-06 | Record privacy-safe immutable `AnalysisSetupSnapshot` and attach it to diagnostics | RedactGuard analysis orchestration/diagnostic contracts/tests/docs | LAS-03 | yes | BLOCKED |
| LAS-08A | Decouple Harness execution lifetime from Binder/UI lifetime and protect active model residency | Harness runtime-core, shared-runtime Host/Service, Consumer/Binder job contract, Manifest/notification policy, direct consumers/tests/docs | LAS-01 | no | BLOCKED |
| LAS-08B | Move RedactGuard active analysis ownership out of Activity/ViewModel lifetime and add reattach/recovery semantics | RedactGuard analysis execution owner, Local AI adapter, product state/recovery, notification/background-execution integration, tests/docs | LAS-03, LAS-06, LAS-08A | no | BLOCKED |
| LAS-08C | Add lifecycle fault-injection and two-APK background/process-recovery evidence | both repos `.engineering/e2e.json`, instrumentation/scripts/workflows/evidence | LAS-08A, LAS-08B | no | BLOCKED |
| LAS-07 | Cross-repo exact-head validation and representative-device evidence | both repos CI/evidence + physical ARM64 journey | LAS-01..LAS-06, LAS-08A..LAS-08C | no | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

Parallel work must have explicit non-conflicting ownership/write boundaries or a defined integration point. After LAS-00, LAS-01 and LAS-04 may proceed in parallel; LAS-02 may begin with existing Consumer use-case/preset/readiness state but must not invent model/config fields while LAS-01 is pending. LAS-08A intentionally follows LAS-01 because both modify the public Consumer/Binder contract and Host integration; they must not be implemented as parallel competing protocol changes. LAS-08B follows the canonical preflight/snapshot contract so a background job cannot execute against setup different from the one shown and recorded.

## Current executable slice

`LAS-00`

Acceptance:

- `design/ux-contract.json` defines Local AI setup/readiness as a first-class user outcome and documents the three top-level destinations.
- The contract distinguishes connection, configuration, compatibility, runtime readiness, and final analysis readiness.
- The contract states that Local AI inspection is read-only and non-activating/non-preparing.
- The contract states that `Analyze` performs a fresh fail-closed preflight before document content enters inference.
- The contract limits technical details through progressive disclosure and preserves Harness ownership.
- The contract defines ordinary app-switch/background as observer detachment, not implicit analysis cancellation.
- The contract distinguishes normal background continuity from true process-loss/critical-pressure recovery and never promises preservation of native RAM state across process death.
- Durable HCP/current-state wording no longer contradicts the new consumer-safe execution projection.

Validation:

- repository documentation/JSON guards selected by `.engineering/commands.json` for the resulting diff.
- review the complete LAS-00 diff against `AGENTS.md`, `skills/plan-workstream/SKILL.md`, and `skills/design-product-experience/SKILL.md`.

## Integration points

- LAS-01 publishes one additive Consumer SDK abstraction for the immutable effective execution setup; RedactGuard consumes that abstraction rather than Binder/Host internals.
- Harness maps the consumer projection from the same canonical resolved execution owner used by activation/session creation so displayed setup cannot drift from executed setup.
- LAS-02 keeps model/configuration fields explicitly unavailable/pending on older Hosts rather than fabricating defaults.
- LAS-03 shares one canonical readiness/preflight result with LAS-05, LAS-06 and LAS-08B so UI eligibility, execution gating, run evidence and background execution do not diverge.
- LAS-04 changes information architecture only; it must not own or duplicate setup resolution logic.
- LAS-06 snapshot identity is captured from the successful preflight/execution contract, not re-derived after generation.
- LAS-08A introduces a stable consumer-safe execution job identity with explicit start/status-observe/cancel/terminal semantics. Binder connection/death remains an authorization/transport concern but no longer acts as an implicit cancellation signal for a durable active job.
- LAS-08A keeps the existing one-resident-model/one-production-decode default unless a separate approved resource policy changes it. Multiple consumers/jobs therefore queue or serialize explicitly instead of creating uncontrolled parallel native workloads.
- LAS-08A extends model residency with explicit active-job/activation protection and bounded warm retention. Ordinary `UI_HIDDEN`/`BACKGROUND` may unload only genuinely unprotected idle resources.
- LAS-08A must select an Android-compliant started/foreground execution mechanism for user-initiated long inference. The valid foreground-service type, permissions, notification behavior, Play declaration and Android-version restrictions are an explicit design/ADR decision; do not label local inference `dataSync`/`mediaProcessing` merely to obtain a foreground lifetime. If no standard type matches, evaluate and justify `specialUse` against current Android/Play requirements before implementation.
- LAS-08B introduces a stable RedactGuard `AnalysisJobId` whose owner is application/execution scoped rather than Activity/ViewModel scoped. Compose/ViewModel observes that job and may detach/reattach without cancelling it.
- LAS-08B must avoid two uncoordinated foreground-execution owners/notifications. During implementation choose and document the minimum architecture that keeps both product orchestration and Harness inference alive during ordinary backgrounding; if both packages require foreground execution, their responsibilities and user-visible notifications must be intentional rather than accidental.
- LAS-08B does not silently persist sensitive analysis payloads. Any process-death continuation beyond currently in-memory work is source-class aware and requires an explicit approved rehydration/privacy design.
- WorkManager/JobScheduler may coordinate deferred recovery/retry where appropriate, but they are not a substitute for the owner of immediate user-initiated native LLM execution. Android-version quotas and stop reasons remain part of the recovery contract.

## LAS-08 acceptance details

### LAS-08A — Harness durable execution and residency

Acceptance:

- A consumer can start an authorized execution and receive a stable consumer-safe job identity that is independent of callback/Binder observer attachment.
- The same authorized application can query/observe/cancel that job after a temporary transport rebind without creating a duplicate native request.
- Client callback/Binder death does not automatically cancel an otherwise valid durable active job; ownership expiry/cancellation is governed by explicit job policy.
- An active job protects the exact model/runtime resources it requires from ordinary `UI_HIDDEN`/`BACKGROUND` release.
- Critical memory pressure remains able to protect system health and produces a structured terminal reason.
- Harness process death produces truthful `Interrupted/ProcessLost` semantics; no contract claims to resume an in-flight native decode from RAM that no longer exists.
- Completed/cancelled/interrupted terminal metadata is bounded and privacy-safe.
- The Android service lifetime/foreground type/notification/stop behavior is documented against current platform and Play requirements.

### LAS-08B — RedactGuard durable product analysis

Acceptance:

- `RedactGuardProductViewModel` is no longer the lifetime owner of a running multi-chunk analysis.
- Pressing Home or switching apps while an analysis is active does not call product cancellation and does not cause a second analysis on return.
- Returning to RedactGuard reattaches to the same `AnalysisJobId`, restores progress/terminal state, and continues to enforce no-partial-findings product semantics.
- Explicit cancel, setup invalidation, Harness process loss, RedactGuard process loss and critical memory pressure have distinct terminal/recovery states.
- The analysis job retains only the minimum process-local sensitive state required while executing; no new durable sensitive persistence is introduced without an ADR/security review.
- Process death recovery is source-aware: transparent continuation is not claimed for pasted text under the current process-local privacy invariant.

### LAS-08C — lifecycle evidence

Acceptance journeys include at minimum:

1. Start analysis -> press Home/switch app -> wait while work progresses -> return -> same job/result, no duplicate inference.
2. Activity recreation/configuration change -> same job remains active/terminal and is reobserved.
3. Temporary Binder disconnect/rebind -> durable Harness job is not implicitly cancelled and the consumer reobserves the same job.
4. Explicit cancel -> exact job becomes terminal `Cancelled`, request/session resources are cleaned up, and model residency follows the documented idle/warm policy.
5. Inject Harness process loss -> loaded native state is considered lost, job becomes structured `Interrupted/ProcessLost`, and RedactGuard follows the documented safe retry boundary.
6. Inject RedactGuard process loss -> no sensitive state is silently reconstructed; recovery behavior matches the source class and privacy contract.
7. Inject critical memory pressure -> structured reason and cleanup are observable; no generic fake-disconnected status substitutes for the pressure event.
8. Multiple consumers/jobs -> one-resident-model/one-production-decode policy remains deterministic through queue/serialization/cancellation.
9. Two-APK emulator proves Android/Binder/job-lifecycle semantics without claiming ARM64/native fidelity.
10. Physical ARM64/JNI/GGUF evidence confirms real model residency, app-switch continuity, memory reclamation and OEM/device behavior separately.

## Validation strategy

Blast radius is cross-repo and includes public Consumer/Binder contracts, privacy-safe diagnostics, lifecycle/readiness, Android Service/Manifest behavior, runtime/model residency, product execution ownership and navigation. Overall workstream validation depth is `STRONG`.

- `LAS-00`: LEAN documentation/design guards.
- `LAS-01`: STRONG Harness contract/Binder/consumer-fixture gates; repository-owned remote automation if equivalent Android tooling is unavailable locally.
- `LAS-02`/`LAS-03`/`LAS-06`: STRONG RedactGuard unit/contract/static/build gates because setup identity changes analysis eligibility/evidence.
- `LAS-04`: SCOPED product-UI/adaptive-navigation/accessibility tests, escalating if shared state/navigation contracts broaden blast radius.
- `LAS-05`: STRONG integrated product-flow + visual evidence because it consumes readiness and recovery state.
- `LAS-08A`: STRONG Harness runtime/Binder/Service/Manifest/resource-lifecycle gates, including exact-head consumer compatibility and fault-injection tests.
- `LAS-08B`: STRONG RedactGuard execution/lifecycle/privacy tests because product job ownership leaves the Activity/ViewModel boundary and may affect process/background behavior.
- `LAS-08C`: STRONG deterministic two-APK Android lifecycle automation plus separate `REAL_ENVIRONMENT` ARM64/JNI/GGUF/model-memory/OEM evidence.
- `LAS-07`: deterministic two-APK/API35 evidence on exact heads plus separate `REAL_ENVIRONMENT` ARM64/JNI/GGUF evidence. Emulator/JVM evidence must not be relabeled physical-device evidence.

Deterministic compile/unit/static/lint/Manifest/Binder/emulator gates are `AGENT_LOCAL` when an equivalent environment exists, otherwise `REMOTE_AUTOMATED`; they are not delegated to the user solely because the agent lacks Android tooling. Real model residency, production ARM64 JNI/llama.cpp behavior, physical memory reclamation, thermal behavior and OEM lifecycle fidelity remain `REAL_ENVIRONMENT` where required.

No deterministic Gradle/Lint/R8/build gate is delegated to the user solely because the agent lacks Android tooling; use repository-owned remote automation and inspect exact-head evidence.

## Durable documentation destinations

- `design/ux-contract.json`: product task model, IA, readiness hierarchy, progressive disclosure, background continuity, interruption states and recovery.
- `docs/current-state.md`: active LAS boundary while implementation is in progress; final delivered behavior after handoff.
- `docs/workstreams/harness-control-plane-consumer-cutover.md`: narrow boundary clarification so HCP no longer forbids the consumer-safe execution projection introduced by LAS while keeping RG-HCP-8 unchanged.
- Harness shared-runtime/control-plane docs: canonical Consumer execution-setup projection, durable job/observer lifecycle, model-residency lease and compatibility behavior introduced by LAS-01/LAS-08A.
- Harness ADR/design contract: Android started/foreground execution ownership, foreground-service type/notification/Play-policy decision, process-loss semantics, job retention and low-memory behavior.
- RedactGuard local-AI/analysis feature/runtime docs: final setup/preflight/snapshot behavior plus `AnalysisJobId`, observer detachment, background execution, process-loss and source-aware recovery introduced by LAS-02/03/05/06/08B.
- both repositories `.engineering/e2e.json`: lifecycle journeys and residual emulator-vs-physical fidelity gaps introduced by LAS-08C.
- tests/contracts: executable truth for non-activating inspection, stale revision rejection, backwards compatibility, privacy-safe projection, snapshot/execution identity agreement, app-switch continuity, no-duplicate reattach, process-loss and pressure cleanup.

## Initial source-of-truth checkpoint

LAS was started from:

- RedactGuard `dev` at `6627d082ed1f1b8b63b1759662a1b05b0138e5e2`.
- Android Local LLM Harness `dev` at `4dc9d98bb11c21ae32e14ebf32c2e53067fbee98`.

At that checkpoint the Consumer SDK exposes assigned use cases, published presets, activation identity and consumer-safe runtime readiness, while Harness internally owns an exact `ResolvedHostExecution` with model profile and generation overrides. No public Consumer setup projection yet exposes that resolved model/effective generation configuration. LAS-01 therefore represents a real additive contract gap rather than duplicated functionality.

## Background-execution source checkpoint

The lifecycle gap was added after inspecting RedactGuard PR `#143` at `f56be55a0a6a5b517df8cd696231490a8e6e94f9` and Harness `dev` at `d198bbaf08b58f92c206206e49b1276d7f04ce03`.

Observed current behavior/ownership at that checkpoint:

- Harness `HarnessSharedRuntimeService` is explicitly a bound-only proof Host. Its `onBind`/`onUnbind` feed warm-idle demand, and the Host workstream states V1 remains bound-only rather than a started foreground execution owner.
- Harness connection cleanup currently owns client requests/sessions and cancels/closes them when a connection dies. This couples active work to the consumer connection lifetime.
- Harness `RuntimeMemoryPolicy` maps ordinary `UI_HIDDEN`/`BACKGROUND` pressure to `UNLOAD_IDLE_MODEL` when the model is loaded and there are no active sessions/generations/queued generations. Critical `LOW_MEMORY` may cancel/release active work.
- Harness already has bounded warm-idle/resolved warm-retention coordination. That improves reuse of an idle model but is not a durable execution owner and does not by itself make a request survive consumer detachment/process death.
- RedactGuard `RedactGuardProductViewModel`, `SequentialDocumentAnalyzer`, `ControlPlaneAnalysisRuntime` and `ConsumerAnalysisRuntime` currently keep the analysis operation, chunk cursor, raw findings, activation/session and callbacks in process-local memory. `onCleared()` cancels the active analysis and closes the Consumer runtime.
- RedactGuard `MainActivity` does not explicitly cancel analysis from `onPause`/`onStop`; therefore a normal app switch is not semantically equivalent to ViewModel cancellation. If the observed device run stops on app-switch, exact device evidence must distinguish process reclamation, Binder connection loss, Host/service teardown and critical memory pressure instead of attributing the stop to `viewModelScope` alone.
- The current two-APK E2E contracts cover reconnect/Host restart but do not yet establish active-job continuity across app-switch/observer detachment or the full process-loss matrix. LAS-08C closes that evidence gap.

## Completion

The workstream is complete only when applicable code, Consumer/Binder contracts, direct consumers, failure/resource behavior, product UX, accessibility, lifecycle/background behavior, validation/evidence and durable docs agree.

Completion additionally requires:

- RedactGuard no longer treats Binder connectivity as final Local AI readiness.
- Before Analyze, the UI can show at least use case, preset, resolved model and effective generation configuration when supported by the connected Host.
- Older/incompatible Hosts degrade explicitly and safely rather than guessing setup.
- `Analyze` is fail-closed for incomplete, stale or incompatible setup and does not send document content before preflight succeeds.
- Opening Local AI/Settings causes no model activation/preparation/loading.
- The setup shown immediately before analysis agrees with the immutable run snapshot/execution identity.
- Diagnostics such as `INVALID_FINDINGS` can be correlated privacy-safely with the setup actually used without claiming unsupported causality.
- An ordinary app-switch, UI hiding, Activity recreation or temporary observer detach does not implicitly cancel a valid active analysis or release model resources protected by that analysis.
- Returning to RedactGuard observes the same analysis job and never silently starts a duplicate generation.
- Harness Binder/client connection lifetime is no longer the implicit owner of a durable active inference job.
- Idle model unloading remains bounded and resource-aware, while active-job residency is explicit and low-memory cancellation is structured.
- Harness process death is reported as interruption/process loss and never as continued native execution; RedactGuard recovery restarts only from an approved safe boundary.
- RedactGuard process-death recovery respects the process-local sensitive-state invariant and does not silently persist pasted/document content.
- Exact-head deterministic STRONG gates pass in both repositories, including Consumer/Binder compatibility, background lifecycle fault-injection and two-APK evidence.
- Representative ARM64/JNI/GGUF app-switch/model-residency/process-loss behavior is recorded separately as `REAL_ENVIRONMENT` evidence where required.

Then update durable documentation/current state and delete this coordination file by default once handoff is complete.