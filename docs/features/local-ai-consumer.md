# Local AI consumer boundary

Status: active
Owner: RedactGuard
Last reviewed: 2026-09-06

RedactGuard consumes Local AI only through the externally published Harnex Consumer Android SDK. The integrated `dev` baseline now pins:

`io.github.daniele21.localllm:consumer-android:0.1.0-alpha.11`

Harnex `0.1.0-alpha.11` was published immutably from the validated Harnex `dev` merge and passed anonymous downstream-consumption evidence. RedactGuard therefore consumes the published alpha.11 artifact in its normal build. Exact-source cross-repository E2E remains a deliberate integration-evidence path and consumes a repository-owned source-built Maven candidate artifact tied to an exact Harnex revision.

The primary public repository is the token-free Maven tree published by Harnex at the dedicated `consumer-sdk-maven` branch and served through the public raw GitHub endpoint. RedactGuard requires no package credential, personal access token, Harnex source checkout, composite build, git submodule or copied Binder client to resolve a published SDK. Exact-candidate cross-repository E2E is the deliberate source-revision exception and consumes a repository-owned source-built Maven candidate artifact.

The Consumer SDK exposes the public Consumer inference API, Consumer Control Plane API, source-backed passive setup-resolution surface, source-backed runtime-readiness surface and `BinderConsumerLocalLlmClient`. Alpha.11 adds reversible `disconnect()` so a user-owned Settings disconnect can release the current registration/bind while keeping the client reusable for a later explicit Connect. Runtime/model/llama.cpp modules are not RedactGuard dependencies.

## Independent signing and authorization

RedactGuard and Harnex are independently distributed Android applications. RedactGuard does not share Harnex signing credentials and does not request a custom Harnex bind permission. It binds only to the exact configured Harnex package/service component.

Service reachability is not authorization. Harnex derives the real Binder calling UID, resolves the exact installed RedactGuard package and current signing certificate, then applies persisted Harnex Control Plane authorization and enabled use-case policy. RedactGuard never supplies a trusted package name, signer digest, UID or application identity to authorize itself.

A newly observed RedactGuard identity is `PENDING` until the user explicitly authorizes that exact package/signer identity in Harnex. A later signing identity replacement fails closed and requires explicit reauthorization. RedactGuard surfaces the resulting Host rejection as an authorization-required recovery state and offers to open Harnex; it never asks a normal user to copy or type signing fingerprints.

The public service intentionally has no custom bind permission because Android API 35 cross-APK evidence showed that a Consumer installed before a Host that later defines a custom `normal` permission may remain denied until reinstall. Consumer-before-Host and Host-before-Consumer install order must converge on the same Binder authorization semantics without reinstalling RedactGuard.

## Control-plane lifecycle

Before requesting Consumer capabilities for `document-pii-detection`, RedactGuard follows the Host-owned control-plane lifecycle: discover its assigned use case, discover published presets, select only an advertised preset (Host default when no product selection exists), resolve the exact consumer-safe setup, activate the exact use-case/binding/preset revisions, then run capability preparation/session/generation. The activation is retained across all chunks of one document analysis and released on success, failure, cancellation or explicit close.

Passive setup resolution is observational only. It does not activate a preset, prepare/load a model, create an inference session or mutate Harnex-owned configuration. RedactGuard treats the resolved setup as compatibility/configuration evidence, not as final Analyze readiness; a fresh fail-closed preflight is repeated immediately before document content may enter inference.

RedactGuard never receives or chooses a concrete model digest, quantization, thread count, cache policy or residency setting. Harnex remains the sole owner of exact execution resolution and activation-protected model residency. Consumer-safe model/configuration fields are displayed only when source-backed by the Host contract.

## Failure and privacy boundary

Control-plane failures remain typed through the RedactGuard boundary. Configuration-required conditions, authorization required, model unavailable/conflict, invalid request, transport/runtime failure and true feature/capability incompatibility map to distinct app-owned analysis/product categories. `FEATURE_UNAVAILABLE` is the capability-incompatibility path; model/configuration/transient failures are not re-labelled as incompatible. The original `ConsumerControlPlaneErrorCode` is retained only as bounded `AnalysisRuntimeDiagnostic` metadata using `ControlPlane:<ENUM>`; free-form Host messages are discarded.

Consumer inference failures follow the same separation. `MODEL_UNAVAILABLE` retains a model-unavailable product meaning, capability incompatibility remains explicit, transport loss remains disconnected and connected runtime/session failures remain transient runtime/generation failures. The lower-level `ConsumerErrorCode` is retained only as bounded `Consumer:<ENUM>` diagnostic identity.

`RG_LOCAL_AI` emits only whitelisted technical identities: transport state, a bounded transport-detail classification, Control Plane step/result/reason and non-sensitive counts. It never logs document text, prompts, finding values, model output, raw Binder payloads, model paths/digests or arbitrary exception messages. Unknown free-form Binder detail is collapsed to `OTHER` rather than copied into logs. Diagnostic emission is observational and cannot replace an authoritative typed outcome.

Harnex publishes the Consumer SDK with ABI and publication evidence. Repository tests and emulator integration evidence do not substitute for final representative physical-device evidence where actual Play App Signing identity, real ARM64/JNI/GGUF residency, memory, thermal or OEM behavior is material.
