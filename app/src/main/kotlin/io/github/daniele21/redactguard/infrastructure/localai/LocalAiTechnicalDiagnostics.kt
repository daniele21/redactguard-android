package io.github.daniele21.redactguard.infrastructure.localai

import android.util.Log
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionSnapshot

internal data class LocalAiTechnicalEvent(
    val step: String,
    val result: String,
    val reason: String? = null,
    val count: Int? = null,
) {
    init {
        require(SAFE_IDENTITY.matches(step)) { "Local AI diagnostic step contains unsupported characters" }
        require(SAFE_IDENTITY.matches(result)) { "Local AI diagnostic result contains unsupported characters" }
        require(reason == null || SAFE_IDENTITY.matches(reason)) { "Local AI diagnostic reason contains unsupported characters" }
        require(count == null || count >= 0) { "Local AI diagnostic count must be non-negative" }
    }

    fun render(): String =
        buildString {
            append("step=")
            append(step)
            append(" result=")
            append(result)
            reason?.let {
                append(" reason=")
                append(it)
            }
            count?.let {
                append(" count=")
                append(it)
            }
        }

    private companion object {
        val SAFE_IDENTITY = Regex("^[A-Za-z0-9._:+-]{1,96}$")
    }
}

internal fun interface LocalAiTechnicalDiagnostics {
    fun record(event: LocalAiTechnicalEvent)
}

internal object NoopLocalAiTechnicalDiagnostics : LocalAiTechnicalDiagnostics {
    override fun record(event: LocalAiTechnicalEvent) = Unit
}

internal object AndroidLocalAiTechnicalDiagnostics : LocalAiTechnicalDiagnostics {
    private const val TAG = "RG_LOCAL_AI"

    override fun record(event: LocalAiTechnicalEvent) {
        Log.i(TAG, event.render())
    }
}

internal fun SharedRuntimeConnectionSnapshot.toTechnicalEvent(): LocalAiTechnicalEvent =
    LocalAiTechnicalEvent(
        step = "transport",
        result = state.name,
        reason = detail.toSafeTransportDetail(),
    )

private fun String?.toSafeTransportDetail(): String =
    when (this) {
        null, "" -> "NONE"
        "Configured host service is not installed" -> "HOST_SERVICE_NOT_INSTALLED"
        "Host rejected the configured caller" -> "HOST_REJECTED_CALLER"
        "Android rejected the explicit host bind" -> "EXPLICIT_BIND_REJECTED"
        "Host Binder connection was lost" -> "BINDER_CONNECTION_LOST"
        "Host is disconnecting" -> "HOST_DISCONNECTING"
        else -> "OTHER"
    }
