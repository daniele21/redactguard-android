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
| RG-HCP-2 | Process-local product preset state and stale-selection handling | RG-HCP-1 | READY |
| RG-HCP-3 | Progressive preset selector UI only when multiple human-readable options exist | RG-HCP-2 + product-ui | BLOCKED |
| RG-HCP-4 | Host-assigned compatible use-case discovery | Harness published Control Plane SDK | DONE |
| RG-HCP-5 | Consumer activation/deactivation lease lifecycle | RG-HCP-4 + Harness activation API | DONE |
| RG-HCP-6 | Control-plane failure/recovery projection + truthful connected state | RG-HCP-4, RG-HCP-5 | DONE |
| RG-HCP-7 | Remove obsolete hardcoded consumer binding assumptions | RG-HCP-2..6 + Harness migration readiness | BLOCKED |
| RG-HCP-8 | Cross-repository/physical two-APK validation | RG-HCP-7 + Harness candidate | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

## Integrated slice — RG-HCP-1

Merged implementation accepts multiple Host-published capability presets, pins prepare to the current capability revision, explicitly requests the Host default or an injected advertised preset, rejects duplicate/non-advertised preset identities and verifies the prepared/execution preset matches the request. It preserves JSON-schema, stateless-session and no-reasoning constraints.

## Integrated slice — RG-HCP-4/5/6

RedactGuard now uses the publicly published `consumer-android:0.1.0-alpha.4` Control Plane surface rather than reconstructing Host policy locally. Alpha.4 is present in Harness's token-free `consumer-sdk-maven` repository and its published ABI evidence contains the activation, assigned-use-case, published-preset and `ConsumerControlPlaneClient` contracts required by this cutover. Alpha.3 is intentionally no longer sufficient.

Execution order:

1. discover RedactGuard's assigned `document-pii-detection` use case;
2. discover the exact published preset set and binding revision;
3. select only an advertised preset, using the single Host-declared default when no product selection exists;
4. activate the exact use-case revision, binding revision and preset revision;
5. request Consumer capabilities and create the strict JSON-schema/stateless session only after activation;
6. keep the activation across every chunk of the atomic document analysis;
7. close the inference session and deactivate the product-level activation on terminal cleanup.

Cancellation before or during preparation, preparation failure, generation failure and normal success all converge on cleanup. Explicit deactivation is best effort after a disconnected transport because Harness additionally owns Binder/client-death activation cleanup. A connected Host deactivation failure remains a real cleanup failure rather than being silently ignored.

Control-plane failure projection remains product-level: transport loss -> disconnected; model/configuration/conflict conditions -> Host unavailable; invalid/missing assignment, stale revision or non-advertised preset -> capability incompatible. No prompt/document/finding value is added to diagnostics.

The connection badge reports `AI locale collegata` for Binder connectivity. It still enables the normal Analyze action, but no longer makes the stronger green claim `AI locale pronta`; assignment/preset/capability readiness is verified by the analysis lifecycle itself.

Repository validation completed on PR #77 exact head `b0d9d81d761ffaca411fdc826f1206156a1e6c5a` with `Validate` and `Repository health` passing. The slice was merged into `dev` as commit `419a0a9e89fbdd6385396444e4de02993cd436cc`. Physical-device evidence remains a separate stronger gate and cannot be inferred from green JVM/CI checks.

## Current executable slice — RG-HCP-2

Define the smallest process-local product state needed to represent published preset metadata and current selection without learning concrete model configuration.

Acceptance:

- one published preset -> select automatically; no selector needed;
- multiple presets -> begin with the Host-declared default unless an in-memory selection is still advertised for the current revision;
- withdrawn/stale selection -> refresh discovery and use only a newly advertised valid/default identity with visible state change when user-relevant;
- selection is not persisted with sensitive document state;
- analysis receives only an advertised preset reference, never model/runtime parameters.

The activation coordinator already accepts an injected advertised preset reference, but durable process-local selection/display metadata remains RG-HCP-2 work.

## Remaining integration points

RG-HCP-3 should be implemented together with the product-ui contract: hide the selector for one option; expose a compact, accessible selector only for multiple useful Host-provided display options; do not show model/quantization/context/threads/cache/residency.

RG-HCP-7 removes the remaining compatibility-era hardcoded binding assumptions only after RG-HCP-2 is integrated and the Harness migration boundary is confirmed. Do not remove compatibility code merely to make the branch look complete.

RG-HCP-8 must cover one/multiple/custom/withdrawn preset behavior, missing binding, Host restart, activation recovery/revocation and complete multi-chunk analysis on exact APK/SDK/device identities without capturing document/prompt content. The immediate physical regression candidate is RedactGuard `versionCode=7` against the already installed Harness `versionCode=27`; that result remains `PENDING` until run on the device.

## Durable destinations and completion

- `docs/features/local-ai-consumer.md` / `docs/features/local-ai-runtime-adapter.md`: final consumer behavior;
- `design/ux-contract.json`: selection/disclosure/error semantics when RG-HCP-2/3 materially extend the current UI;
- tests: discovery, activation/deactivation, capability revision and preset-selection invariants;
- physical evidence: exact cross-repo/device identity only.

Delete this workstream after the complete cutover and durable handoff. Until then, keep remaining slices explicit instead of reconstructing Host ownership inside RedactGuard.
