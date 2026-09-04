package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionState

/** Bounded process-local reconnect policy for an already-enabled Harness Binder transport. */
internal class SharedRuntimeReconnectController(
    private val currentState: () -> SharedRuntimeConnectionState,
    private val connect: () -> Unit,
    private val schedule: (delayMillis: Long, task: () -> Unit) -> Unit,
    private val onSynchronousFailure: (RuntimeException) -> Unit = {},
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val initialDelayMillis: Long = DEFAULT_INITIAL_DELAY_MILLIS,
    private val maxDelayMillis: Long = DEFAULT_MAX_DELAY_MILLIS,
) : AutoCloseable {
    private val lock = Any()
    private var enabled = false
    private var closed = false
    private var attempts = 0
    private var pending = false
    private var generation = 0L

    init {
        require(maxAttempts > 0) { "Reconnect attempts must be positive" }
        require(initialDelayMillis > 0L) { "Initial reconnect delay must be positive" }
        require(maxDelayMillis >= initialDelayMillis) { "Maximum reconnect delay must cover the initial delay" }
    }

    fun enable() {
        synchronized(lock) {
            if (!closed) enabled = true
        }
    }

    fun onStateChanged(state: SharedRuntimeConnectionState) {
        when (state) {
            SharedRuntimeConnectionState.CONNECTED -> resetBackoff()

            SharedRuntimeConnectionState.CONNECTION_LOST -> scheduleNext()

            SharedRuntimeConnectionState.HOST_NOT_INSTALLED,
            SharedRuntimeConnectionState.PERMISSION_DENIED,
            SharedRuntimeConnectionState.INCOMPATIBLE,
            SharedRuntimeConnectionState.CLOSED,
            -> invalidatePending()

            SharedRuntimeConnectionState.DISCONNECTED,
            SharedRuntimeConnectionState.BINDING,
            SharedRuntimeConnectionState.NEGOTIATING,
            -> Unit
        }
    }

    fun onConnectFailure(error: RuntimeException) {
        if (error is SecurityException) {
            invalidatePending()
        } else {
            scheduleNext()
        }
    }

    override fun close() {
        synchronized(lock) {
            closed = true
            enabled = false
            generation += 1
            pending = false
        }
    }

    private fun resetBackoff() {
        synchronized(lock) {
            attempts = 0
            generation += 1
            pending = false
        }
    }

    private fun invalidatePending() {
        synchronized(lock) {
            generation += 1
            pending = false
        }
    }

    private fun scheduleNext() {
        val scheduled =
            synchronized(lock) {
                if (!enabled || closed || pending || attempts >= maxAttempts) return
                val attemptNumber = attempts + 1
                val delayMillis = delayFor(attemptNumber)
                pending = true
                generation += 1
                ScheduledAttempt(generation, attemptNumber, delayMillis)
            }
        schedule(scheduled.delayMillis) { runScheduled(scheduled) }
    }

    private fun runScheduled(scheduled: ScheduledAttempt) {
        val shouldRun =
            synchronized(lock) {
                val current =
                    enabled &&
                        !closed &&
                        pending &&
                        generation == scheduled.generation
                if (current) {
                    pending = false
                    attempts = scheduled.attemptNumber
                }
                current
            }
        if (!shouldRun) return

        try {
            connect()
        } catch (error: RuntimeException) {
            onSynchronousFailure(error)
            onConnectFailure(error)
            return
        }

        if (currentState() in RETRYABLE_IDLE_STATES) scheduleNext()
    }

    private fun delayFor(attemptNumber: Int): Long {
        var delay = initialDelayMillis
        repeat(attemptNumber - 1) {
            delay = (delay * 2L).coerceAtMost(maxDelayMillis)
        }
        return delay
    }

    private data class ScheduledAttempt(
        val generation: Long,
        val attemptNumber: Int,
        val delayMillis: Long,
    )

    private companion object {
        const val DEFAULT_MAX_ATTEMPTS = 5
        const val DEFAULT_INITIAL_DELAY_MILLIS = 250L
        const val DEFAULT_MAX_DELAY_MILLIS = 4_000L

        val RETRYABLE_IDLE_STATES =
            setOf(
                SharedRuntimeConnectionState.DISCONNECTED,
                SharedRuntimeConnectionState.CONNECTION_LOST,
            )
    }
}
