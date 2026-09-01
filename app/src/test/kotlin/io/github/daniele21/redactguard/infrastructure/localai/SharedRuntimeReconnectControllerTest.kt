package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedRuntimeReconnectControllerTest {
    @Test
    fun `connection loss schedules one bounded reconnect with exponential backoff`() {
        var state = SharedRuntimeConnectionState.CONNECTION_LOST
        var connectCalls = 0
        val scheduler = FakeScheduler()
        val controller =
            SharedRuntimeReconnectController(
                currentState = { state },
                connect = { connectCalls += 1 },
                schedule = scheduler::schedule,
                maxAttempts = 3,
                initialDelayMillis = 250L,
                maxDelayMillis = 1_000L,
            )
        controller.enable()

        controller.onStateChanged(SharedRuntimeConnectionState.CONNECTION_LOST)
        controller.onStateChanged(SharedRuntimeConnectionState.CONNECTION_LOST)

        assertEquals(listOf(250L), scheduler.delays())
        scheduler.runNext()
        assertEquals(1, connectCalls)
        assertEquals(listOf(500L), scheduler.delays())
        scheduler.runNext()
        assertEquals(2, connectCalls)
        assertEquals(listOf(1_000L), scheduler.delays())
        scheduler.runNext()
        assertEquals(3, connectCalls)
        assertTrue(scheduler.isEmpty())

        state = SharedRuntimeConnectionState.CONNECTED
        controller.onStateChanged(state)
        state = SharedRuntimeConnectionState.CONNECTION_LOST
        controller.onStateChanged(state)
        assertEquals(listOf(250L), scheduler.delays())
    }

    @Test
    fun `connected state invalidates stale scheduled reconnect`() {
        var state = SharedRuntimeConnectionState.CONNECTION_LOST
        var connectCalls = 0
        val scheduler = FakeScheduler()
        val controller =
            SharedRuntimeReconnectController(
                currentState = { state },
                connect = { connectCalls += 1 },
                schedule = scheduler::schedule,
            )
        controller.enable()
        controller.onStateChanged(state)

        state = SharedRuntimeConnectionState.CONNECTED
        controller.onStateChanged(state)
        scheduler.runNext()

        assertEquals(0, connectCalls)
    }

    @Test
    fun `terminal transport states do not reconnect automatically`() {
        var state = SharedRuntimeConnectionState.CONNECTION_LOST
        var connectCalls = 0
        val scheduler = FakeScheduler()
        val controller =
            SharedRuntimeReconnectController(
                currentState = { state },
                connect = { connectCalls += 1 },
                schedule = scheduler::schedule,
            )
        controller.enable()
        controller.onStateChanged(state)

        state = SharedRuntimeConnectionState.PERMISSION_DENIED
        controller.onStateChanged(state)
        scheduler.runNext()
        controller.onStateChanged(SharedRuntimeConnectionState.HOST_NOT_INSTALLED)
        controller.onStateChanged(SharedRuntimeConnectionState.INCOMPATIBLE)

        assertEquals(0, connectCalls)
        assertTrue(scheduler.isEmpty())
    }

    @Test
    fun `synchronous transient failure retries but security failure stops the chain`() {
        var state = SharedRuntimeConnectionState.DISCONNECTED
        var connectCalls = 0
        val scheduler = FakeScheduler()
        var failure: RuntimeException? = IllegalStateException("transient")
        val controller =
            SharedRuntimeReconnectController(
                currentState = { state },
                connect = {
                    connectCalls += 1
                    failure?.let { throw it }
                },
                schedule = scheduler::schedule,
                maxAttempts = 4,
            )
        controller.enable()
        controller.onConnectFailure(IllegalStateException("initial transient"))

        scheduler.runNext()
        assertEquals(1, connectCalls)
        assertEquals(1, scheduler.size())

        failure = SecurityException("denied")
        scheduler.runNext()
        assertEquals(2, connectCalls)
        assertTrue(scheduler.isEmpty())

        state = SharedRuntimeConnectionState.PERMISSION_DENIED
        controller.onStateChanged(state)
    }

    @Test
    fun `close invalidates pending reconnect`() {
        var connectCalls = 0
        val scheduler = FakeScheduler()
        val controller =
            SharedRuntimeReconnectController(
                currentState = { SharedRuntimeConnectionState.CONNECTION_LOST },
                connect = { connectCalls += 1 },
                schedule = scheduler::schedule,
            )
        controller.enable()
        controller.onStateChanged(SharedRuntimeConnectionState.CONNECTION_LOST)

        controller.close()
        scheduler.runNext()

        assertEquals(0, connectCalls)
    }

    private class FakeScheduler {
        private val tasks = ArrayDeque<ScheduledTask>()

        fun schedule(delayMillis: Long, task: () -> Unit) {
            tasks.addLast(ScheduledTask(delayMillis, task))
        }

        fun delays(): List<Long> = tasks.map(ScheduledTask::delayMillis)

        fun size(): Int = tasks.size

        fun isEmpty(): Boolean = tasks.isEmpty()

        fun runNext() {
            tasks.removeFirst().task()
        }

        private data class ScheduledTask(val delayMillis: Long, val task: () -> Unit)
    }
}
