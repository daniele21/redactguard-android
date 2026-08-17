# Local AI consumer boundary

Status: integration in progress
Owner: RedactGuard

RedactGuard consumes Local AI only through the externally published Harness Consumer Android SDK. The first pinned cross-repository artifact is:

`io.github.daniele21.localllm:consumer-android:0.1.0-alpha.1`

The primary public repository is the token-free Maven tree published by Harness at the dedicated `consumer-sdk-maven` branch and served through the public raw GitHub endpoint. RedactGuard requires no package credential, personal access token, Harness source checkout, composite build, git submodule or copied Binder client to resolve the SDK. Harness may retain GitHub Packages as an authenticated/internal secondary channel.

`consumer-android` exposes the public Consumer API transitively and supplies `BinderConsumerLocalLlmClient`. Runtime/model/llama.cpp modules are not RedactGuard dependencies.

The Harness publication workflow must validate manifest/checksum/public ABI identity and prove unauthenticated external compilation before a version is considered available to RedactGuard.

This RG-6 slice is deliberately compile/linkage-only. Binding, capability negotiation, `redactguard` registration, `document-pii-detection`, session lifecycle, sequential generation, cancellation and disconnect handling remain application integration/evidence work and must be tested separately.
