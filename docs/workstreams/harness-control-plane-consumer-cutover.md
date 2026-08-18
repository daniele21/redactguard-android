# Harness Control Plane consumer cutover

Status: active
Document type: roadmap
Owner: redactguard-android
Canonical scope: workstream.harness-control-plane-consumer-cutover
Read when: adapting RedactGuard to Harness-managed use-case discovery, multiple/custom presets, activation lifecycle or removing consumer-side binding assumptions
Last reviewed: 2026-08-18

RedactGuard remains a pure consumer of the published Harness Consumer Android SDK. Harness owns application registration, use-case definition, preset creation/publication, exact model/configuration binding, residency, host notifications/decisions and complete inference/session telemetry. This repository owns only product-side selection and lifecycle state that a normal consumer is allowed to control.

Canonical Harness plan: `daniele21/android-local-llm-harness/docs/shared-runtime/control-plane/roadmap.md`.

## Product boundary

RedactGuard may:

- connect to Harness through the packaged Consumer SDK;
- discover the use case(s) assigned by Harness;
- discover presets published/exposed by Harness for the selected use case;
- choose a published preset when more than one is available;
- activate/deactivate the chosen local-AI use case when the SDK supports activation;
- create/generate/close analysis sessions through that activation;
- display privacy-safe consumer failures and recovery actions.

RedactGuard must not:

- define Harness use cases;
- create or edit Harness presets;
- choose a concrete model, digest, quantization, context, threads or residency policy;
- persist Harness model/runtime diagnostics;
- recreate application/use-case/model bindings locally;
- assume Fast/Balanced/Quality are fixed protocol values;
- require exactly one host preset.

A user-created Harness preset must be able to appear in RedactGuard through capability refresh without a RedactGuard code release, provided the preset is exposed to RedactGuard and satisfies the use-case contract.

## Dependency map

```text
Harness HCP-20 published preset discovery
      |
      +---------------------> RG-HCP-1 multi-preset tolerant adapter
      |                              |
      |                              v
      |                           RG-HCP-2 preset product state
      |                              |
      |                              v
      |                           RG-HCP-3 selector UI
      |
Harness HCP-19 assigned use-case discovery
      |
      +---------------------> RG-HCP-4 host-assigned use case

Harness HCP-21 activation protocol
      |
      +---------------------> RG-HCP-5 activation lifecycle
                                      |
                                      v
                                   RG-HCP-6 lifecycle/recovery

RG-HCP-1..6 + Harness HCP-27
      -> RG-HCP-7 hardcoded assumption removal
      -> RG-HCP-8 physical two-APK validation
```

RG-HCP-1/2 may begin before the Harness Binder evolution because the current Consumer API already advertises a preset collection and an explicit default. RG-HCP-4/5 require the corresponding new published SDK surface.

## RG-HCP-1 — Accept multiple host-published presets

State: **PLANNED**
Dependencies: current Consumer API only; no Harness HCP-21 dependency.

Current `ConsumerAnalysisRuntime` rejects any capability set where `presets.size != 1`. Remove that consumer-only restriction while retaining RedactGuard's real invariants:

- required use-case identity until assigned-use-case discovery is available;
- JSON Schema output;
- stateless session;
- no surfaced reasoning;
- valid host default preset;
- selected preset must belong to the current capability revision.

Preparation must explicitly request the selected host preset when the SDK supports that field. Until product selection is wired, the host default is used.

Tests:

- one default preset remains valid;
- multiple presets with one valid default are valid;
- missing/invalid default fails closed;
- selected preset not advertised fails closed;
- changed capability revision does not silently reuse stale selection.

Exit gate: multiple host presets no longer produce `CAPABILITY_INCOMPATIBLE` solely because there is more than one.

## RG-HCP-2 — Product preset state

State: **PLANNED**
Dependencies: RG-HCP-1.

Add process-local product state containing only consumer-safe metadata:

- preset ID/ref and revision;
- display name/description when supplied by the SDK;
- default marker;
- current selection;
- capability revision that selection belongs to.

Rules:

- exactly one published preset -> select automatically and hide unnecessary selector;
- multiple presets -> default selection starts from Harness default unless the current in-memory choice is still valid;
- withdrawn/stale choice -> refresh capabilities and fall back only to the newly advertised default with visible state change; never map to a concrete model locally;
- no sensitive document state is coupled to preset persistence;
- v1 selection remains process-local unless a later explicit product requirement justifies preference persistence.

Exit gate: analysis execution can receive an explicit selected host preset without knowing its model configuration.

## RG-HCP-3 — Preset selector UI

State: **PLANNED**
Dependencies: RG-HCP-2 and host metadata sufficient for human-readable options.

UI behavior:

- no selector when only one preset is available;
- compact selector when multiple presets are available;
- show Harness-provided display name and short description/intent;
- do not expose model, quantization, context, threads, cache or residency;
- custom Harness presets appear like any other published option;
- loading, unavailable, stale/refreshing and selection-change states are explicit;
- changing preset is disabled or deferred while an analysis operation is active unless the application lifecycle can safely restart the operation.

Accessibility and adaptive layout use the repository product-ui contract.

Exit gate: user can choose among Harness-published presets without being forced to understand runtime internals.

## RG-HCP-4 — Host-assigned use-case discovery

State: **BLOCKED BY HARNESS HCP-19**

Replace the compile-time assumption that `document-pii-detection` must always be the requested Harness use case with SDK discovery of use cases assigned to RedactGuard.

RedactGuard may still validate that an assigned use case satisfies the PII analysis contract. If exactly one compatible default is assigned, it is selected automatically. If none are assigned, show an actionable Harness-configuration-required state rather than a generic compatibility error.

Do not let RedactGuard create or rename Harness use cases.

Exit gate: the Harness application/use-case binding can change without shipping a new hardcoded binding in RedactGuard.

## RG-HCP-5 — Activation lifecycle

State: **BLOCKED BY HARNESS HCP-21**

Adopt the SDK activation/deactivation lifecycle so product-level "Local AI active" owns one Harness activation lease across multiple analysis sessions.

Required semantics:

- connect/discover -> select use case/preset -> activate;
- sessions are created under the active resolved execution;
- closing one session does not deactivate Local AI;
- user disable/close of the product feature deactivates explicitly;
- ViewModel/application teardown closes owned activation;
- Binder/process death cleanup remains host-enforced and client close remains idempotent;
- reconnect requires a new discovery/activation rather than reusing stale activation IDs.

Exit gate: RedactGuard's product lifetime maps to Harness residency ownership without local residency controls.

## RG-HCP-6 — Recovery and evidence mapping

State: **PLANNED after RG-HCP-4/5**

Map new host failures to actionable product states while preserving technical evidence already required by `failure-diagnostics-hardening.md`:

- application pending/not configured;
- use case not bound;
- no published preset;
- stale preset/capability revision;
- model unavailable/broken preset;
- active model conflict;
- activation revoked by critical memory pressure;
- host restart/disconnect.

User recovery should direct configuration decisions to Harness when Harness owns the fix. RedactGuard must not emulate or repair Harness bindings locally.

Exit gate: every control-plane failure identifies the correct owner and next action.

## RG-HCP-7 — Remove consumer binding assumptions

State: **PLANNED**
Dependencies: RG-HCP-1 through RG-HCP-6 and Harness HCP-27 migration readiness.

Remove obsolete assumptions and tests including:

- exactly one exposed preset;
- consumer-owned fixed preset mapping;
- hardcoded use-case request when assigned-use-case discovery is available;
- any product copy that tells the user to choose/load a concrete Harness model;
- any workaround that treats Harness global selected model as RedactGuard configuration.

Keep the document PII functional contract in RedactGuard: schema, prompt, input chunking, result validation, review/redaction/export remain product-owned.

Exit gate: RedactGuard contains no Harness model/binding administration logic.

## RG-HCP-8 — Cross-repository validation

State: **PLANNED**
Dependencies: Harness HCP final candidate + RG-HCP-7.

Required deterministic/device matrix:

| Scenario | RedactGuard expectation |
| --- | --- |
| one published preset | automatic selection, no selector |
| Fast + Quality + custom published | all safe names visible; user can select |
| custom preset withdrawn | stale selection refreshes/fails explicitly |
| app has no Harness binding | actionable configure-Harness state |
| Local AI active between chunks/sessions | consumer remains active; host owns residency |
| host restarts | disconnected -> reconnect -> rediscover -> reactivate |
| memory-pressure activation revocation | typed recoverable failure with evidence |
| preset changed in Harness during active analysis | current operation remains pinned; next activation sees new revision |
| physical multi-chunk document analysis | complete result or atomic failure; no partial findings leak |

Physical evidence must record exact RedactGuard APK, Harness APK/SDK/protocol, model/preset revision and device identity without prompt/document content.

## Parallel work policy

Proceed immediately with RG-HCP-1 and RG-HCP-2 on a focused branch because they depend only on the existing public capability contract. UI preparation for RG-HCP-3 may use fake multi-preset metadata after RG-HCP-2. Keep RG-HCP-4/5 behind the explicit Harness SDK dependencies rather than inventing local protocol substitutes.

When a task becomes implemented, update this workstream state and `docs/current-state.md`. Delete this workstream after the complete cutover and transfer durable behavior to architecture/features/runbooks, in line with repository documentation policy.
