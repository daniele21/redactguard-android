package io.github.daniele21.redactguard

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import io.github.daniele21.redactguard.ui.ProductStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream

@RunWith(AndroidJUnit4::class)
class TwoApkActivityPersistenceE2eTest {
    @Test
    fun successfulRedactGuardInferencePersistsInHarnessActivityAcrossHostRestart() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        val fault = HarnessEmulatorE2eFaultControl

        fault.resetGenerationGate(application)
        viewModel.connectHarness()
        try {
            awaitHarnessReady(viewModel)
            val before = fault.activityAuditStatus(application)

            prepareSyntheticAnalysis(viewModel, "success")
            viewModel.startAnalysis()
            await("RedactGuard analysis reaches review", ANALYSIS_TIMEOUT_MS) {
                viewModel.uiState.value.step in setOf(ProductStep.REVIEW, ProductStep.NO_FINDINGS, ProductStep.ERROR)
            }
            val finalState = viewModel.uiState.value
            assertEquals(finalState.error?.technicalDetails?.code, ProductStep.REVIEW, finalState.step)
            assertTrue(finalState.reviewTotal >= 1)

            val recorded = awaitNewActivity(application, before.count, COMPLETED_STATUS, ANALYSIS_TIMEOUT_MS)
            val recordedIdentity = requireNotNull(recorded.identity)
            assertVerifiedExternalActivity(recorded)
            assertTrue(recorded.content.input)
            assertTrue(recorded.content.effectivePrompt)
            assertTrue(recorded.content.answer)
            assertTrue(recorded.execution.modelDigest)
            assertTrue(recorded.metrics.totalMs)
            assertTrue(recorded.metrics.outputTokens)
            assertTrue(recorded.metrics.decodeTokensPerSecond)

            shell("am force-stop ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}")
            await("Binder disconnect after Host restart boundary", READY_TIMEOUT_MS) {
                !viewModel.uiState.value.connection.analysisReady
            }
            startHarnessHost()
            viewModel.connectHarness()
            awaitHarnessReady(viewModel)

            val persisted =
                awaitValue("same Activity record after Host restart", READY_TIMEOUT_MS) {
                    activityAuditStatusOrNull(application)
                        ?.takeIf { status ->
                            status.available &&
                                status.identity?.requestId == recordedIdentity.requestId &&
                                status.identity.status == COMPLETED_STATUS
                        }
                }
            val persistedIdentity = requireNotNull(persisted.identity)
            assertEquals(recordedIdentity, persistedIdentity)
            assertTrue(persisted.count >= recorded.count)
            assertVerifiedExternalActivity(persisted)
            assertTrue(persisted.content.input)
            assertTrue(persisted.content.effectivePrompt)
            assertTrue(persisted.content.answer)
            assertTrue(persisted.execution.modelDigest)
            assertTrue(persisted.metrics.totalMs)
            assertTrue(persisted.metrics.outputTokens)
            assertTrue(persisted.metrics.decodeTokensPerSecond)

            writeActivityEvidence(
                application = application,
                fileName = SUCCESS_EVIDENCE_FILE,
                scenario = "success_restart",
                beforeCount = before.count,
                status = persisted,
                extra =
                    listOf(
                        "records_after_generation=${recorded.count}",
                        "records_after_restart=${persisted.count}",
                        "same_request_after_restart=${persistedIdentity.requestId == recordedIdentity.requestId}",
                        "persisted_after_restart=true",
                    ),
            )
        } finally {
            runCatching { fault.resetGenerationGate(application) }
            store.clear()
        }
    }

    @Test
    fun explicitCancelEndsAsCancelledInHarnessActivity() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        val fault = HarnessEmulatorE2eFaultControl

        fault.resetGenerationGate(application)
        fault.pauseGeneration(application)
        viewModel.connectHarness()
        try {
            awaitHarnessReady(viewModel)
            val before = fault.activityAuditStatus(application)

            prepareSyntheticAnalysis(viewModel, "cancel")
            viewModel.startAnalysis()
            val blocked = fault.awaitGenerationBlocked(application)
            assertTrue(blocked.paused)
            assertTrue(blocked.waitingRequests > 0)

            viewModel.cancelAnalysis()

            val cancelled = awaitNewActivity(application, before.count, CANCELLED_STATUS, ANALYSIS_TIMEOUT_MS)
            assertVerifiedExternalActivity(cancelled)
            assertTrue(cancelled.content.input)
            assertTrue(cancelled.content.effectivePrompt)
            assertFalse(cancelled.content.answer)
            assertTrue(cancelled.execution.modelDigest)
            assertTrue(requireNotNull(cancelled.identity).terminalCode != NO_TERMINAL_CODE)

            val cleaned = awaitGateCleaned(application)
            writeActivityEvidence(
                application = application,
                fileName = CANCEL_EVIDENCE_FILE,
                scenario = "explicit_cancel",
                beforeCount = before.count,
                status = cancelled,
                extra = listOf("host_waiters_after_terminal=${cleaned.waitingRequests}"),
            )
        } finally {
            runCatching { fault.releaseGeneration(application) }
            runCatching { fault.resetGenerationGate(application) }
            store.clear()
        }
    }

    @Test
    fun criticalPressureEndsAsFailedInHarnessActivity() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        val fault = HarnessEmulatorE2eFaultControl

        fault.resetGenerationGate(application)
        fault.pauseGeneration(application)
        viewModel.connectHarness()
        try {
            awaitHarnessReady(viewModel)
            val before = fault.activityAuditStatus(application)

            prepareSyntheticAnalysis(viewModel, "critical-pressure")
            viewModel.startAnalysis()
            val blocked = fault.awaitGenerationBlocked(application)
            assertTrue(blocked.paused)
            assertTrue(blocked.waitingRequests > 0)

            shell("am send-trim-memory ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE} RUNNING_CRITICAL")

            val failed = awaitNewActivity(application, before.count, FAILED_STATUS, ANALYSIS_TIMEOUT_MS)
            assertVerifiedExternalActivity(failed)
            assertTrue(failed.content.input)
            assertTrue(failed.content.effectivePrompt)
            assertFalse(failed.content.answer)
            assertTrue(failed.execution.modelDigest)
            assertTrue(requireNotNull(failed.identity).terminalCode != NO_TERMINAL_CODE)
            await("critical-pressure product failure", ANALYSIS_TIMEOUT_MS) {
                viewModel.uiState.value.step == ProductStep.ERROR
            }

            val cleaned = awaitGateCleaned(application)
            writeActivityEvidence(
                application = application,
                fileName = FAILURE_EVIDENCE_FILE,
                scenario = "critical_pressure_failure",
                beforeCount = before.count,
                status = failed,
                extra = listOf("host_waiters_after_terminal=${cleaned.waitingRequests}"),
            )
        } finally {
            runCatching { fault.releaseGeneration(application) }
            runCatching { fault.resetGenerationGate(application) }
            store.clear()
        }
    }

    @Test
    fun hostProcessLossReconcilesSameHarnessActivityAsInterrupted() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        val fault = HarnessEmulatorE2eFaultControl

        fault.resetGenerationGate(application)
        fault.pauseGeneration(application)
        viewModel.connectHarness()
        try {
            awaitHarnessReady(viewModel)
            val before = fault.activityAuditStatus(application)

            prepareSyntheticAnalysis(viewModel, "host-process-loss")
            viewModel.startAnalysis()
            val blocked = fault.awaitGenerationBlocked(application)
            assertTrue(blocked.paused)
            assertTrue(blocked.waitingRequests > 0)

            val beforeLoss =
                awaitValue("non-terminal Activity before Host process loss", READY_TIMEOUT_MS) {
                    activityAuditStatusOrNull(application)
                        ?.takeIf { status ->
                            status.available &&
                                status.count > before.count &&
                                status.identity?.status in setOf(PREPARED_STATUS, RUNNING_STATUS)
                        }
                }
            val beforeLossIdentity = requireNotNull(beforeLoss.identity)
            assertVerifiedExternalActivity(beforeLoss)
            assertTrue(beforeLoss.content.input)
            assertTrue(beforeLoss.content.effectivePrompt)
            assertTrue(beforeLoss.execution.modelDigest)

            shell("am force-stop ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}")
            await("Host process disconnect", READY_TIMEOUT_MS) {
                !viewModel.uiState.value.connection.analysisReady
            }
            startHarnessHost()
            viewModel.connectHarness()
            awaitHarnessReady(viewModel)

            val interrupted =
                awaitValue("same interrupted Activity after Host restart", ANALYSIS_TIMEOUT_MS) {
                    activityAuditStatusOrNull(application)
                        ?.takeIf { status ->
                            status.available &&
                                status.identity?.requestId == beforeLossIdentity.requestId &&
                                status.identity.status == INTERRUPTED_STATUS
                        }
                }
            val interruptedIdentity = requireNotNull(interrupted.identity)
            assertEquals(HOST_PROCESS_LOSS_TERMINAL_CODE, interruptedIdentity.terminalCode)
            assertVerifiedExternalActivity(interrupted)
            assertTrue(interrupted.content.input)
            assertTrue(interrupted.content.effectivePrompt)
            assertFalse(interrupted.content.answer)
            assertTrue(interrupted.execution.modelDigest)

            await("structured RedactGuard Host process loss", ANALYSIS_TIMEOUT_MS) {
                val state = viewModel.uiState.value
                state.step == ProductStep.ERROR &&
                    state.error?.technicalDetails?.cause == ProductFailureKind.HOST_PROCESS_LOST.name
            }

            writeActivityEvidence(
                application = application,
                fileName = PROCESS_LOSS_EVIDENCE_FILE,
                scenario = "host_process_loss",
                beforeCount = before.count,
                status = interrupted,
                extra =
                    listOf(
                        "pre_loss_request_id=${beforeLossIdentity.requestId}",
                        "same_request_after_restart=${interruptedIdentity.requestId == beforeLossIdentity.requestId}",
                        "reconciled_after_restart=true",
                    ),
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

    private fun prepareSyntheticAnalysis(
        viewModel: RedactGuardProductViewModel,
        scenario: String,
    ) {
        viewModel.importText(
            "Ada Lovelace lives at 1 Test Street. Contact ada@example.test for the $scenario Activity fixture.",
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

    private fun awaitHarnessReady(viewModel: RedactGuardProductViewModel) {
        await("Harness readiness", READY_TIMEOUT_MS) {
            viewModel.uiState.value.connection.analysisReady
        }
    }

    private fun startHarnessHost() {
        shell(
            "am start -W -n ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}/" +
                "io.github.daniele21.localllm.phonetest.MainActivity",
        )
    }

    private fun awaitNewActivity(
        application: Application,
        beforeCount: Int,
        expectedStatus: String,
        timeoutMs: Long,
    ): HarnessEmulatorE2eFaultControl.ActivityAuditStatus =
        awaitValue("new durable Harnex Activity record with status $expectedStatus", timeoutMs) {
            activityAuditStatusOrNull(application)
                ?.takeIf { status ->
                    status.available &&
                        status.count > beforeCount &&
                        status.identity?.status == expectedStatus
                }
        }

    private fun activityAuditStatusOrNull(
        application: Application,
    ): HarnessEmulatorE2eFaultControl.ActivityAuditStatus? =
        try {
            HarnessEmulatorE2eFaultControl.activityAuditStatus(application)
        } catch (failure: IllegalStateException) {
            if (failure.message == BROADCAST_TIMEOUT_MESSAGE) null else throw failure
        }

    private fun awaitGateCleaned(
        application: Application,
    ): HarnessEmulatorE2eFaultControl.GateStatus =
        awaitValue("Harness generation waiter cleanup", ANALYSIS_TIMEOUT_MS) {
            runCatching { HarnessEmulatorE2eFaultControl.generationGateStatus(application) }
                .getOrNull()
                ?.takeIf { status -> status.waitingRequests == 0 }
        }

    private fun assertVerifiedExternalActivity(status: HarnessEmulatorE2eFaultControl.ActivityAuditStatus) {
        val identity = requireNotNull(status.identity)
        assertEquals(BuildConfig.APPLICATION_ID, identity.verifiedPackageName)
        assertTrue(identity.applicationId.isNotBlank())
        assertTrue(identity.useCaseId.isNotBlank())
    }

    private fun writeActivityEvidence(
        application: Application,
        fileName: String,
        scenario: String,
        beforeCount: Int,
        status: HarnessEmulatorE2eFaultControl.ActivityAuditStatus,
        extra: List<String> = emptyList(),
    ) {
        val identity = requireNotNull(status.identity)
        val directory =
            File(requireNotNull(application.getExternalFilesDir(null)), EVIDENCE_DIRECTORY).apply(File::mkdirs)
        File(directory, fileName).writeText(
            buildString {
                appendLine("scenario=$scenario")
                appendLine("request_id=${identity.requestId}")
                appendLine("verified_package=${identity.verifiedPackageName}")
                appendLine("application_id=${identity.applicationId}")
                appendLine("use_case_id=${identity.useCaseId}")
                appendLine("status=${identity.status}")
                appendLine("terminal_code=${identity.terminalCode}")
                appendLine("records_before=$beforeCount")
                appendLine("records_after=${status.count}")
                appendLine("input_present=${status.content.input}")
                appendLine("effective_prompt_present=${status.content.effectivePrompt}")
                appendLine("answer_present=${status.content.answer}")
                appendLine("reasoning_present=${status.content.reasoning}")
                appendLine("model_digest_present=${status.execution.modelDigest}")
                appendLine("total_ms_present=${status.metrics.totalMs}")
                appendLine("output_tokens_present=${status.metrics.outputTokens}")
                appendLine("decode_tps_present=${status.metrics.decodeTokensPerSecond}")
                extra.forEach { line -> appendLine(line) }
                appendLine("sensitive_values_exported=false")
            },
        )
    }

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
        timeoutMs: Long,
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
        const val PREPARED_STATUS = "PREPARED"
        const val RUNNING_STATUS = "RUNNING"
        const val COMPLETED_STATUS = "COMPLETED"
        const val FAILED_STATUS = "FAILED"
        const val CANCELLED_STATUS = "CANCELLED"
        const val INTERRUPTED_STATUS = "INTERRUPTED"
        const val HOST_PROCESS_LOSS_TERMINAL_CODE = "HOST_PROCESS_LOSS"
        const val NO_TERMINAL_CODE = "none"
        const val BROADCAST_TIMEOUT_MESSAGE = "Harness emulator fault command timed out"
        const val EVIDENCE_DIRECTORY = "two-apk-activity-persistence"
        const val SUCCESS_EVIDENCE_FILE = "activity-persistence-identity.txt"
        const val CANCEL_EVIDENCE_FILE = "activity-cancel-identity.txt"
        const val FAILURE_EVIDENCE_FILE = "activity-failure-identity.txt"
        const val PROCESS_LOSS_EVIDENCE_FILE = "activity-process-loss-identity.txt"
    }
}
