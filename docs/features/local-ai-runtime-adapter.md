# Local AI runtime adapter

Status: integration in progress
Owner: RedactGuard

RedactGuard owns an Android-independent `AnalysisRuntimePort`. Harness-specific contracts and Binder types remain confined to `infrastructure/localai`.

The production adapter requests only the `document-pii-detection` use case. It accepts the host-selected default model/preset only when the public capability contract is exactly compatible with RedactGuard: one default preset, `JSON_SCHEMA` only, `STATELESS` only and reasoning `NOT_SUPPORTED`. Consumer limits are converted into app-owned `AnalysisLimits` before crossing the boundary.

Each chunk is sent with the RedactGuard structured-analysis instruction plus framed JSON data and the exact versioned output schema. Prepared/completed execution identity must match the negotiated use case, capability revision, preset, disabled reasoning, JSON schema and stateless session. Any surfaced reasoning, request-ID mismatch or identity drift fails closed.

The Binder composition uses an explicit configured Harness package/service and Android permission; it never scans installed packages or binds implicitly. Debug targets the Harness debug host package while release targets the release host package.

Cancellation is propagated to the active generation handle and sessions are closed explicitly. Runtime/model internals, GGUF lifecycle and llama.cpp remain Harness-owned.

The migration branch is formatted by the repository Spotless policy before exact-head `Validate`; the runtime adapter is considered repository-valid only when unit tests, Android Lint and debug assembly pass on that same formatted head.
