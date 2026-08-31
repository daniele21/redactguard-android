# Local AI Setup & Readiness (LAS)

Status: active
Owner: RedactGuard + Android Local LLM Harness
Read when: implementing or coordinating consumer-visible Local AI setup inspection, preflight readiness, execution identity, or setup-aware diagnostics

## Goal

Make Local AI readiness explicit and verifiable before document analysis, so RedactGuard can show the consumer-safe setup that Harness will execute and can fail closed when that setup is incomplete, stale, incompatible, or not runtime-ready.

Target progression:

`Connected -> Configured -> Compatible -> Runtime Ready -> Ready to Analyze`

The user-facing setup chain is:

`RedactGuard -> use case -> preset -> resolved model -> effective generation configuration -> Harness runtime`

The setup shown before analysis must match the immutable setup identity recorded for the run.

## Non-goals

- Do not move model installation, model selection, preset editing, runtime tuning, residency, or lifecycle ownership from Harness into RedactGuard.
- Do not expose model paths, file digests, prompts, generated content, raw Host telemetry, Binder internals, or mutable Host administration state to product UI.
- Do not activate, prepare, load, or retain a model merely because the user opens the Local AI or Settings surface.
- Do not attribute `INVALID_FINDINGS` or another product failure to setup/configuration without evidence; setup identity is correlation evidence, not a root-cause assumption.
- Do not replace the existing HCP physical validation slice; `RG-HCP-8` remains separate REAL_ENVIRONMENT evidence for the established Consumer control plane.

## Invariants

- Harness remains the source of truth for application/use-case publication, preset publication, binding, resolved model identity, effective generation configuration, activation, runtime preparation, residency, and inference.
- RedactGuard consumes only a consumer-safe, read-only published projection of execution setup; it never reconstructs or guesses Harness-owned model/configuration state.
- Sensitive document text, prompts, findings, and generated output stay outside setup telemetry and setup snapshots.
- Setup inspection is side-effect free with respect to activation/runtime preparation/model loading.
- `Analyze` revalidates the relevant revisions and setup immediately before document content can enter inference; stale or incompatible setup fails closed.
- A run records an immutable privacy-safe `AnalysisSetupSnapshot` sufficient to correlate diagnostics with the setup actually used.
- The existing Consumer SDK contract remains backwards compatible; any Harness extension is additive and feature-gated/protocol-versioned as required by the repository contract.
- Consumer-visible model/configuration information is deliberately narrower than Harness internal execution state. This workstream supersedes the older blanket visibility rule that consumer surfaces receive no model/configuration identity, but does not transfer ownership or expose paths/digests/runtime internals.

## Work graph

| ID | Work | Owns/writes | Depends on | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| LAS-00 | Freeze product/UX/contract semantics for setup inspection and readiness | RedactGuard `design/ux-contract.json`, this workstream, durable boundary docs | — | yes | ACTIVE |
| LAS-01 | Add consumer-safe read-only execution setup introspection to Harness | Harness `core/contracts`, Binder contract/client, Android service Host, Consumer fixture/tests/docs | LAS-00 | yes | BLOCKED |
| LAS-02 | Add RedactGuard setup projection using existing Consumer state plus LAS-01 when available | RedactGuard local-AI infrastructure/domain/UI models/tests | LAS-00; LAS-01 only for model/config fields | yes | BLOCKED |
| LAS-03 | Add side-effect-free setup inspection and fail-closed analysis preflight/revalidation | RedactGuard control-plane runtime/coordinator/tests | LAS-02 | yes | BLOCKED |
| LAS-04 | Introduce top-level `Analizza / AI locale / Impostazioni` information architecture | RedactGuard Compose navigation, state restoration, adaptive navigation, UI tests | LAS-00 | yes | BLOCKED |
| LAS-05 | Build Local AI readiness/setup surface and pre-analysis summary/recovery | RedactGuard Compose UI + accessibility/visual evidence | LAS-02, LAS-03, LAS-04 | no | BLOCKED |
| LAS-06 | Record privacy-safe immutable `AnalysisSetupSnapshot` and attach it to diagnostics | RedactGuard analysis orchestration/diagnostic contracts/tests/docs | LAS-03 | yes | BLOCKED |
| LAS-07 | Cross-repo exact-head validation and representative-device evidence | both repos CI/evidence + physical ARM64 journey | LAS-01..LAS-06 | no | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

Parallel work must have explicit non-conflicting ownership/write boundaries or a defined integration point. After LAS-00, LAS-01 and LAS-04 may proceed in parallel; LAS-02 may begin with existing Consumer use-case/preset/readiness state but must not invent model/config fields while LAS-01 is pending.

## Current executable slice

`LAS-00`

Acceptance:

- `design/ux-contract.json` defines Local AI setup/readiness as a first-class user outcome and documents the three top-level destinations.
- The contract distinguishes connection, configuration, compatibility, runtime readiness, and final analysis readiness.
- The contract states that Local AI inspection is read-only and non-activating/non-preparing.
- The contract states that `Analyze` performs a fresh fail-closed preflight before document content enters inference.
- The contract limits technical details through progressive disclosure and preserves Harness ownership.
- Durable HCP/current-state wording no longer contradicts the new consumer-safe execution projection.

Validation:

- repository documentation/JSON guards selected by `.engineering/commands.json` for the resulting diff.
- review the complete LAS-00 diff against `AGENTS.md`, `skills/plan-workstream/SKILL.md`, and `skills/design-product-experience/SKILL.md`.

## Integration points

- LAS-01 publishes one additive Consumer SDK abstraction for the immutable effective execution setup; RedactGuard consumes that abstraction rather than Binder/Host internals.
- Harness maps the consumer projection from the same canonical resolved execution owner used by activation/session creation so displayed setup cannot drift from executed setup.
- LAS-02 keeps model/configuration fields explicitly unavailable/pending on older Hosts rather than fabricating defaults.
- LAS-03 shares one canonical readiness/preflight result with LAS-05 and LAS-06 so UI eligibility, execution gating, and run evidence do not diverge.
- LAS-04 changes information architecture only; it must not own or duplicate setup resolution logic.
- LAS-06 snapshot identity is captured from the successful preflight/execution contract, not re-derived after generation.

## Validation strategy

Blast radius is cross-repo and includes public Consumer/Binder contracts, privacy-safe diagnostics, lifecycle/readiness and product navigation. Overall workstream validation depth is `STRONG`.

- `LAS-00`: LEAN documentation/design guards.
- `LAS-01`: STRONG Harness contract/Binder/consumer-fixture gates; repository-owned remote automation if equivalent Android tooling is unavailable locally.
- `LAS-02`/`LAS-03`/`LAS-06`: STRONG RedactGuard unit/contract/static/build gates because setup identity changes analysis eligibility/evidence.
- `LAS-04`: SCOPED product-UI/adaptive-navigation/accessibility tests, escalating if shared state/navigation contracts broaden blast radius.
- `LAS-05`: STRONG integrated product-flow + visual evidence because it consumes readiness and recovery state.
- `LAS-07`: deterministic two-APK/API35 evidence on exact heads plus separate `REAL_ENVIRONMENT` ARM64/JNI/GGUF evidence. Emulator/JVM evidence must not be relabeled physical-device evidence.

No deterministic Gradle/Lint/R8/build gate is delegated to the user solely because the agent lacks Android tooling; use repository-owned remote automation and inspect exact-head evidence.

## Durable documentation destinations

- `design/ux-contract.json`: product task model, IA, readiness hierarchy, progressive disclosure, states and recovery.
- `docs/current-state.md`: active LAS boundary while implementation is in progress; final delivered behavior after handoff.
- `docs/workstreams/harness-control-plane-consumer-cutover.md`: narrow boundary clarification so HCP no longer forbids the consumer-safe execution projection introduced by LAS while keeping RG-HCP-8 unchanged.
- Harness shared-runtime/control-plane docs: canonical Consumer execution-setup projection and compatibility behavior introduced by LAS-01.
- RedactGuard local-AI feature/runtime docs: final setup/preflight/snapshot behavior introduced by LAS-02/03/05/06.
- tests/contracts: executable truth for non-activating inspection, stale revision rejection, backwards compatibility, privacy-safe projection, and snapshot/execution identity agreement.

## Initial source-of-truth checkpoint

LAS was started from:

- RedactGuard `dev` at `6627d082ed1f1b8b63b1759662a1b05b0138e5e2`.
- Android Local LLM Harness `dev` at `4dc9d98bb11c21ae32e14ebf32c2e53067fbee98`.

At that checkpoint the Consumer SDK exposes assigned use cases, published presets, activation identity and consumer-safe runtime readiness, while Harness internally owns an exact `ResolvedHostExecution` with model profile and generation overrides. No public Consumer setup projection yet exposes that resolved model/effective generation configuration. LAS-01 therefore represents a real additive contract gap rather than duplicated functionality.

## Completion

The workstream is complete only when applicable code, Consumer/Binder contracts, direct consumers, failure/resource behavior, product UX, accessibility, validation/evidence and durable docs agree.

Completion additionally requires:

- RedactGuard no longer treats Binder connectivity as final Local AI readiness.
- Before Analyze, the UI can show at least use case, preset, resolved model and effective generation configuration when supported by the connected Host.
- Older/incompatible Hosts degrade explicitly and safely rather than guessing setup.
- `Analyze` is fail-closed for incomplete, stale or incompatible setup and does not send document content before preflight succeeds.
- Opening Local AI/Settings causes no model activation/preparation/loading.
- The setup shown immediately before analysis agrees with the immutable run snapshot/execution identity.
- Diagnostics such as `INVALID_FINDINGS` can be correlated privacy-safely with the setup actually used without claiming unsupported causality.
- Exact-head deterministic STRONG gates pass in both repositories, including Consumer/Binder compatibility and two-APK evidence.
- Representative ARM64/JNI/GGUF behavior is recorded separately as `REAL_ENVIRONMENT` evidence where required.

Then update durable documentation/current state and delete this coordination file by default once handoff is complete.
