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
import org.junit.Assert.assertNotNull
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
        hostProcessLossEvidenceDirectory(application).deleteRecursively()

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
            val gateStatus = fault.awaitGenerationBlocked(application)
            val logicalJobId =
                awaitValue("accepted Harness logical job") {
                    fault.acceptedLogicalJobId(owner)
                }
            assertTrue(gateStatus.paused)
            assertTrue(gateStatus.waitingRequests > 0)

            shell("am force-stop ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}")
            await("Host process disconnect", READY_TIMEOUT_MS) {
                !viewModel.uiState.value.connection.analysisReady
            }

            shell(
                "am start -W -n ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}/" +
                    "io.github.daniele21.localllm.phonetest.MainActivity",
            )
            viewModel.connectHarness()
            await("Harness reconnect after process restart", READY_TIMEOUT_MS) {
                viewModel.uiState.value.connection.analysisReady
            }

            val interrupted =
                awaitValue("structured product Host-process-loss outcome", ANALYSIS_TIMEOUT_MS) {
                    owner.currentSnapshot()?.takeIf { snapshot -> snapshot.state == AnalysisJobState.FAILED }
                }
            assertEquals(productJob.jobId, interrupted.jobId)
            assertEquals(DocumentAnalysisFailureCode.HOST_PROCESS_LOST, interrupted.failureCode)

            await("Host-process-loss recovery UI", DEFAULT_TIMEOUT_MS) {
                viewModel.uiState.value.step == ProductStep.ERROR
            }
            val productError = assertNotNull(viewModel.uiState.value.error)
            assertEquals(ProductFailureKind.HOST_PROCESS_LOST.name, productError.technicalDetails.cause)
            assertEquals(ProductRetryTarget.ANALYSIS, productError.retryTarget)

            val directory = hostProcessLossEvidenceDirectory(application).apply(File::mkdirs)
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
        const val DEFAULT_TIMEOUT_MS = 8_000L
        const val READY_TIMEOUT_MS = 15_000L
        const val ANALYSIS_TIMEOUT_MS = 30_000L
    }
}
