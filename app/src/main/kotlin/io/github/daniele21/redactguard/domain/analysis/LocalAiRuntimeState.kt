package io.github.daniele21.redactguard.domain.analysis

internal enum class LocalAiRuntimeState {
    /** Binder transport is connected; Host assignment/preset readiness is not yet proven. */
    CONNECTED,

    /** Side-effect-free Host control-plane discovery is in progress. */
    CONFIGURING,

    /** Transport plus assigned use case and a valid Host-published preset have been discovered. */
    READY,

    CONNECTING,
    PERMISSION_DENIED,
    INCOMPATIBLE,
    HOST_NOT_INSTALLED,
    DISCONNECTED,
}
