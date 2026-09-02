package io.github.daniele21.redactguard

import android.app.Application
import android.os.Process
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobState
import io.github.daniele21.redactguard.ui.ProductStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class TwoApkRedactGuardProcessLossE2eTest {
    @Test
    fun phase1StartsActiveAnalysisWithoutPersistingSensitiveReattachContext() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ProcessLocalProductAnalysisOwner.get(application)
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        val fault = HarnessEmulatorE2eFaultControl
        consumerProcessLossEvidenceDirectory(application).deleteRecursively()

        fault.resetGenerationGate(application)
        fault.pauseGeneration(application)
        viewModel.connectHarness()
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

        val directory = consumerProcessLossEvidenceDirectory(application).apply(File::mkdirs)
        File(directory, PHASE_ONE_FILE).writeText(
            buildString {
                appendLine("phase1_pid=${Process.myPid()}")
                appendLine("analysis_job_id=${productJob.jobId.value}")
                appendLine("logical_job_id=${logicalJobId.value}")
                appendLine("analysis_state=${productJob.state.name}")
                appendLine("persisted_sensitive_context=false")
            },
        )
    }

    @Test
    fun phase2StartsFreshWithoutReconstructingLostDocumentOrProductJob() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val directory = consumerProcessLossEvidenceDirectory(application)
        val phaseOneFile = File(directory, PHASE_ONE_FILE)
        assertTrue("Missing phase-one process-loss evidence", phaseOneFile.isFile)
        val phaseOneEvidence = phaseOneFile.readText()
        val phaseOnePid =
            phaseOneEvidence
                .lineSequence()
                .first { line -> line.startsWith("phase1_pid=") }
                .substringAfter('=')
                .toInt()
        assertTrue("Phase two must run in a new RedactGuard process", phaseOnePid != Process.myPid())

        val owner = ProcessLocalProductAnalysisOwner.get(application)
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        try {
            assertNull(owner.currentSnapshot())
            assertEquals(ProductStep.IMPORT, viewModel.uiState.value.step)
            assertTrue(viewModel.uiState.value.definitions.isEmpty())
            assertEquals(0, viewModel.uiState.value.reviewTotal)
            assertNull(viewModel.uiState.value.reviewFinding)

            File(directory, "redactguard-process-loss-identity.txt").writeText(
                buildString {
                    append(phaseOneEvidence)
                    appendLine("phase2_pid=${Process.myPid()}")
                    appendLine("product_job_restored=false")
                    appendLine("document_context_restored=false")
                    appendLine("review_context_restored=false")
                    appendLine("recovery_step=${viewModel.uiState.value.step.name}")
                },
            )
        } finally {
            runCatching { HarnessEmulatorE2eFaultControl.releaseGeneration(application) }
            runCatching { HarnessEmulatorE2eFaultControl.resetGenerationGate(application) }
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
                viewModel.uiState.value.definitions.isNotEmpty()
        }
        val firstChoice = viewModel.uiState.value.definitions.first()
        if (!firstChoice.selected) viewModel.toggleDefinition(firstChoice.id)
        assertTrue(viewModel.uiState.value.definitions.any { it.selected })
        assertTrue(viewModel.uiState.value.connection.analysisReady)
    }

    private fun consumerProcessLossEvidenceDirectory(application: Application): File =
        File(requireNotNull(application.getExternalFilesDir(null)), "two-apk-redactguard-process-loss")

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

    private companion object {
        const val PHASE_ONE_FILE = "phase1-identity.txt"
        const val POLL_INTERVAL_MS = 50L
        const val DEFAULT_TIMEOUT_MS = 8_000L
        const val READY_TIMEOUT_MS = 15_000L
    }
}
