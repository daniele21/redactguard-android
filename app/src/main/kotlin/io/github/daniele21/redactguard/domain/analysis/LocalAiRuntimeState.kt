package io.github.daniele21.redactguard.domain.analysis

/** Consumer-safe local-AI readiness; transport connectivity alone never implies analysis readiness. */
internal enum class LocalAiRuntimeState {
    CONNECTED,
    CHECKING_CONFIGURATION,
    READY,
    CONFIGURATION_REQUIRED,
    CONNECTING,
    PERMISSION_DENIED,
    INCOMPATIBLE,
    HOST_NOT_INSTALLED,
    DISCONNECTED,
}
