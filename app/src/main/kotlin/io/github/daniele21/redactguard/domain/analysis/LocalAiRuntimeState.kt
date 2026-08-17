package io.github.daniele21.redactguard.domain.analysis

internal enum class LocalAiRuntimeState {
    CONNECTED,
    CONNECTING,
    PERMISSION_DENIED,
    INCOMPATIBLE,
    HOST_NOT_INSTALLED,
    DISCONNECTED,
}
