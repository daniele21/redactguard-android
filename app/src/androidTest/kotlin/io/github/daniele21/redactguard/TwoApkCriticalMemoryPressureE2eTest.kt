package io.github.daniele21.redactguard

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
class TwoApkCriticalMemoryPressureE2eTest {
    @Test
    fun activeLogicalJobFailsAsRuntimePressureAndCleansHostExecution() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ProcessLocalProductAnalysisOwner.get(application)
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        val fault = HarnessEmulatorE2eFaultControl
        criticalPressureEvidenceDirectory(application).deleteRecursively()

        fault.resetGenerationGate(application)
        fault.pauseGeneration(application)
        viewModel.connectHarness()
        try {
            await("initial Harness readiness", READY_TIMEOUT_MS) {
                viewModel.uiState.value.connection.analysisReady
            }
            prepareSyntheticAnalysis(viewModel)
            viewModel.startAnalysis()

            val productJob =
                awaitValue("accepted product analysis job") {
                    owner.currentSnapshot()?.takeIf { snapshot -> snapshot.state == AnalysisJobState.ACTIVE }
                }
            val blocked = fault.awaitGenerationBlocked(application)
            val logicalJobId =
                awaitValue("accepted Harness logical job") {
                    fault.acceptedLogicalJobId(owner)
                }
            assertTrue(blocked.paused)
            assertTrue(blocked.waitingRequests > 0)

            shell("am send-trim-memory ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE} RUNNING_CRITICAL")

            val failed =
                awaitValue("structured critical-pressure failure", ANALYSIS_TIMEOUT_MS) {
                    owner.currentSnapshot()?.takeIf { snapshot -> snapshot.state == AnalysisJobState.FAILED }
                }
            assertEquals(productJob.jobId, failed.jobId)
            assertEquals(DocumentAnalysisFailureCode.CHUNK_FAILED, failed.failureCode)

            await("critical-pressure recovery UI", DEFAULT_TIMEOUT_MS) {
                viewModel.uiState.value.step == ProductStep.ERROR
            }
            val productError = requireNotNull(viewModel.uiState.value.error)
            assertEquals(ProductFailureKind.CHUNK_FAILED.name, productError.technicalDetails.cause)
            assertEquals(ProductRetryTarget.ANALYSIS, productError.retryTarget)
            assertTrue(viewModel.uiState.value.connection.analysisReady)

            val cleaned =
                awaitValue("Host generation waiter cleanup") {
                    fault.generationGateStatus(application).takeIf { status -> status.waitingRequests == 0 }
                }
            assertTrue(cleaned.paused)

            val directory = criticalPressureEvidenceDirectory(application).apply(File::mkdirs)
            File(directory, "critical-pressure-identity.txt").writeText(
                buildString {
                    appendLine("analysis_job_id=${productJob.jobId.value}")
                    appendLine("logical_job_id=${logicalJobId.value}")
                    appendLine("pre_pressure_state=${productJob.state.name}")
                    appendLine("post_pressure_state=${failed.state.name}")
                    appendLine("failure_code=${failed.failureCode?.name}")
                    appendLine("product_failure=${productError.technicalDetails.cause}")
                    appendLine("host_waiters_after_pressure=${cleaned.waitingRequests}")
                    appendLine("host_process_lost=false")
                    appendLine("explicit_user_cancel=false")
                    appendLine("physical_memory_pressure_claimed=false")
                },
            )
        } finally {
            runCatching { fault.releaseGeneration(application) }
            runCatching { fault.resetGenerationGate(application) }
            store.clear()
        }
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
        const val DEFAULT_TIMEOUT_MS = 8_000L
        const val READY_TIMEOUT_MS = 15_000L
        const val ANALYSIS_TIMEOUT_MS = 30_000L
    }
}
