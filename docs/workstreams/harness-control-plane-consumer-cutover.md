# Harness Control Plane consumer cutover

Status: active
Owner: redactguard-android
Read when: adapting RedactGuard to Harness-managed presets, assigned use cases, activation lifecycle or removing consumer-side binding assumptions
Last reviewed: 2026-08-25

## Goal

Keep RedactGuard a pure Consumer SDK client while replacing consumer-side assumptions with Harness-owned discovery/activation contracts. User-facing choices may select only host-published consumer-safe options; RedactGuard never owns concrete model/runtime configuration.

Canonical Harness owner: `daniele21/android-local-llm-harness/docs/shared-runtime/control-plane/roadmap.md`.

## Non-goals

- defining/editing Harness applications, use cases or presets;
- choosing concrete model, digest, quantization, context, threads, cache or residency policy;
- copying Harness Binder/runtime implementation into RedactGuard;
- persisting Host/model diagnostics as RedactGuard product state.

## Invariants

- Harness owns application registration, use-case/preset publication, model/config binding, residency and Host telemetry/decisions;
- RedactGuard owns only consumer-safe selection/lifecycle state exposed by the published SDK;
- external analysis activates the Host-owned use-case/binding/preset revision before requesting inference capabilities;
- one activation spans the complete sequential document analysis and is released on success, failure, cancellation or explicit close;
- multiple valid published presets must not be rejected merely because there is more than one;
- selected preset identity must be advertised by Harness; stale/withdrawn identity fails closed and is never mapped locally to a model;
- normal UI uses task language and does not present Binder connectivity as proof that inference configuration is already ready;
- sensitive document state is independent of preset/control-plane state.

## Work graph

| ID | Work | Depends on | State |
| --- | --- | --- | --- |
| RG-HCP-1 | Multi-preset tolerant Consumer adapter + explicit advertised/default preset request | current Consumer API | DONE |
| RG-HCP-2 | Process-local product preset state and stale-selection handling | RG-HCP-1 | DONE |
| RG-HCP-3 | Progressive preset selector UI only when multiple human-readable options exist | RG-HCP-2 + product-ui | DONE |
| RG-HCP-4 | Host-assigned compatible use-case discovery | Harness published Control Plane SDK | DONE |
| RG-HCP-5 | Consumer activation/deactivation lease lifecycle | RG-HCP-4 + Harness activation API | DONE |
| RG-HCP-6 | Control-plane failure/recovery projection + truthful connected state | RG-HCP-4, RG-HCP-5 | DONE |
| RG-HCP-7 | Remove obsolete hardcoded consumer binding assumptions | RG-HCP-2..6 + Harness migration readiness | DONE |
| RG-HCP-8 | Cross-repository/physical two-APK validation | RG-HCP-7 + Harness candidate | ACTIVE |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

## Integrated slice — RG-HCP-1

Merged implementation accepts multiple Host-published capability presets, pins prepare to the current capability revision, explicitly requests the Host default or an injected advertised preset, rejects duplicate/non-advertised preset identities and verifies the prepared/execution preset matches the request. It preserves JSON-schema, stateless-session and no-reasoning constraints.

## Integrated slice — RG-HCP-4/5/6

RedactGuard now uses the publicly published `consumer-android:0.1.0-alpha.4` Control Plane surface rather than reconstructing Host policy locally. Alpha.4 is present in Harness's token-free `consumer-sdk-maven` repository and its published ABI evidence contains the activation, assigned-use-case, published-preset and `ConsumerControlPlaneClient` contracts required by this cutover. Alpha.3 is intentionally no longer sufficient.

Execution order:

1. discover RedactGuard's assigned `document-pii-detection` use case;
2. discover the exact published preset set and binding revision;
3. select only an advertised preset;
4. activate the exact use-case revision, binding revision and preset revision;
5. request Consumer capabilities and create the strict JSON-schema/stateless session only after activation;
6. keep the activation across every chunk of the atomic document analysis;
7. close the inference session and deactivate the product-level activation on terminal cleanup.

Cancellation before or during preparation, preparation failure, generation failure and normal success all converge on cleanup. Explicit deactivation is best effort after a disconnected transport because Harness additionally owns Binder/client-death activation cleanup. A connected Host deactivation failure remains a real cleanup failure rather than being silently ignored.

Control-plane failure projection remains product-level: transport loss -> disconnected; model/configuration/conflict conditions -> Host unavailable; invalid/missing assignment, stale revision or non-advertised preset -> capability incompatible. No prompt/document/finding value is added to diagnostics.

The connection badge reports `AI locale collegata` for Binder connectivity. It still enables the normal Analyze action, but no longer makes the stronger green claim `AI locale pronta`; assignment/preset/capability readiness is verified by the analysis lifecycle itself.

Repository validation completed on PR #77 exact head `b0d9d81d761ffaca411fdc826f1206156a1e6c5a` with `Validate` and `Repository health` passing. The slice was merged into `dev` as commit `419a0a9e89fbdd6385396444e4de02993cd436cc`. Physical-device evidence remains a separate stronger gate and cannot be inferred from green JVM/CI checks.

## Integrated slice — RG-HCP-2

PR #82 added the smallest process-local product state needed to represent published preset metadata and current selection without learning concrete model configuration.

Integrated behavior:

- one published preset -> select automatically even when a Host default marker is unnecessary;
- multiple presets -> begin with the Host-declared default unless an in-memory selection is still advertised as the exact current preset reference;
- an in-memory selection withdrawn by a later Host refresh -> replace it with the newly advertised valid/default identity and expose that replacement in process-local state;
- an explicit non-advertised preset request remains fail-closed rather than silently changing user intent;
- only consumer-safe display name, description, default flag and opaque `InferencePresetRef` are retained;
- selection state is process-local only and is not written to `SavedStateHandle`, preferences, files or databases;
- the same process-local selected reference is shared by Control Plane activation and subsequent Consumer prepare, preventing activation/data-plane preset drift;
- analysis receives only an advertised preset reference, never model/runtime parameters.

Exact-head `Validate`, `Repository health` and repository formatting passed for `075ee82522c048de052d66f46b142b0d9bcb134e`. PR #82 was merged into canonical `dev` as `860792986537716a1c3f625a5fe6dc132a48ef0c`.

## Integrated slice — RG-HCP-3

PR #85 added progressive disclosure for Host-published preset choices without exposing runtime identity.

Integrated behavior:

- best-effort discovery may populate consumer-safe preset display metadata before analysis, while authoritative assignment/preset discovery and activation still occur when analysis starts;
- zero or one useful option keeps the selector hidden and preserves the sensible automatic choice;
- multiple human-readable options expose a compact accessible selector in the protection-selection step;
- UI receives process-local `preset-N` identifiers plus Host-published display name/description only; raw `InferencePresetRef`, revisions, model IDs and runtime configuration never cross into product UI models;
- a withdrawn in-memory choice may surface a polite user-relevant replacement notice while the authoritative Host state remains the source of truth;
- selection remains independent of sensitive document/review state and is not persisted.

Exact-head `Validate`, `Repository health` and repository formatting passed for `9fdd9ecee3d913ecc39dd3991409928d423d1cff`, including native UI test APK assembly, Android Lint, debug APK and minified release APK. PR #85 was merged into canonical `dev` as `53e9a5c70aaf9e69ffe936ee33cf79a07c0c045f`.

## Integrated slice — RG-HCP-7

PR #89 removed the compatibility-era Consumer data-plane fallback once the Control Plane owns the exact advertised preset for the analysis activation.

Integrated behavior:

- Consumer prepare requires the process-local Control Plane-selected `InferencePresetRef` rather than silently falling back to `UseCaseCapabilities.defaultPreset`;
- the selected preset must still be advertised by the current capability revision;
- capability metadata may omit a default preset when the Control Plane already supplied a valid selected preset;
- default metadata is validated only when the capability payload actually publishes a default;
- `document-pii-detection`, JSON-schema-only output, stateless sessions, disabled reasoning and execution-identity checks remain explicit product protocol constraints;
- missing Control Plane-selected preset fails closed before Consumer prepare/session creation.

Exact-head `Validate`, `Repository health` and repository formatting passed for `7c9c121a6737d1b454a02f7bf17656f0d12fc923`, including JVM tests, native UI test APK assembly, Android Lint, debug APK and minified release APK. PR #89 was merged into canonical `dev` as `be3ee4ce30e796ab282a7abdac4cc386e8dadb53`.

## Active validation slice — RG-HCP-8

The next physical gate uses new, unambiguous candidate identities rather than reusing earlier v8/v27 evidence:

- RedactGuard candidate: `versionCode=9`, `versionName=0.1.4`, branch `chore/hcp8-redactguard-v9` based on post-HCP-7 `dev`;
- Harness phone candidate: `versionCode=28`, `versionName=1.0.0`, PR #439 / branch `chore/hcp8-phone-candidate-v28`;
- Consumer SDK remains `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.4`.

Harness versionCode 27 is intentionally not reused because that identity already appeared in earlier physical RedactGuard evidence. Candidate repository CI and package identity must be green before device evidence is collected.

RG-HCP-8 must cover one/multiple/custom/withdrawn preset behavior, missing binding, Host restart, activation recovery/revocation and complete multi-chunk analysis on exact APK/SDK/device identities without capturing document/prompt content. Physical evidence remains a separate stronger gate and must not be inferred from repository CI.

## Durable destinations and completion

- `docs/features/local-ai-consumer.md` / `docs/features/local-ai-runtime-adapter.md`: final consumer behavior;
- `design/ux-contract.json`: selection/disclosure/error semantics;
- tests: discovery, activation/deactivation, capability revision and preset-selection invariants;
- physical evidence: exact cross-repo/device identity only.

Delete this workstream after the complete cutover and durable handoff. Until then, keep remaining slices explicit instead of reconstructing Host ownership inside RedactGuard.
