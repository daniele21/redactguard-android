package io.github.daniele21.redactguard

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobSnapshot
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobState
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobSubscription
import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisFailureCode
import io.github.daniele21.redactguard.ui.ProductStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class TwoApkExplicitCancelE2eTest {
    @Test
    fun explicitCancelPublishesExactTerminalAndCleansHostWaiter() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ProcessLocalProductAnalysisOwner.get(application)
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        val fault = HarnessEmulatorE2eFaultControl
        val observed = CopyOnWriteArrayList<AnalysisJobSnapshot>()
        var subscription: AnalysisJobSubscription? = null
        explicitCancelEvidenceDirectory(application).deleteRecursively()

        viewModel.connectHarness()
        try {
            await("initial Harness readiness", READY_TIMEOUT_MS) {
                viewModel.uiState.value.connection.analysisReady
            }
            fault.resetGenerationGate(application)
            fault.pauseGeneration(application)
            prepareSyntheticAnalysis(viewModel)
            viewModel.startAnalysis()

            val active =
                awaitValue("accepted product analysis job") {
                    owner.currentSnapshot()?.takeIf { snapshot -> snapshot.state == AnalysisJobState.ACTIVE }
                }
            subscription = owner.observe(active.jobId, observed::add)
            val blocked = fault.awaitGenerationBlocked(application)
            assertTrue(blocked.paused)
            assertTrue(blocked.waitingRequests > 0)

            val logicalJobIdAtCancel = fault.acceptedLogicalJobId(owner)
            viewModel.cancelAnalysis()

            await("product cancel requested") {
                observed.any { snapshot -> snapshot.state == AnalysisJobState.CANCEL_REQUESTED }
            }
            try {
                await("product terminal cancellation", ANALYSIS_TIMEOUT_MS) {
                    observed.any { snapshot -> snapshot.state == AnalysisJobState.CANCELLED }
                }
            } catch (failure: AssertionError) {
                val gate = fault.generationGateStatus(application)
                val logicalJobId = logicalJobIdAtCancel ?: fault.acceptedLogicalJobId(owner)
                val host =
                    logicalJobId?.let { jobId -> fault.logicalJobDiagnostic(owner, jobId) }
                        ?: "logical_job=unavailable"
                val currentProduct = owner.currentSnapshot()
                val diagnostic =
                    buildString {
                        append("product_current=${currentProduct?.state?.name ?: "none"}")
                        append(";observed=${observed.joinToString(",") { it.state.name }}")
                        append(";logical_job_id=${logicalJobId?.value ?: "unavailable"}")
                        append(";$host")
                        append(";gate_paused=${gate.paused}")
                        append(";gate_waiters=${gate.waitingRequests}")
                        append(";connection=${fault.consumerConnectionState(owner).name}")
                    }
                val directory = explicitCancelEvidenceDirectory(application).apply(File::mkdirs)
                File(directory, "explicit-cancel-timeout-diagnostic.txt").writeText(diagnostic + "\n")
                throw AssertionError("${failure.message}; $diagnostic", failure)
            }
            val cancelled =
                observed.last { snapshot -> snapshot.state == AnalysisJobState.CANCELLED }
            assertEquals(DocumentAnalysisFailureCode.CANCELLED, cancelled.failureCode)
            assertEquals(
                listOf(
                    AnalysisJobState.ACTIVE,
                    AnalysisJobState.CANCEL_REQUESTED,
                    AnalysisJobState.CANCELLED,
                ),
                observed.map(AnalysisJobSnapshot::state),
            )

            val logicalJobIdForEvidence = logicalJobIdAtCancel ?: fault.acceptedLogicalJobId(owner)
            await("terminal product job consumption") {
                owner.currentSnapshot() == null &&
                    viewModel.uiState.value.step == ProductStep.DEFINITIONS
            }
            val cleanedGate =
                awaitValue("Harness generation waiter cleanup") {
                    fault.generationGateStatus(application).takeIf { status ->
                        status.paused && status.waitingRequests == 0
                    }
                }

            val directory = explicitCancelEvidenceDirectory(application).apply(File::mkdirs)
            File(directory, "explicit-cancel-identity.txt").writeText(
                buildString {
                    appendLine("analysis_job_id=${active.jobId.value}")
                    appendLine("logical_job_id=${logicalJobIdForEvidence?.value ?: "unavailable"}")
                    appendLine("observed_states=${observed.joinToString(",") { it.state.name }}")
                    appendLine("failure_code=${cancelled.failureCode?.name}")
                    appendLine("host_waiting_requests=${cleanedGate.waitingRequests}")
                    appendLine("product_job_consumed=true")
                    appendLine("explicit_cancel=true")
                },
            )
        } finally {
            subscription?.close()
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
            "Ada Lovelace lives at 1 Test Street. Contact ada@example.test for this explicit-cancel fixture.",
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

    private fun explicitCancelEvidenceDirectory(application: Application): File =
        File(requireNotNull(application.getExternalFilesDir(null)), "two-apk-explicit-cancel")

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
