package io.github.daniele21.redactguard

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.redactguard.ui.ProductStep
import org.junit.Assert.assertEquals
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
            await("initial Harness readiness", READY_TIMEOUT_MS) {
                viewModel.uiState.value.connection.analysisReady
            }
            val before = fault.activityAuditStatus(application)

            prepareSyntheticAnalysis(viewModel)
            viewModel.startAnalysis()
            await("RedactGuard analysis reaches review", ANALYSIS_TIMEOUT_MS) {
                viewModel.uiState.value.step in setOf(ProductStep.REVIEW, ProductStep.NO_FINDINGS, ProductStep.ERROR)
            }
            val finalState = viewModel.uiState.value
            assertEquals(finalState.error?.technicalDetails?.code, ProductStep.REVIEW, finalState.step)
            assertTrue(finalState.reviewTotal >= 1)

            val recorded =
                awaitValue("new durable Harnex Activity record", ANALYSIS_TIMEOUT_MS) {
                    fault.activityAuditStatus(application).takeIf { status ->
                        status.available &&
                            status.count > before.count &&
                            status.identity?.status == SUCCEEDED_STATUS &&
                            status.identity.verifiedPackageName == BuildConfig.APPLICATION_ID
                    }
                }
            val recordedIdentity = requireNotNull(recorded.identity)
            assertTrue(recorded.content.input)
            assertTrue(recorded.content.effectivePrompt)
            assertTrue(recorded.content.answer)

            shell("am force-stop ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}")
            await("Binder disconnect after Host restart boundary", READY_TIMEOUT_MS) {
                !viewModel.uiState.value.connection.analysisReady
            }
            shell(
                "am start -W -n ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}/" +
                    "io.github.daniele21.localllm.phonetest.MainActivity",
            )

            val persisted =
                awaitValue("same Activity record after Host restart", READY_TIMEOUT_MS) {
                    runCatching { fault.activityAuditStatus(application) }
                        .getOrNull()
                        ?.takeIf { status ->
                            status.available && status.identity?.requestId == recordedIdentity.requestId
                        }
                }
            val persistedIdentity = requireNotNull(persisted.identity)
            assertEquals(recordedIdentity, persistedIdentity)
            assertTrue(persisted.count >= recorded.count)
            assertTrue(persisted.content.input)
            assertTrue(persisted.content.effectivePrompt)
            assertTrue(persisted.content.answer)

            writePrivacySafeEvidence(
                application = application,
                beforeCount = before.count,
                recorded = recorded,
                persisted = persisted,
            )
        } finally {
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
        viewModel.importText(SYNTHETIC_INPUT)
        await("pasted text definitions") {
            viewModel.uiState.value.step == ProductStep.DEFINITIONS &&
                viewModel.uiState.value.definitions.isNotEmpty()
        }
        val firstChoice = viewModel.uiState.value.definitions.first()
        if (!firstChoice.selected) viewModel.toggleDefinition(firstChoice.id)
        assertTrue(viewModel.uiState.value.definitions.any { it.selected })
        assertTrue(viewModel.uiState.value.connection.analysisReady)
    }

    private fun writePrivacySafeEvidence(
        application: Application,
        beforeCount: Int,
        recorded: HarnessEmulatorE2eFaultControl.ActivityAuditStatus,
        persisted: HarnessEmulatorE2eFaultControl.ActivityAuditStatus,
    ) {
        val identity = requireNotNull(recorded.identity)
        val directory =
            File(requireNotNull(application.getExternalFilesDir(null)), EVIDENCE_DIRECTORY).apply {
                deleteRecursively()
                mkdirs()
            }
        File(directory, EVIDENCE_FILE).writeText(
            buildString {
                appendLine("request_id=${identity.requestId}")
                appendLine("verified_package=${identity.verifiedPackageName}")
                appendLine("application_id=${identity.applicationId}")
                appendLine("use_case_id=${identity.useCaseId}")
                appendLine("status=${identity.status}")
                appendLine("records_before=$beforeCount")
                appendLine("records_after_generation=${recorded.count}")
                appendLine("records_after_restart=${persisted.count}")
                appendLine("input_present=${persisted.content.input}")
                appendLine("effective_prompt_present=${persisted.content.effectivePrompt}")
                appendLine("answer_present=${persisted.content.answer}")
                appendLine("reasoning_present=${persisted.content.reasoning}")
                appendLine("persisted_after_restart=true")
                appendLine("sensitive_values_exported=false")
            },
        )
    }

    private fun await(
        label: String,
        timeoutMs: Long,
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
        const val READY_TIMEOUT_MS = 15_000L
        const val ANALYSIS_TIMEOUT_MS = 30_000L
        const val SUCCEEDED_STATUS = "SUCCEEDED"
        const val EVIDENCE_DIRECTORY = "two-apk-activity-persistence"
        const val EVIDENCE_FILE = "activity-persistence-identity.txt"
        const val SYNTHETIC_INPUT =
            "Ada Lovelace lives at 1 Test Street. Contact ada@example.test for this Activity persistence fixture."
    }
}
