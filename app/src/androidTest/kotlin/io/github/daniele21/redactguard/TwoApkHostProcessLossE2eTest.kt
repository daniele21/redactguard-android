package io.github.daniele21.redactguard

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobId
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobState
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

@RunWith(AndroidJUnit4::class)
class TwoApkHostProcessLossE2eTest {
    @Test
    fun activeLogicalJobBecomesStructuredHostProcessLossAfterHostRestart() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ProcessLocalProductAnalysisOwner.get(application)
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        val fault = HarnessEmulatorE2eFaultControl
        val directory = hostProcessLossEvidenceDirectory(application).apply {
            deleteRecursively()
            mkdirs()
        }
        val timeline = File(directory, "host-process-loss-timeline.txt")

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
                "transport=${safeTransportDiagnostic(fault, owner)};${processDiagnostic()}",
            )
            prepareSyntheticAnalysis(viewModel)
            viewModel.startAnalysis()

            val productJob =
                awaitValue("accepted product analysis job") {
                    owner.currentSnapshot()?.takeIf { snapshot -> snapshot.state == AnalysisJobState.ACTIVE }
                }
            val gateStatus = fault.awaitGenerationBlocked(application)
            val logicalJobId =
                awaitValue("accepted Harness logical job") {
                    fault.acceptedLogicalJobId(owner)
                }
            assertTrue(gateStatus.paused)
            assertTrue(gateStatus.waitingRequests > 0)
            recordTrace(
                timeline,
                "logical-job-accepted",
                "analysis_job_id=${productJob.jobId.value};logical_job_id=${logicalJobId.value};" +
                    "transport=${safeTransportDiagnostic(fault, owner)};" +
                    "logical_job=${safeLogicalJobDiagnostic(fault, owner, logicalJobId)};${processDiagnostic()}",
            )
            recordHostStoreTrace(timeline, "durable-pre-kill", logicalJobId)

            shell("am force-stop ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}")
            await("Host process disconnect", READY_TIMEOUT_MS) {
                !viewModel.uiState.value.connection.analysisReady
            }
            recordTrace(
                timeline,
                "host-force-stopped",
                "transport=${safeTransportDiagnostic(fault, owner)};product=${productJobDiagnostic(owner)};${processDiagnostic()}",
            )
            recordHostStoreTrace(timeline, "durable-post-kill", logicalJobId)

            shell(
                "am start -W -n ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}/" +
                    "io.github.daniele21.localllm.phonetest.MainActivity",
            )
            viewModel.connectHarness()
            await("Harness reconnect after process restart", READY_TIMEOUT_MS) {
                viewModel.uiState.value.connection.analysisReady
            }
            recordTrace(
                timeline,
                "host-reconnected-ready",
                "transport=${safeTransportDiagnostic(fault, owner)};" +
                    "logical_job=${safeLogicalJobDiagnostic(fault, owner, logicalJobId)};" +
                    "product=${productJobDiagnostic(owner)};${processDiagnostic()}",
            )
            recordHostStoreTrace(timeline, "durable-post-reconnect", logicalJobId)

            val interrupted =
                awaitStructuredHostProcessLoss(
                    owner = owner,
                    fault = fault,
                    logicalJobId = logicalJobId,
                    timeline = timeline,
                )
            assertEquals(productJob.jobId, interrupted.jobId)
            assertEquals(DocumentAnalysisFailureCode.HOST_PROCESS_LOST, interrupted.failureCode)

            await("Host-process-loss recovery UI", DEFAULT_TIMEOUT_MS) {
                viewModel.uiState.value.step == ProductStep.ERROR
            }
            val productError = requireNotNull(viewModel.uiState.value.error)
            assertEquals(ProductFailureKind.HOST_PROCESS_LOST.name, productError.technicalDetails.cause)
            assertEquals(ProductRetryTarget.ANALYSIS, productError.retryTarget)
            recordTrace(
                timeline,
                "structured-host-process-lost",
                "transport=${safeTransportDiagnostic(fault, owner)};" +
                    "logical_job=${safeLogicalJobDiagnostic(fault, owner, logicalJobId)};" +
                    "product=${productJobDiagnostic(owner)};${processDiagnostic()}",
            )
            recordHostStoreTrace(timeline, "durable-terminal", logicalJobId)

            File(directory, "host-process-loss-identity.txt").writeText(
                buildString {
                    appendLine("analysis_job_id=${productJob.jobId.value}")
                    appendLine("logical_job_id=${logicalJobId.value}")
                    appendLine("pre_loss_state=${productJob.state.name}")
                    appendLine("post_restart_state=${interrupted.state.name}")
                    appendLine("failure_code=${interrupted.failureCode?.name}")
                    appendLine("product_failure=${productError.technicalDetails.cause}")
                    appendLine("native_execution_survived=false")
                },
            )
        } finally {
            runCatching { fault.releaseGeneration(application) }
            runCatching { fault.resetGenerationGate(application) }
            store.clear()
        }
    }

    private fun awaitStructuredHostProcessLoss(
        owner: ProcessLocalProductAnalysisOwner,
        fault: HarnessEmulatorE2eFaultControl,
        logicalJobId: ConsumerInferenceJobId,
        timeline: File,
    ): io.github.daniele21.redactguard.domain.analysis.AnalysisJobSnapshot {
        val deadline = SystemClock.elapsedRealtime() + ANALYSIS_TIMEOUT_MS
        var lastLogicalDiagnostic: String? = null
        while (SystemClock.elapsedRealtime() < deadline) {
            owner.currentSnapshot()?.takeIf { snapshot -> snapshot.state == AnalysisJobState.FAILED }?.let { return it }

            val logicalDiagnostic = safeLogicalJobDiagnostic(fault, owner, logicalJobId)
            if (logicalDiagnostic != lastLogicalDiagnostic) {
                recordTrace(
                    timeline,
                    "post-reconnect-poll",
                    "transport=${safeTransportDiagnostic(fault, owner)};logical_job=$logicalDiagnostic;" +
                        "product=${productJobDiagnostic(owner)};${processDiagnostic()}",
                )
                lastLogicalDiagnostic = logicalDiagnostic
            }
            SystemClock.sleep(DIAGNOSTIC_POLL_INTERVAL_MS)
        }

        val logicalDiagnostic = safeLogicalJobDiagnostic(fault, owner, logicalJobId)
        val durableDiagnostic = hostLogicalJobStoreSnapshot(logicalJobId)
        val productDiagnostic = productJobDiagnostic(owner)
        recordTrace(
            timeline,
            "timeout",
            "transport=${safeTransportDiagnostic(fault, owner)};logical_job=$logicalDiagnostic;" +
                "product=$productDiagnostic;host_store=$durableDiagnostic;${processDiagnostic()}",
        )
        throw AssertionError(
            "Timed out waiting for structured product Host-process-loss outcome; " +
                "logical_job=$logicalDiagnostic; product=$productDiagnostic; host_store=$durableDiagnostic",
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
            "Ada Lovelace lives at 1 Test Street. Contact ada@example.test for this process-loss fixture.",
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
            raw.lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .filter { line -> logicalJobId.value in line }
                .toList()
        if (matching.isNotEmpty()) return matching.joinToString("|")
        val compactRaw = raw.lineSequence().map(String::trim).filter(String::isNotEmpty).joinToString("|")
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
        timeline.appendText("$line\n")
        println("RG_HOST_PROCESS_LOSS_TRACE $line")
    }

    private fun hostProcessLossEvidenceDirectory(application: Application): File =
        File(requireNotNull(application.getExternalFilesDir(null)), "two-apk-host-process-loss")

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
        const val ANALYSIS_TIMEOUT_MS = 30_000L
        const val HOST_LOGICAL_JOB_PREFERENCES = "harnex_consumer_logical_jobs_v1"
    }
}
