package io.github.daniele21.redactguard

import android.app.Application
import android.app.Instrumentation
import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobId
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobSnapshot
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobState
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobSubscription
import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisFailureCode
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import io.github.daniele21.redactguard.ui.ProductRetryTarget
import io.github.daniele21.redactguard.ui.ProductStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class TwoApkCriticalMemoryPressureE2eTest {
    @Test
    fun activeLogicalJobFailsAsRuntimePressureAndCleansHostExecution() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ProcessLocalProductAnalysisOwner.get(application)
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        val fault = HarnessEmulatorE2eFaultControl
        val directory =
            criticalPressureEvidenceDirectory(application).apply {
                deleteRecursively()
                mkdirs()
            }
        val timeline = File(directory, "critical-pressure-timeline.txt")
        val observedTerminal = AtomicReference<AnalysisJobSnapshot?>()
        val terminalHistory = CopyOnWriteArrayList<String>()
        var terminalSubscription: AnalysisJobSubscription? = null

        fault.resetGenerationGate(application)
        fault.pauseGeneration(application)
        viewModel.connectHarness()
        try {
            await("initial Harness readiness", READY_TIMEOUT_MS) {
                viewModel.uiState.value.connection.analysisReady
            }
            recordTrace(
                timeline,
                "initial-ready",
                "transport=${safeTransportDiagnostic(fault, owner)};ui=${uiDiagnostic(viewModel)};${processDiagnostic()}",
            )
            prepareSyntheticAnalysis(viewModel)
            viewModel.startAnalysis()

            val productJob =
                awaitValue("accepted product analysis job") {
                    owner.currentSnapshot()?.takeIf { snapshot -> snapshot.state == AnalysisJobState.ACTIVE }
                }
            terminalSubscription =
                owner.observe(productJob.jobId) { snapshot ->
                    terminalHistory += terminalSnapshotDiagnostic(snapshot)
                    if (snapshot.isTerminal) observedTerminal.compareAndSet(null, snapshot)
                }
            val blocked = fault.awaitGenerationBlocked(application)
            val logicalJobId =
                awaitValue("accepted Harness logical job") {
                    fault.acceptedLogicalJobId(owner)
                }
            assertTrue(blocked.paused)
            assertTrue(blocked.waitingRequests > 0)
            recordTrace(
                timeline,
                "logical-job-accepted",
                "analysis_job_id=${productJob.jobId.value};logical_job_id=${logicalJobId.value};" +
                    "gate=${safeGenerationGateDiagnostic(fault, application)};" +
                    "transport=${safeTransportDiagnostic(fault, owner)};" +
                    "logical_job=${safeLogicalJobDiagnostic(fault, owner, logicalJobId)};" +
                    "product=${productJobDiagnostic(owner)};" +
                    "terminal_history=${terminalHistoryDiagnostic(terminalHistory)};" +
                    "ui=${uiDiagnostic(viewModel)};${processDiagnostic()}",
            )
            recordHostStoreTrace(timeline, "durable-pre-pressure", logicalJobId)

            val trimOutput =
                shell("am send-trim-memory ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE} RUNNING_CRITICAL")
                    .lineSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .joinToString("|")
                    .ifBlank { "none" }
            recordTrace(
                timeline,
                "pressure-command-sent",
                "shell_output=$trimOutput;gate=${safeGenerationGateDiagnostic(fault, application)};" +
                    "transport=${safeTransportDiagnostic(fault, owner)};" +
                    "logical_job=${safeLogicalJobDiagnostic(fault, owner, logicalJobId)};" +
                    "product=${productJobDiagnostic(owner)};" +
                    "terminal_history=${terminalHistoryDiagnostic(terminalHistory)};" +
                    "ui=${uiDiagnostic(viewModel)};${processDiagnostic()}",
            )
            recordHostStoreTrace(timeline, "durable-post-pressure-immediate", logicalJobId)

            val failed =
                awaitStructuredCriticalPressureFailure(
                    owner = owner,
                    viewModel = viewModel,
                    observedTerminal = observedTerminal,
                    terminalHistory = terminalHistory,
                    fault = fault,
                    logicalJobId = logicalJobId,
                    timeline = timeline,
                )
            assertEquals(productJob.jobId, failed.jobId)
            assertEquals(DocumentAnalysisFailureCode.CHUNK_FAILED, failed.failureCode)
            recordTrace(
                timeline,
                "structured-critical-pressure-failure",
                "gate=${safeGenerationGateDiagnostic(fault, application)};" +
                    "transport=${safeTransportDiagnostic(fault, owner)};" +
                    "logical_job=${safeLogicalJobDiagnostic(fault, owner, logicalJobId)};" +
                    "product=${productJobDiagnostic(owner)};" +
                    "terminal_history=${terminalHistoryDiagnostic(terminalHistory)};" +
                    "ui=${uiDiagnostic(viewModel)};${processDiagnostic()}",
            )
            recordHostStoreTrace(timeline, "durable-terminal", logicalJobId)

            await("critical-pressure recovery UI", DEFAULT_TIMEOUT_MS) {
                viewModel.uiState.value.step == ProductStep.ERROR
            }
            val productError = requireNotNull(viewModel.uiState.value.error)
            assertEquals(ProductFailureKind.CHUNK_FAILED.name, productError.technicalDetails.cause)
            assertEquals(ProductRetryTarget.ANALYSIS, productError.retryTarget)

            val cleaned =
                awaitValue("Host generation waiter cleanup", HOST_RECOVERY_TIMEOUT_MS) {
                    runCatching { fault.generationGateStatus(application) }
                        .getOrNull()
                        ?.takeIf { status -> status.waitingRequests == 0 }
                }
            await("Harness readiness after critical pressure", HOST_RECOVERY_TIMEOUT_MS) {
                viewModel.uiState.value.connection.analysisReady
            }
            recordTrace(
                timeline,
                "host-cleaned-ready",
                "gate=${safeGenerationGateDiagnostic(fault, application)};" +
                    "transport=${safeTransportDiagnostic(fault, owner)};" +
                    "logical_job=${safeLogicalJobDiagnostic(fault, owner, logicalJobId)};" +
                    "product=${productJobDiagnostic(owner)};" +
                    "terminal_history=${terminalHistoryDiagnostic(terminalHistory)};" +
                    "ui=${uiDiagnostic(viewModel)};${processDiagnostic()}",
            )

            File(directory, "critical-pressure-identity.txt").writeText(
                buildString {
                    appendLine("analysis_job_id=${productJob.jobId.value}")
                    appendLine("logical_job_id=${logicalJobId.value}")
                    appendLine("pre_pressure_state=${productJob.state.name}")
                    appendLine("post_pressure_state=${failed.state.name}")
                    appendLine("failure_code=${failed.failureCode?.name}")
                    appendLine("product_failure=${productError.technicalDetails.cause}")
                    appendLine("terminal_history=${terminalHistoryDiagnostic(terminalHistory)}")
                    appendLine("host_waiters_after_pressure=${cleaned.waitingRequests}")
                    appendLine("host_gate_paused_after_recovery=${cleaned.paused}")
                    appendLine("host_process_survival_claimed=false")
                    appendLine("explicit_user_cancel=false")
                    appendLine("physical_memory_pressure_claimed=false")
                },
            )
        } finally {
            terminalSubscription?.close()
            runCatching { fault.releaseGeneration(application) }
            runCatching { fault.resetGenerationGate(application) }
            store.clear()
        }
    }

    private fun awaitStructuredCriticalPressureFailure(
        owner: ProcessLocalProductAnalysisOwner,
        viewModel: RedactGuardProductViewModel,
        observedTerminal: AtomicReference<AnalysisJobSnapshot?>,
        terminalHistory: List<String>,
        fault: HarnessEmulatorE2eFaultControl,
        logicalJobId: ConsumerInferenceJobId,
        timeline: File,
    ): AnalysisJobSnapshot {
        val deadline = SystemClock.elapsedRealtime() + ANALYSIS_TIMEOUT_MS
        var lastDiagnostic: String? = null
        while (SystemClock.elapsedRealtime() < deadline) {
            observedTerminal.get()?.let { terminal ->
                if (
                    terminal.state == AnalysisJobState.FAILED &&
                    terminal.failureCode == DocumentAnalysisFailureCode.CHUNK_FAILED
                ) {
                    return terminal
                }
                val history = terminalHistoryDiagnostic(terminalHistory)
                val ui = uiDiagnostic(viewModel)
                recordTrace(
                    timeline,
                    "unexpected-terminal",
                    "terminal=${terminalSnapshotDiagnostic(terminal)};terminal_history=$history;ui=$ui;" +
                        "transport=${safeTransportDiagnostic(fault, owner)};${processDiagnostic()}",
                )
                throw AssertionError(
                    "Unexpected product terminal while waiting for critical-pressure failure; " +
                        "terminal=${terminalSnapshotDiagnostic(terminal)}; terminal_history=$history; ui=$ui",
                )
            }

            val diagnostic =
                "gate=${safeGenerationGateDiagnostic(fault, viewModel.getApplication())};" +
                    "transport=${safeTransportDiagnostic(fault, owner)};" +
                    "logical_job=${safeLogicalJobDiagnostic(fault, owner, logicalJobId)};" +
                    "product=${productJobDiagnostic(owner)};" +
                    "terminal_history=${terminalHistoryDiagnostic(terminalHistory)};" +
                    "ui=${uiDiagnostic(viewModel)};${processDiagnostic()}"
            if (diagnostic != lastDiagnostic) {
                recordTrace(timeline, "post-pressure-poll", diagnostic)
                lastDiagnostic = diagnostic
            }
            SystemClock.sleep(DIAGNOSTIC_POLL_INTERVAL_MS)
        }

        val application = viewModel.getApplication<Application>()
        val logicalDiagnostic = safeLogicalJobDiagnostic(fault, owner, logicalJobId)
        val durableDiagnostic = hostLogicalJobStoreSnapshot(logicalJobId)
        val productDiagnostic = productJobDiagnostic(owner)
        val history = terminalHistoryDiagnostic(terminalHistory)
        val gateDiagnostic = safeGenerationGateDiagnostic(fault, application)
        val transportDiagnostic = safeTransportDiagnostic(fault, owner)
        val ui = uiDiagnostic(viewModel)
        recordTrace(
            timeline,
            "timeout",
            "gate=$gateDiagnostic;transport=$transportDiagnostic;logical_job=$logicalDiagnostic;" +
                "product=$productDiagnostic;terminal_history=$history;ui=$ui;" +
                "host_store=$durableDiagnostic;${processDiagnostic()}",
        )
        throw AssertionError(
            "Timed out waiting for structured critical-pressure failure; gate=$gateDiagnostic; " +
                "transport=$transportDiagnostic; logical_job=$logicalDiagnostic; product=$productDiagnostic; " +
                "terminal_history=$history; ui=$ui; host_store=$durableDiagnostic",
        )
    }

    private fun createViewModel(
        store: ViewModelStore,
        application: Application,
    ): RedactGuardProductViewModel =
        ViewModelProvider(
            store,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application),
        )[RedactGuardProductViewModel::class.java]

    private fun prepareSyntheticAnalysis(viewModel: RedactGuardProductViewModel) {
        viewModel.importText(
            "Ada Lovelace lives at 1 Test Street. Contact ada@example.test for this pressure fixture.",
        )
        await("pasted text definitions") {
            viewModel.uiState.value.step == ProductStep.DEFINITIONS &&
                viewModel.uiState.value.definitions
                    .isNotEmpty()
        }
        val firstChoice =
            viewModel.uiState.value.definitions
                .first()
        if (!firstChoice.selected) viewModel.toggleDefinition(firstChoice.id)
        assertTrue(
            viewModel.uiState.value.definitions
                .any { it.selected },
        )
        assertTrue(viewModel.uiState.value.connection.analysisReady)
    }

    private fun productJobDiagnostic(owner: ProcessLocalProductAnalysisOwner): String =
        owner.currentSnapshot()?.let { snapshot ->
            "job_id=${snapshot.jobId.value},state=${snapshot.state.name},failure=${snapshot.failureCode?.name ?: "none"}"
        } ?: "none"

    private fun terminalSnapshotDiagnostic(snapshot: AnalysisJobSnapshot): String =
        "state=${snapshot.state.name},failure=${snapshot.failureCode?.name ?: "none"},revision=${snapshot.revision}"

    private fun terminalHistoryDiagnostic(history: List<String>): String =
        if (history.isEmpty()) "none" else history.joinToString(separator = ">")

    private fun uiDiagnostic(viewModel: RedactGuardProductViewModel): String {
        val state = viewModel.uiState.value
        val error = state.error
        return "step=${state.step.name},failure=${error?.technicalDetails?.cause ?: "none"}," +
            "retry=${error?.retryTarget?.name ?: "none"}"
    }

    private fun safeLogicalJobDiagnostic(
        fault: HarnessEmulatorE2eFaultControl,
        owner: ProcessLocalProductAnalysisOwner,
        logicalJobId: ConsumerInferenceJobId,
    ): String =
        runCatching { fault.logicalJobDiagnostic(owner, logicalJobId) }
            .getOrElse { failure -> "diagnostic_exception=${failure.javaClass.simpleName}" }

    private fun safeTransportDiagnostic(
        fault: HarnessEmulatorE2eFaultControl,
        owner: ProcessLocalProductAnalysisOwner,
    ): String =
        runCatching { fault.consumerTransportDiagnostic(owner) }
            .getOrElse { failure -> "diagnostic_exception=${failure.javaClass.simpleName}" }

    private fun safeGenerationGateDiagnostic(
        fault: HarnessEmulatorE2eFaultControl,
        application: Application,
    ): String =
        runCatching { fault.generationGateStatus(application) }
            .fold(
                onSuccess = { status -> "paused=${status.paused},waiting_requests=${status.waitingRequests}" },
                onFailure = { failure -> "diagnostic_exception=${failure.javaClass.simpleName}" },
            )

    private fun recordHostStoreTrace(
        timeline: File,
        stage: String,
        logicalJobId: ConsumerInferenceJobId,
    ) {
        recordTrace(timeline, stage, "host_store=${hostLogicalJobStoreSnapshot(logicalJobId)};${processDiagnostic()}")
    }

    private fun hostLogicalJobStoreSnapshot(logicalJobId: ConsumerInferenceJobId): String {
        val raw =
            runCatching {
                shell(
                    "run-as ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE} " +
                        "cat shared_prefs/$HOST_LOGICAL_JOB_PREFERENCES.xml",
                )
            }.getOrElse { failure ->
                return "read_exception=${failure.javaClass.simpleName}"
            }
        val matching =
            raw
                .lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .filter { line -> logicalJobId.value in line }
                .toList()
        if (matching.isNotEmpty()) return matching.joinToString("|")
        val compactRaw =
            raw
                .lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .joinToString("|")
        return if (compactRaw.isBlank()) "empty" else "job_not_found:$compactRaw"
    }

    private fun processDiagnostic(): String {
        val hostPid = runCatching { shell("pidof ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}").trim() }.getOrDefault("")
        return "redactguard_pid=${android.os.Process.myPid()};host_pid=${hostPid.ifBlank { "none" }}"
    }

    private fun recordTrace(
        timeline: File,
        stage: String,
        details: String,
    ) {
        val line = "t_ms=${SystemClock.elapsedRealtime()};stage=$stage;$details"
        val rendered = "RG_CRITICAL_PRESSURE_TRACE $line"
        timeline.appendText("$line\n")
        println(rendered)
        InstrumentationRegistry.getInstrumentation().sendStatus(
            0,
            Bundle().apply { putString(Instrumentation.REPORT_KEY_STREAMRESULT, "$rendered\n") },
        )
    }

    private fun criticalPressureEvidenceDirectory(application: Application): File =
        File(requireNotNull(application.getExternalFilesDir(null)), "two-apk-critical-pressure")

    private fun await(
        label: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError("Timed out waiting for $label")
    }

    private fun <T : Any> awaitValue(
        label: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        producer: () -> T?,
    ): T {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            producer()?.let { return it }
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError("Timed out waiting for $label")
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return descriptor.use { pfd ->
            FileInputStream(pfd.fileDescriptor).bufferedReader().use { it.readText() }
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 50L
        const val DIAGNOSTIC_POLL_INTERVAL_MS = 500L
        const val DEFAULT_TIMEOUT_MS = 8_000L
        const val READY_TIMEOUT_MS = 15_000L
        const val HOST_RECOVERY_TIMEOUT_MS = 20_000L
        const val ANALYSIS_TIMEOUT_MS = 30_000L
        const val HOST_LOGICAL_JOB_PREFERENCES = "harnex_consumer_logical_jobs_v1"
    }
}
