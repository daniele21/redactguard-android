# Local AI runtime adapter

Status: active
Owner: RedactGuard

RedactGuard owns an Android-independent `AnalysisRuntimePort`. Harness-specific contracts and Binder types remain confined to `infrastructure/localai`.

The production composition has two explicit layers. `ConsumerAnalysisRuntime` owns strict Consumer inference semantics: capability validation, prepared selection, stateless session lifecycle, JSON-schema generation identity and cancellation. `ControlPlaneAnalysisRuntime` owns the product-level activation lifecycle required by the current Harness Host Control Plane and delegates exact assignment/preset/model/runtime resolution to Harness.

For each analysis operation RedactGuard discovers the assigned `document-pii-detection` use case, discovers the published preset set, uses only an advertised preset, activates the exact use-case/binding/preset revisions and only then requests Consumer capabilities. One activation spans the complete sequential multi-chunk document analysis. Session close and activation deactivation are separate cleanup steps; success, failure and cancellation all release RedactGuard-owned client state, while Binder/client death is additionally cleaned by the Host connection owner.

The inference adapter accepts Host capabilities only when the public contract is compatible with RedactGuard: at least one unique advertised preset with exactly one advertised default, `JSON_SCHEMA` only, `STATELESS` only and reasoning `NOT_SUPPORTED`. Consumer limits are converted into app-owned `AnalysisLimits` before crossing the boundary.

Each chunk is sent with the RedactGuard structured-analysis instruction plus framed JSON data and the exact versioned output schema. Prepared/completed execution identity must match the negotiated use case, capability revision, preset, disabled reasoning, JSON schema and stateless session. Any surfaced reasoning, request-ID mismatch or identity drift fails closed.

Unexpected unchecked failures at the Local AI boundary must not collapse into the product-wide `RG-SYS-001` fallback. Control-plane and Consumer SDK call sites convert those failures into `RG-AI-012 LOCAL_AI_INTERNAL` and preserve only a bounded privacy-safe step plus a whitelisted exception type. Exception messages, document text, prompts, findings and raw Binder/model payloads are never propagated into product diagnostics. Known typed Host/protocol failures keep their existing stable cause codes.

The Binder composition uses an explicit configured Harness package/service and Android permission; it never scans installed packages or binds implicitly. Debug targets the Harness debug host package while release targets the release host package.

The normal connection badge deliberately distinguishes Binder connectivity from proven inference readiness. `CONNECTED` permits the user to start the verification/analysis path but is presented as "AI locale collegata", not as a green claim that capabilities and Host configuration have already passed.

Runtime/model internals, GGUF lifecycle, llama.cpp, exact model selection and residency policy remain Harness-owned. Repository validity requires unit tests, Android Lint and build gates on the exact candidate head; physical two-APK evidence remains a separate gate.
