package io.github.daniele21.redactguard.domain.analysis

internal enum class LocalAiRuntimeState {
    /** Transport plus side-effect-free Host assignment/preset discovery are ready for analysis. */
    CONNECTED,
    CONNECTING,
    PERMISSION_DENIED,
    INCOMPATIBLE,
    HOST_NOT_INSTALLED,
    DISCONNECTED,
}
