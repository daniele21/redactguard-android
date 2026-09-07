# Local AI runtime adapter

Status: active
Owner: RedactGuard
Last reviewed: 2026-09-06

RedactGuard owns an Android-independent `AnalysisRuntimePort`. Harnex-specific contracts and Binder types remain confined to `infrastructure/localai`.

The production composition has two explicit layers. `ConsumerAnalysisRuntime` owns strict Consumer inference semantics: capability validation, prepared selection, stateless session lifecycle, JSON-schema generation identity and cancellation. `ControlPlaneAnalysisRuntime` owns the product-level activation lifecycle required by the Harnex Host Control Plane and delegates exact assignment/preset/model/runtime resolution to Harnex.

For each analysis operation RedactGuard discovers the assigned `document-pii-detection` use case, discovers the published preset set, uses only an advertised preset, resolves the exact consumer-safe setup, activates the exact use-case/binding/preset revisions and only then requests Consumer capabilities. One activation spans the complete sequential multi-chunk document analysis. Session close and activation deactivation are separate cleanup steps; success, failure and cancellation all release RedactGuard-owned client state, while Binder/client death is additionally cleaned by the Host connection owner.

Passive setup inspection and explicit runtime readiness are separate axes. `LocalAiSetupStage` stops at `COMPATIBLE`; opening or refreshing the Local AI surface never activates, prepares or loads a model. Runtime readiness is projected independently only from an explicit analysis/preflight execution path. The Analyze path always repeats a fresh fail-closed setup/preflight before document content can enter inference.

Setup/control-plane failures preserve their typed product meaning instead of being collapsed into a generic incompatibility bucket. Configuration required, Harnex authorization required, model unavailable, invalid request, transient runtime unavailability and true protocol/capability incompatibility map to distinct canonical `ProductFailureKind` values with stable `RG-AI-*` codes and typed `FailureRecoveryAction`s. `LocalAiRuntimeState.INCOMPATIBLE` is reserved for real protocol/capability incompatibility; configuration/model/transient failures do not masquerade as incompatibility.

The inference adapter accepts Host capabilities only when the public contract is compatible with RedactGuard: at least one unique advertised preset with exactly one advertised default, `JSON_SCHEMA` only, `STATELESS` only and reasoning `NOT_SUPPORTED`. Consumer limits are converted into app-owned `AnalysisLimits` before crossing the boundary.

Each chunk is sent with the RedactGuard structured-analysis instruction plus framed JSON data and the exact versioned output schema. Prepared/completed execution identity must match the negotiated use case, capability revision, preset, disabled reasoning, JSON schema and stateless session. Any surfaced reasoning, request-ID mismatch or identity drift fails closed.

Unexpected unchecked failures at the Local AI boundary must not collapse into the product-wide `RG-SYS-001` fallback. Control-plane and Consumer SDK call sites convert those failures into the Local AI product failure family and preserve only a bounded privacy-safe step plus a whitelisted exception/type identity. Exception messages, document text, prompts, findings and raw Binder/model payloads are never propagated into product diagnostics.

Known typed Consumer failures preserve two independent identities. The app-owned failure family carries product semantics, while the original `ConsumerErrorCode` is retained only as bounded `AnalysisRuntimeDiagnostic` metadata using `Consumer:<ENUM>`. The diagnostic step identifies the boundary that returned the failure: `consumer.prepare`, `consumer.create-session` or `consumer.generate`. `ConsumerFailure.message` is deliberately discarded and cannot enter product diagnostics. `MODEL_UNAVAILABLE` remains distinct from capability incompatibility; connected runtime/session failures remain transient runtime failures, while transport loss remains disconnected.

Known typed Host/protocol failures follow the same rule. `CONFIGURATION_REQUIRED`, assignment/preset/revision conditions, model unavailable/conflict, invalid request, runtime failure and `FEATURE_UNAVAILABLE` retain distinct typed semantics. A pending/disabled/unapproved signer is an authorization failure, not a protocol mismatch. The lower-level Control Plane rejection code is preserved separately using `ControlPlane:<ENUM>`. Product behavior never parses free-form Host/Binder messages.

The production Binder composition also emits a dedicated `RG_LOCAL_AI` Android log tag for connected-device diagnosis. Transport snapshots are reduced to state plus a whitelist-derived detail identity (`NONE`, known fixed categories or `OTHER`); raw `snapshot.detail` is never logged. The Control Plane coordinator emits only fixed step/result/reason tokens and non-sensitive counts for assignment discovery, preset publication/validation, setup resolution, activation and deactivation. Diagnostic event construction and sink emission are observational and non-interfering: invalid diagnostic metadata or a failing sink is dropped rather than replacing or changing the authoritative connection/readiness/control-plane outcome.

## Binder connection boundary

The Binder composition uses an explicit configured Harnex package/service and never scans installed packages or binds implicitly. Debug targets the Harnex debug host package while release targets the release host package.

RedactGuard does not request a custom Harnex bind permission. Harnex authorization is performed inside the Host from Binder-derived UID -> exact installed RedactGuard package -> current signing certificate -> persisted Harnex authorization -> enabled use case. This keeps independently signed distribution secure without making permission grant timing or install order part of the product contract.

The process-local runtime composition has a reversible `connect()` / `disconnect()` lifecycle. Settings owns the user's connection preference. An explicit disconnect disables reconnect scheduling, releases the current Consumer SDK registration/bind and persists the disabled preference; ordinary `onResume` connection attempts respect that preference. A later explicit Connect re-enables the transport and performs fresh Harnex negotiation/authorization. Disconnect is blocked while RedactGuard owns a non-terminal analysis so transport detachment cannot masquerade as semantic cancellation.

The normal connection badge deliberately distinguishes Binder connectivity from proven inference readiness. `CONNECTED` permits the user to start the verification/analysis path but is presented as "AI locale collegata", not as a green claim that setup, capabilities and runtime preflight have already passed.

Runtime/model internals, GGUF lifecycle, llama.cpp, exact model selection and residency policy remain Harnex-owned. Repository validity requires selector-chosen deterministic gates on the exact candidate head; independent-signer two-APK emulator evidence proves Android/Binder/control-plane/install-order semantics, while representative physical-device evidence remains separate for actual Play signing identities and real ARM64/JNI/GGUF residency, memory, thermal and OEM behavior.
