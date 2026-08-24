# Harness Control Plane consumer cutover

Status: active
Owner: redactguard-android
Read when: adapting RedactGuard to Harness-managed presets, assigned use cases, activation lifecycle or removing consumer-side binding assumptions
Last reviewed: 2026-08-24

## Goal

Keep RedactGuard a pure Consumer SDK client while progressively replacing consumer-side assumptions with Harness-owned discovery/activation contracts. User-facing choices may select only host-published consumer-safe options; RedactGuard never owns concrete model/runtime configuration.

Canonical Harness owner: `daniele21/android-local-llm-harness/docs/shared-runtime/control-plane/roadmap.md`.

## Non-goals

- defining/editing Harness applications, use cases or presets;
- choosing concrete model, digest, quantization, context, threads, cache or residency policy;
- copying Harness Binder/runtime implementation into RedactGuard;
- persisting Host/model diagnostics as RedactGuard product state.

## Invariants

- Harness owns application registration, use-case/preset publication, model/config binding, residency and Host telemetry/decisions;
- RedactGuard owns only consumer-safe selection/lifecycle state exposed by the published SDK;
- multiple valid published presets must not be rejected merely because there is more than one;
- selected preset identity must be advertised in the current capability revision;
- stale capability/preset identity fails closed or refreshes to a truthfully advertised default; it never maps locally to a model;
- normal UI uses task language, not model/runtime internals;
- sensitive document state is independent of preset/control-plane state.

## Work graph

| ID | Work | Depends on | State |
| --- | --- | --- | --- |
| RG-HCP-1 | Multi-preset tolerant Consumer adapter + explicit advertised/default preset request | current Consumer API | DONE |
| RG-HCP-2 | Process-local product preset state and stale-selection handling | RG-HCP-1 | READY |
| RG-HCP-3 | Progressive preset selector UI only when multiple human-readable options exist | RG-HCP-2 + product-ui | BLOCKED |
| RG-HCP-4 | Host-assigned compatible use-case discovery | Harness HCP-19 / SDK surface | BLOCKED |
| RG-HCP-5 | Consumer activation/deactivation lease lifecycle | Harness HCP-21 / SDK surface | BLOCKED |
| RG-HCP-6 | New control-plane failure/recovery projection | RG-HCP-4, RG-HCP-5 | BLOCKED |
| RG-HCP-7 | Remove obsolete hardcoded consumer binding assumptions | RG-HCP-2..6 + Harness migration readiness | BLOCKED |
| RG-HCP-8 | Cross-repository/physical two-APK validation | RG-HCP-7 + Harness candidate | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

## Integrated slice — RG-HCP-1

Merged implementation accepts multiple Host-published presets, pins prepare to the current capability revision, explicitly requests the Host default or an injected advertised preset, rejects duplicate/non-advertised preset identities and verifies the prepared/execution preset matches the request. It preserves JSON-schema, stateless-session and no-reasoning constraints. Multiple valid presets no longer produce incompatibility solely because their count is greater than one. PR #51 is implementation history, not the durable owner of this behavior. fileciteturn161file0

## Current executable slice — RG-HCP-2

Define the smallest process-local product state needed to represent published preset metadata and current selection without learning concrete model configuration.

Acceptance:

- one published preset -> select automatically; no selector needed;
- multiple presets -> begin with the Host-declared default unless an in-memory selection is still advertised for the current revision;
- withdrawn/stale selection -> refresh capability and use only a newly advertised valid/default identity with visible state change when user-relevant;
- selection is not persisted with sensitive document state;
- analysis receives only an advertised preset reference, never model/runtime parameters.

RG-HCP-2 may proceed before assigned-use-case/activation protocol work because the current Consumer capability already exposes a preset collection/default.

## Blocked integration points

RG-HCP-3 should be implemented together with the product-ui contract: hide the selector for one option; expose a compact, accessible selector only for multiple useful Host-provided display options; do not show model/quantization/context/threads/cache/residency.

RG-HCP-4 and RG-HCP-5 must wait for the corresponding published Harness SDK contracts. Do not invent a local protocol substitute. New Host failures from those contracts feed the existing stable RedactGuard failure/recovery system through RG-HCP-6.

RG-HCP-8 must cover one/multiple/custom/withdrawn preset behavior, missing binding, Host restart, activation recovery/revocation and complete multi-chunk analysis on exact APK/SDK/device identities without capturing document/prompt content.

## Durable destinations and completion

- `docs/features/local-ai-consumer.md` / runtime-adapter feature docs: final consumer behavior;
- `design/ux-contract.json`: selection/disclosure/error semantics;
- tests: capability revision/preset selection invariants;
- physical evidence: exact cross-repo/device identity only.

Delete this workstream after the complete cutover and durable handoff. Until then, keep Harness-dependent slices explicitly blocked rather than reconstructing Host ownership inside RedactGuard.
