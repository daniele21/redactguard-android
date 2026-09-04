# Local AI consumer boundary

Status: active
Owner: RedactGuard

RedactGuard consumes Local AI only through the externally published Harness Consumer Android SDK. The pinned cross-repository artifact is:

`io.github.daniele21.localllm:consumer-android:0.1.0-alpha.9`

The primary public repository is the token-free Maven tree published by Harness at the dedicated `consumer-sdk-maven` branch and served through the public raw GitHub endpoint. RedactGuard requires no package credential, personal access token, Harness source checkout, composite build, git submodule or copied Binder client to resolve the SDK. Harness may retain GitHub Packages as an authenticated/internal secondary channel.

`consumer-android` alpha.9 exposes the public Consumer inference API, Consumer Control Plane API, source-backed passive setup-resolution surface, source-backed runtime-readiness surface and `BinderConsumerLocalLlmClient`. Runtime/model/llama.cpp modules are not RedactGuard dependencies.

Before requesting Consumer capabilities for `document-pii-detection`, RedactGuard follows the Host-owned control-plane lifecycle: discover its assigned use case, discover published presets, select only an advertised preset (Host default when no product selection exists), resolve the exact consumer-safe setup, activate the exact use-case/binding/preset revisions, then run capability preparation/session/generation. The activation is retained across all chunks of one document analysis and released on success, failure, cancellation or explicit close.

Passive setup resolution is observational only. It does not activate a preset, prepare/load a model, create an inference session or mutate Harness-owned configuration. RedactGuard treats the resolved setup as compatibility/configuration evidence, not as final Analyze readiness; a fresh fail-closed preflight is repeated immediately before document content may enter inference.

RedactGuard never receives or chooses a concrete model digest, quantization, thread count, cache policy or residency setting. Harness remains the sole owner of exact execution resolution and activation-protected model residency. Consumer-safe model/configuration fields are displayed only when source-backed by the Host contract.

Control-plane failures remain typed through the RedactGuard boundary. Configuration-required conditions, model unavailable/conflict, invalid request, transport/runtime failure and true feature/capability incompatibility map to distinct app-owned analysis/product categories. `FEATURE_UNAVAILABLE` is the capability-incompatibility path; model/configuration/transient failures are not re-labelled as incompatible. The original `ConsumerControlPlaneErrorCode` is retained only as bounded `AnalysisRuntimeDiagnostic` metadata using `ControlPlane:<ENUM>`; free-form Host messages are discarded.

Consumer inference failures follow the same separation. `MODEL_UNAVAILABLE` retains a model-unavailable product meaning, capability incompatibility remains explicit, transport loss remains disconnected and connected runtime/session failures remain transient runtime/generation failures. The lower-level `ConsumerErrorCode` is retained only as bounded `Consumer:<ENUM>` diagnostic identity.

`RG_LOCAL_AI` emits only whitelisted technical identities: transport state, a bounded transport-detail classification, Control Plane step/result/reason and non-sensitive counts. It never logs document text, prompts, finding values, model output, raw Binder payloads, model paths/digests or arbitrary exception messages. Unknown free-form Binder detail is collapsed to `OTHER` rather than copied into logs. Diagnostic emission is observational and cannot replace an authoritative typed outcome.

Harness publishes the Consumer SDK in the public `consumer-sdk-maven` repository with ABI and publication evidence. Repository tests and emulator integration evidence do not substitute for final representative physical-device evidence where real ARM64/JNI/GGUF residency, memory, thermal or OEM behavior is material.
