package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerLocalLlmClient
import io.github.daniele21.localllm.transport.binder.client.BinderConsumerLocalLlmClient

/**
 * Compile-time proof that RedactGuard receives both the public Consumer API and Binder client from
 * the externally published `consumer-android` artifact rather than from a Harness source checkout.
 */
internal object ConsumerSdkLinkage {
    val publicClientType: Class<ConsumerLocalLlmClient> = ConsumerLocalLlmClient::class.java
    val binderClientType: Class<BinderConsumerLocalLlmClient> = BinderConsumerLocalLlmClient::class.java
}
