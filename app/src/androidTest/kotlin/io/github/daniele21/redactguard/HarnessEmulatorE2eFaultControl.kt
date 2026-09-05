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

    fun activityAuditStatus(context: Context): ActivityAuditStatus =
        parseActivityAuditStatus(
            command(context, ACTION_QUERY_ACTIVITY) { putExtra(EXTRA_VERIFIED_PACKAGE, BuildConfig.APPLICATION_ID) },
        )

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
        configure: Intent.() -> Unit = {},
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
            Intent(action)
                .setComponent(
                    ComponentName(
                        BuildConfig.SHARED_RUNTIME_HOST_PACKAGE,
                        HOST_FAULT_RECEIVER,
                    ),
                ).apply(configure)
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
        val values = parseValues(raw, "Harness emulator gate status")
        return GateStatus(
            paused = requireNotNull(values["paused"]) { "Missing paused gate status" }.toBooleanStrict(),
            waitingRequests = requireNotNull(values["waiting"]) { "Missing waiting gate status" }.toInt(),
        )
    }

    private fun parseActivityAuditStatus(raw: String): ActivityAuditStatus {
        val values = parseValues(raw, "Harness Activity audit status")
        val available = requireNotNull(values["available"]) { "Missing Activity availability" }.toBooleanStrict()
        val count = requireNotNull(values["count"]) { "Missing Activity record count" }.toInt()
        if (!available) {
            return ActivityAuditStatus(
                available = false,
                count = count,
                error = values["error"],
            )
        }
        return ActivityAuditStatus(
            available = true,
            count = count,
            identity =
                ActivityAuditIdentity(
                    requestId = requireNotNull(values["request_id"]) { "Missing Activity request ID" },
                    status = requireNotNull(values["status"]) { "Missing Activity status" },
                    applicationId = requireNotNull(values["application_id"]) { "Missing Activity application ID" },
                    useCaseId = requireNotNull(values["use_case_id"]) { "Missing Activity use-case ID" },
                    verifiedPackageName = requireNotNull(values["verified_package"]) { "Missing verified Activity package" },
                    terminalCode = requireNotNull(values["terminal_code"]) { "Missing Activity terminal code" },
                ),
            content =
                ActivityContentPresence(
                    input = requireNotNull(values["input_present"]) { "Missing Activity input presence" }.toBooleanStrict(),
                    effectivePrompt =
                        requireNotNull(values["effective_prompt_present"]) { "Missing effective prompt presence" }
                            .toBooleanStrict(),
                    answer = requireNotNull(values["answer_present"]) { "Missing Activity answer presence" }.toBooleanStrict(),
                    reasoning =
                        requireNotNull(values["reasoning_present"]) { "Missing Activity reasoning presence" }
                            .toBooleanStrict(),
                ),
            execution =
                ActivityExecutionPresence(
                    modelDigest =
                        requireNotNull(values["model_digest_present"]) { "Missing Activity model digest presence" }
                            .toBooleanStrict(),
                ),
            metrics =
                ActivityMetricsPresence(
                    totalMs = requireNotNull(values["total_ms_present"]) { "Missing Activity total-ms presence" }.toBooleanStrict(),
                    outputTokens =
                        requireNotNull(values["output_tokens_present"]) { "Missing Activity output-token presence" }
                            .toBooleanStrict(),
                    decodeTokensPerSecond =
                        requireNotNull(values["decode_tps_present"]) { "Missing Activity decode throughput presence" }
                            .toBooleanStrict(),
                ),
        )
    }

    private fun parseValues(
        raw: String,
        label: String,
    ): Map<String, String> =
        raw.split(';').associate { entry ->
            val parts = entry.split('=', limit = 2)
            require(parts.size == 2) { "Malformed $label" }
            parts[0] to parts[1]
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

    data class ActivityAuditStatus(
        val available: Boolean,
        val count: Int,
        val identity: ActivityAuditIdentity? = null,
        val content: ActivityContentPresence = ActivityContentPresence(),
        val execution: ActivityExecutionPresence = ActivityExecutionPresence(),
        val metrics: ActivityMetricsPresence = ActivityMetricsPresence(),
        val error: String? = null,
    )

    data class ActivityAuditIdentity(
        val requestId: String,
        val status: String,
        val applicationId: String,
        val useCaseId: String,
        val verifiedPackageName: String,
        val terminalCode: String,
    )

    data class ActivityContentPresence(
        val input: Boolean = false,
        val effectivePrompt: Boolean = false,
        val answer: Boolean = false,
        val reasoning: Boolean = false,
    )

    data class ActivityExecutionPresence(
        val modelDigest: Boolean = false,
    )

    data class ActivityMetricsPresence(
        val totalMs: Boolean = false,
        val outputTokens: Boolean = false,
        val decodeTokensPerSecond: Boolean = false,
    )

    private const val HOST_FAULT_RECEIVER = "io.github.daniele21.localllm.phonetest.EmulatorE2eFaultReceiver"
    private const val ACTION_PAUSE_GENERATION = "io.github.daniele21.localllm.phonetest.emulatorE2e.PAUSE_GENERATION"
    private const val ACTION_RELEASE_GENERATION = "io.github.daniele21.localllm.phonetest.emulatorE2e.RELEASE_GENERATION"
    private const val ACTION_RESET = "io.github.daniele21.localllm.phonetest.emulatorE2e.RESET"
    private const val ACTION_QUERY = "io.github.daniele21.localllm.phonetest.emulatorE2e.QUERY"
    private const val ACTION_QUERY_ACTIVITY = "io.github.daniele21.localllm.phonetest.emulatorE2e.QUERY_ACTIVITY"
    private const val EXTRA_VERIFIED_PACKAGE = "verified_package"
    private const val POLL_INTERVAL_MILLIS = 50L
    private const val DEFAULT_TIMEOUT_MILLIS = 8_000L
    private const val BROADCAST_TIMEOUT_SECONDS = 3L
}
