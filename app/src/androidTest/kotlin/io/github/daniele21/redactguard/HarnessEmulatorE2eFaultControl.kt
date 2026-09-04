package io.github.daniele21.redactguard

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobId
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobResponse
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobClient
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.localllm.transport.binder.client.BinderConsumerLocalLlmClient
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Test-only bridge to the Harnex emulator fault surface and real Binder connection-loss path. */
internal object HarnessEmulatorE2eFaultControl {
    fun resetGenerationGate(context: Context) {
        command(context, ACTION_RESET)
    }

    fun pauseGeneration(context: Context) {
        val status = parseStatus(command(context, ACTION_PAUSE_GENERATION))
        check(status.paused) { "Harness emulator generation gate did not pause" }
    }

    fun releaseGeneration(context: Context) {
        val status = parseStatus(command(context, ACTION_RELEASE_GENERATION))
        check(!status.paused) { "Harness emulator generation gate did not release" }
    }

    fun generationGateStatus(context: Context): GateStatus = parseStatus(command(context, ACTION_QUERY))

    fun awaitGenerationBlocked(
        context: Context,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    ): GateStatus {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            val status = generationGateStatus(context)
            if (status.paused && status.waitingRequests > 0) return status
            SystemClock.sleep(POLL_INTERVAL_MILLIS)
        }
        throw AssertionError("Timed out waiting for Harness emulator generation gate")
    }

    fun acceptedLogicalJobId(owner: ProcessLocalProductAnalysisOwner): ConsumerInferenceJobId? {
        val consumerRuntime = owner.runtime.readField("consumerRuntime") ?: return null
        val operations = consumerRuntime.readField("operations") as? Map<*, *> ?: return null
        val accepted =
            operations.values.mapNotNull { operation ->
                val activeGeneration = operation?.readField("activeGeneration") ?: return@mapNotNull null
                when (val rawJobId = activeGeneration.readField("jobId")) {
                    is ConsumerInferenceJobId -> rawJobId
                    is String -> ConsumerInferenceJobId(rawJobId)
                    else -> null
                }
            }
        return accepted.singleOrNull()
    }

    fun logicalJobDiagnostic(
        owner: ProcessLocalProductAnalysisOwner,
        jobId: ConsumerInferenceJobId,
    ): String {
        val consumerRuntime =
            requireNotNull(owner.runtime.readField("consumerRuntime")) {
                "Consumer runtime reflection contract changed"
            }
        val logicalJobs =
            requireNotNull(consumerRuntime.readField("logicalJobs") as? ConsumerLogicalJobClient) {
                "Logical job client reflection contract changed"
            }
        val useCaseId =
            requireNotNull(
                when (val rawUseCaseId = consumerRuntime.readField("useCaseId")) {
                    is UseCaseId -> rawUseCaseId
                    is String -> UseCaseId(rawUseCaseId)
                    else -> null
                },
            ) {
                "Consumer use-case reflection contract changed"
            }
        return runCatching { logicalJobs.logicalJobResult(jobId, useCaseId) }
            .fold(
                onSuccess = { response ->
                    when (response) {
                        is ConsumerInferenceJobResponse.Available -> {
                            buildString {
                                append("response=AVAILABLE")
                                append(";job_id=${response.snapshot.jobId.value}")
                                append(";state=${response.snapshot.state.name}")
                                append(";revision=${response.snapshot.revision}")
                                append(";attempt=${response.snapshot.attempt}")
                                append(";runtime_session=${response.snapshot.runtimeSessionId.value}")
                                append(";result_available=${response.snapshot.resultAvailable}")
                                append(";error=${response.snapshot.errorCode?.name ?: "none"}")
                            }
                        }

                        is ConsumerInferenceJobResponse.Rejected -> {
                            "response=REJECTED;error=${response.failure.code.name}"
                        }
                    }
                },
                onFailure = { failure ->
                    "response=EXCEPTION;type=${failure.javaClass.simpleName};message=${failure.message.orEmpty()}"
                },
            )
    }

    fun consumerConnectionState(owner: ProcessLocalProductAnalysisOwner): SharedRuntimeConnectionState =
        binderClient(owner).connectionSnapshot.state

    fun consumerTransportDiagnostic(owner: ProcessLocalProductAnalysisOwner): String {
        val client = binderClient(owner)
        val connection = requireNotNull(client.readField("connection")) { "SharedRuntimeConnection reflection contract changed" }
        val epoch = connection.readField("connectionEpoch") as? Long
        return "state=${client.connectionSnapshot.state.name};epoch=${epoch ?: "unknown"}"
    }

    fun injectConsumerConnectionLoss(owner: ProcessLocalProductAnalysisOwner): SharedRuntimeConnectionState {
        val client = binderClient(owner)
        val connection = requireNotNull(client.readField("connection")) { "SharedRuntimeConnection reflection contract changed" }
        val epoch = requireNotNull(connection.readField("connectionEpoch") as? Long) { "Connection epoch reflection contract changed" }
        val method =
            connection.javaClass.getDeclaredMethod(
                "connectionLost",
                String::class.java,
                java.lang.Long.TYPE,
            )
        method.isAccessible = true
        method.invoke(connection, "Injected emulator E2E Binder disconnect", epoch)
        return client.connectionSnapshot.state
    }

    private fun binderClient(owner: ProcessLocalProductAnalysisOwner): BinderConsumerLocalLlmClient =
        requireNotNull(owner.runtime.readField("client") as? BinderConsumerLocalLlmClient) {
            "Binder consumer client reflection contract changed"
        }

    private fun command(
        context: Context,
        action: String,
    ): String {
        val latch = CountDownLatch(1)
        var response: String? = null
        val finalReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    response = resultData
                    latch.countDown()
                }
            }
        val intent =
            Intent(action).setComponent(
                ComponentName(
                    BuildConfig.SHARED_RUNTIME_HOST_PACKAGE,
                    HOST_FAULT_RECEIVER,
                ),
            )
        @Suppress("DEPRECATION")
        context.sendOrderedBroadcast(
            intent,
            null,
            finalReceiver,
            null,
            Activity.RESULT_CANCELED,
            null,
            null,
        )
        check(latch.await(BROADCAST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            "Harness emulator fault command timed out"
        }
        return requireNotNull(response) { "Harness emulator fault receiver returned no status" }
    }

    private fun parseStatus(raw: String): GateStatus {
        val values =
            raw.split(';').associate { entry ->
                val parts = entry.split('=', limit = 2)
                require(parts.size == 2) { "Malformed Harness emulator gate status" }
                parts[0] to parts[1]
            }
        return GateStatus(
            paused = requireNotNull(values["paused"]) { "Missing paused gate status" }.toBooleanStrict(),
            waitingRequests = requireNotNull(values["waiting"]) { "Missing waiting gate status" }.toInt(),
        )
    }

    private fun Any.readField(name: String): Any? {
        var type: Class<*>? = javaClass
        while (type != null) {
            val currentType = type
            val field = runCatching { currentType.getDeclaredField(name) }.getOrNull()
            if (field != null) {
                field.isAccessible = true
                return field.get(this)
            }
            type = currentType.superclass
        }
        return null
    }

    data class GateStatus(
        val paused: Boolean,
        val waitingRequests: Int,
    )

    private const val HOST_FAULT_RECEIVER = "io.github.daniele21.localllm.phonetest.EmulatorE2eFaultReceiver"
    private const val ACTION_PAUSE_GENERATION = "io.github.daniele21.localllm.phonetest.emulatorE2e.PAUSE_GENERATION"
    private const val ACTION_RELEASE_GENERATION = "io.github.daniele21.localllm.phonetest.emulatorE2e.RELEASE_GENERATION"
    private const val ACTION_RESET = "io.github.daniele21.localllm.phonetest.emulatorE2e.RESET"
    private const val ACTION_QUERY = "io.github.daniele21.localllm.phonetest.emulatorE2e.QUERY"
    private const val POLL_INTERVAL_MILLIS = 50L
    private const val DEFAULT_TIMEOUT_MILLIS = 8_000L
    private const val BROADCAST_TIMEOUT_SECONDS = 3L
}
