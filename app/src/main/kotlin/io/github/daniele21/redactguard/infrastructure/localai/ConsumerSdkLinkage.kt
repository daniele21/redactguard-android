package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerControlPlaneClient
import io.github.daniele21.localllm.contracts.ConsumerLocalLlmClient
import io.github.daniele21.localllm.transport.binder.client.BinderConsumerLocalLlmClient

/**
 * Compile-time proof that RedactGuard receives the public Consumer inference/control-plane API and
 * Binder client from the externally published `consumer-android` artifact rather than from a
 * Harness source checkout.
 */
internal object ConsumerSdkLinkage {
    val publicClientType: Class<ConsumerLocalLlmClient> = ConsumerLocalLlmClient::class.java
    val controlPlaneClientType: Class<ConsumerControlPlaneClient> = ConsumerControlPlaneClient::class.java
    val binderClientType: Class<BinderConsumerLocalLlmClient> = BinderConsumerLocalLlmClient::class.java
}
