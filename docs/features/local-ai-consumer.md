# Local AI consumer boundary

Status: active
Owner: RedactGuard

RedactGuard consumes Local AI only through the externally published Harness Consumer Android SDK. The pinned cross-repository artifact is:

`io.github.daniele21.localllm:consumer-android:0.1.0-alpha.4`

The primary public repository is the token-free Maven tree published by Harness at the dedicated `consumer-sdk-maven` branch and served through the public raw GitHub endpoint. RedactGuard requires no package credential, personal access token, Harness source checkout, composite build, git submodule or copied Binder client to resolve the SDK. Harness may retain GitHub Packages as an authenticated/internal secondary channel.

`consumer-android` alpha.4 exposes both the public Consumer inference API and the Consumer Control Plane API, and supplies `BinderConsumerLocalLlmClient`. Runtime/model/llama.cpp modules are not RedactGuard dependencies. Alpha.3 predates the required control-plane surface and is intentionally no longer the RedactGuard pin for this cutover.

Before requesting Consumer capabilities for `document-pii-detection`, RedactGuard follows the Host-owned control-plane lifecycle: discover its assigned use case, discover published presets, select only an advertised preset (Host default when no product selection exists), activate the exact use-case/binding/preset revisions, then run capability preparation/session/generation. The activation is retained across all chunks of one document analysis and released on success, failure, cancellation or explicit close.

RedactGuard never receives or chooses a concrete model digest, quantization, context, thread count, cache policy or residency setting. Harness remains the sole owner of exact execution resolution and activation-protected model residency.

Control-plane transport failure maps to a disconnected analysis state; missing/incompatible assignments or presets fail closed as capability incompatibility; model/configuration/conflict conditions map to Host unavailability. Binder/process death remains safe because the Host owns connection-death activation cleanup in addition to RedactGuard's explicit deactivation path.

Harness publishes alpha.4 in the public `consumer-sdk-maven` repository with ABI evidence that contains the control-plane contracts. The Harness publication workflow validates manifest/checksum/public ABI identity and proves unauthenticated external compilation. Repository tests do not substitute for the final same-signer two-APK physical evidence.
