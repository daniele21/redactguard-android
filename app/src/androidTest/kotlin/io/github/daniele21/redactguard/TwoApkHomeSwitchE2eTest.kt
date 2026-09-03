package io.github.daniele21.redactguard

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobState
import io.github.daniele21.redactguard.ui.ProductStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class TwoApkHomeSwitchE2eTest {
    @Test
    fun activeLogicalJobSurvivesHomeSwitchAndReturnsToReview() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ProcessLocalProductAnalysisOwner.get(application)
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        val fault = HarnessEmulatorE2eFaultControl
        val evidenceDirectory = lifecycleEvidenceDirectory(application)
        evidenceDirectory.deleteRecursively()

        fault.resetGenerationGate(application)
        fault.pauseGeneration(application)
        viewModel.connectHarness()
        try {
            await("initial Harness readiness", READY_TIMEOUT_MS) {
                viewModel.uiState.value.connection.analysisReady
            }
            prepareSyntheticAnalysis(viewModel)
            viewModel.startAnalysis()

            val active =
                awaitValue("accepted background process-local analysis job") {
                    owner.currentSnapshot()?.takeIf { snapshot -> snapshot.state == AnalysisJobState.ACTIVE }
                }
            val blocked = fault.awaitGenerationBlocked(application)
            val logicalJobId =
                awaitValue("accepted Harness logical job") {
                    fault.acceptedLogicalJobId(owner)
                }
            assertTrue(blocked.paused)
            assertTrue(blocked.waitingRequests > 0)

            launchRedactGuardActivity()
            await("RedactGuard activity resumed") { redactGuardIsResumed() }
            captureLifecycleScreenshot(application, "01-analysis-before-home")

            shell("input keyevent KEYCODE_HOME")
            await("RedactGuard activity backgrounded") { !redactGuardIsResumed() }
            captureLifecycleScreenshot(application, "02-launcher-background")

            val backgroundSnapshot = owner.currentSnapshot()
            assertNotNull(backgroundSnapshot)
            assertEquals(active.jobId, backgroundSnapshot?.jobId)
            assertEquals(AnalysisJobState.ACTIVE, backgroundSnapshot?.state)
            assertFalse(
                backgroundSnapshot?.state == AnalysisJobState.CANCEL_REQUESTED ||
                    backgroundSnapshot?.state == AnalysisJobState.CANCELLED,
            )

            launchRedactGuardActivity()
            await("RedactGuard activity resumed after app switch") { redactGuardIsResumed() }
            val reattachedSnapshot = owner.currentSnapshot()
            assertNotNull(reattachedSnapshot)
            assertEquals(active.jobId, reattachedSnapshot?.jobId)
            assertEquals(AnalysisJobState.ACTIVE, reattachedSnapshot?.state)

            fault.releaseGeneration(application)
            await("background analysis reaches review after return", ANALYSIS_TIMEOUT_MS) {
                viewModel.uiState.value.step in setOf(ProductStep.REVIEW, ProductStep.NO_FINDINGS, ProductStep.ERROR)
            }

            val finalState = viewModel.uiState.value
            assertEquals(finalState.error?.technicalDetails?.code, ProductStep.REVIEW, finalState.step)
            assertNotNull(finalState.reviewFinding)
            assertTrue(finalState.reviewTotal >= 1)
            captureLifecycleScreenshot(application, "03-review-after-return")

            File(evidenceDirectory.apply(File::mkdirs), "lifecycle-identity.txt").writeText(
                buildString {
                    appendLine("analysis_job_id=${active.jobId.value}")
                    appendLine("logical_job_id=${logicalJobId.value}")
                    appendLine("background_state=${backgroundSnapshot?.state?.name}")
                    appendLine("reattached_state=${reattachedSnapshot?.state?.name}")
                    appendLine("gate_waiting_requests=${blocked.waitingRequests}")
                    appendLine("implicit_cancel=false")
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
            "Ada Lovelace lives at 1 Test Street. Contact ada@example.test for this Home-switch fixture.",
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

    private fun launchRedactGuardActivity() {
        shell(
            "am start -W -n ${BuildConfig.APPLICATION_ID}/" +
                "io.github.daniele21.redactguard.MainActivity",
        )
    }

    private fun redactGuardIsResumed(): Boolean {
        var resumed = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            resumed =
                ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .any { activity -> activity is MainActivity }
        }
        return resumed
    }

    private fun captureLifecycleScreenshot(
        application: Application,
        name: String,
    ) {
        val directory = lifecycleEvidenceDirectory(application).apply(File::mkdirs)
        val screenshot = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        FileOutputStream(File(directory, "$name.png")).use { output ->
            check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        screenshot.recycle()
    }

    private fun lifecycleEvidenceDirectory(application: Application): File =
        File(requireNotNull(application.getExternalFilesDir(null)), "two-apk-lifecycle")

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
