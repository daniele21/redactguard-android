package io.github.daniele21.redactguard

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobId
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionState
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobId
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobState
import io.github.daniele21.redactguard.ui.ProductStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class TwoApkBinderLossE2eTest {
    @Test
    fun acceptedLogicalJobSurvivesBinderDisconnectAndRebind() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ProcessLocalProductAnalysisOwner.get(application)
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        val fault = HarnessEmulatorE2eFaultControl
        binderLossEvidenceDirectory(application).deleteRecursively()

        fault.resetGenerationGate(application)
        fault.pauseGeneration(application)
        try {
            await("initial Harness readiness", READY_TIMEOUT_MS) {
                viewModel.uiState.value.connection.analysisReady
            }
            prepareSyntheticAnalysis(viewModel)
            viewModel.startAnalysis()

            val productJobId =
                awaitValue("accepted process-local analysis job") {
                    owner.currentSnapshot()?.takeIf { snapshot -> snapshot.state == AnalysisJobState.ACTIVE }?.jobId
                }
            val gateStatus = fault.awaitGenerationBlocked(application)
            val logicalJobId =
                awaitValue("accepted Harness logical job") {
                    fault.acceptedLogicalJobId(owner)
                }

            assertTrue(gateStatus.paused)
            assertTrue(gateStatus.waitingRequests > 0)
            assertEquals(
                SharedRuntimeConnectionState.CONNECTION_LOST,
                fault.injectConsumerConnectionLoss(owner),
            )

            val duringDisconnect = owner.currentSnapshot()
            assertNotNull(duringDisconnect)
            assertEquals(productJobId, duringDisconnect?.jobId)
            assertFalse(
                duringDisconnect?.state == AnalysisJobState.CANCEL_REQUESTED ||
                    duringDisconnect?.state == AnalysisJobState.CANCELLED,
            )

            await("real Binder client reconnect", READY_TIMEOUT_MS) {
                fault.consumerConnectionState(owner) == SharedRuntimeConnectionState.CONNECTED
            }
            await("product readiness after Binder rebind", READY_TIMEOUT_MS) {
                viewModel.uiState.value.connection.analysisReady
            }
            val reattachedLogicalJobId =
                awaitValue("same Harness logical job after reconnect") {
                    fault.acceptedLogicalJobId(owner)
                }
            assertEquals(logicalJobId, reattachedLogicalJobId)
            writeBinderLossIdentity(
                application = application,
                productJobId = productJobId,
                logicalJobId = logicalJobId,
                gateWaitingRequests = gateStatus.waitingRequests,
            )

            fault.releaseGeneration(application)
            await("analysis completion after Binder rebind", ANALYSIS_TIMEOUT_MS) {
                viewModel.uiState.value.step in setOf(ProductStep.REVIEW, ProductStep.NO_FINDINGS, ProductStep.ERROR)
            }

            val finalState = viewModel.uiState.value
            assertEquals(finalState.error?.technicalDetails?.code, ProductStep.REVIEW, finalState.step)
            assertNotNull(finalState.reviewFinding)
            assertTrue(finalState.reviewTotal >= 1)
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
            "Ada Lovelace lives at 1 Test Street. Contact ada@example.test for this synthetic fixture.",
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

    private fun writeBinderLossIdentity(
        application: Application,
        productJobId: AnalysisJobId,
        logicalJobId: ConsumerInferenceJobId,
        gateWaitingRequests: Int,
    ) {
        val directory = binderLossEvidenceDirectory(application).apply(File::mkdirs)
        File(directory, "binder-loss-identity.txt").writeText(
            buildString {
                appendLine("analysis_job_id=${productJobId.value}")
                appendLine("logical_job_id_before=${logicalJobId.value}")
                appendLine("logical_job_id_after=${logicalJobId.value}")
                appendLine("gate_waiting_requests=$gateWaitingRequests")
                appendLine("connection_loss=CONNECTION_LOST")
                appendLine("connection_rebound=CONNECTED")
                appendLine("implicit_cancel=false")
            },
        )
    }

    private fun binderLossEvidenceDirectory(application: Application): File =
        File(requireNotNull(application.getExternalFilesDir(null)), "two-apk-binder-loss")

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
        const val POLL_INTERVAL_MS = 50L
        const val DEFAULT_TIMEOUT_MS = 8_000L
        const val READY_TIMEOUT_MS = 15_000L
        const val ANALYSIS_TIMEOUT_MS = 30_000L
    }
}
