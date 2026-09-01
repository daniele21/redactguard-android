package io.github.daniele21.redactguard

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import java.io.FileInputStream
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class TwoApkEmulatorE2eTest {
    @Test
    fun hostAbsentFailsClosedWithoutMakingAnalysisReady() {
        val viewModel = RedactGuardProductViewModel(ApplicationProvider.getApplicationContext<Application>())

        await("Host-absent connection state") {
            !viewModel.uiState.value.connection.analysisReady &&
                viewModel.uiState.value.step == ProductStep.IMPORT
        }
        assertFalse(viewModel.uiState.value.connection.analysisReady)
        assertEquals(ProductStep.IMPORT, viewModel.uiState.value.step)
    }

    @Test
    fun realBinderControlPlaneRuntimeAndReviewSurviveHostRestart() {
        val viewModel = RedactGuardProductViewModel(ApplicationProvider.getApplicationContext<Application>())
        await("initial Harness readiness", READY_TIMEOUT_MS) {
            viewModel.uiState.value.connection.analysisReady
        }

        shell("am force-stop ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}")
        await("Binder disconnect after Host force-stop", READY_TIMEOUT_MS) {
            !viewModel.uiState.value.connection.analysisReady
        }

        shell(
            "am start -W -n ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}/" +
                "io.github.daniele21.localllm.phonetest.MainActivity",
        )
        viewModel.connectHarness()
        await("Harness reconnect after restart", READY_TIMEOUT_MS) {
            viewModel.uiState.value.connection.analysisReady
        }

        prepareSyntheticAnalysis(viewModel)
        viewModel.startAnalysis()
        val observedProgressTitles = linkedSetOf<String>()
        await("cross-process local analysis", ANALYSIS_TIMEOUT_MS) {
            observedProgressTitles += viewModel.analysisProgress.value.title
            viewModel.uiState.value.step in setOf(ProductStep.REVIEW, ProductStep.NO_FINDINGS, ProductStep.ERROR)
        }

        val finalState = viewModel.uiState.value
        assertEquals(finalState.error?.technicalDetails?.code, ProductStep.REVIEW, finalState.step)
        assertNotNull(finalState.reviewFinding)
        assertTrue(finalState.reviewTotal >= 1)
        assertTrue(
            "Expected source-backed runtime progress, observed=$observedProgressTitles",
            observedProgressTitles.any { title ->
                title == "Preparazione AI locale" ||
                    title == "AI locale pronta" ||
                    title == "Ricerca dei dati sensibili"
            },
        )
    }

    @Test
    fun analysisSurvivesViewModelStoreClearAndReattachesSameProcessJob() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ProcessLocalProductAnalysisOwner.get(application)
        val firstStore = ViewModelStore()
        val first = createViewModel(firstStore, application)

        await("initial Harness readiness", READY_TIMEOUT_MS) {
            first.uiState.value.connection.analysisReady
        }
        prepareSyntheticAnalysis(first)
        first.startAnalysis()

        val accepted =
            awaitValue("accepted process-local analysis job") {
                owner.currentSnapshot()?.takeIf { snapshot ->
                    snapshot.state == AnalysisJobState.ACTIVE
                }
            }
        val acceptedJobId = accepted.jobId

        firstStore.clear()

        val detached = owner.currentSnapshot()
        assertNotNull(detached)
        assertEquals(acceptedJobId, detached?.jobId)
        assertFalse(
            detached?.state == AnalysisJobState.CANCEL_REQUESTED ||
                detached?.state == AnalysisJobState.CANCELLED,
        )

        val secondStore = ViewModelStore()
        val second = createViewModel(secondStore, application)
        try {
            await("reattached analysis reaches review", ANALYSIS_TIMEOUT_MS) {
                second.uiState.value.step in setOf(ProductStep.REVIEW, ProductStep.NO_FINDINGS, ProductStep.ERROR)
            }
            val finalState = second.uiState.value
            assertEquals(finalState.error?.technicalDetails?.code, ProductStep.REVIEW, finalState.step)
            assertNotNull(finalState.reviewFinding)
            assertTrue(finalState.reviewTotal >= 1)
        } finally {
            secondStore.clear()
        }
    }

    @Test
    fun analysisSurvivesHomeSwitchAndReturnsToReviewWithSameProcessJob() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ProcessLocalProductAnalysisOwner.get(application)
        val store = ViewModelStore()
        val viewModel = createViewModel(store, application)
        clearLifecycleEvidence(application)

        try {
            await("initial Harness readiness", READY_TIMEOUT_MS) {
                viewModel.uiState.value.connection.analysisReady
            }
            prepareSyntheticBackgroundAnalysis(viewModel)
            viewModel.startAnalysis()

            val accepted =
                awaitValue("accepted background process-local analysis job") {
                    owner.currentSnapshot()?.takeIf { snapshot -> snapshot.state == AnalysisJobState.ACTIVE }
                }
            val acceptedJobId = accepted.jobId

            launchRedactGuardActivity()
            await("RedactGuard activity resumed") { redactGuardIsResumed() }
            captureLifecycleScreenshot(application, "01-analysis-before-home")

            shell("input keyevent KEYCODE_HOME")
            await("RedactGuard activity backgrounded") { !redactGuardIsResumed() }
            captureLifecycleScreenshot(application, "02-launcher-background")

            val backgroundSnapshot = owner.currentSnapshot()
            assertNotNull(backgroundSnapshot)
            assertEquals(acceptedJobId, backgroundSnapshot?.jobId)
            assertFalse(
                backgroundSnapshot?.state == AnalysisJobState.CANCEL_REQUESTED ||
                    backgroundSnapshot?.state == AnalysisJobState.CANCELLED,
            )
            writeLifecycleIdentity(application, acceptedJobId, backgroundSnapshot?.state)

            launchRedactGuardActivity()
            await("RedactGuard activity resumed after app switch") { redactGuardIsResumed() }
            await("background analysis reaches review after return", ANALYSIS_TIMEOUT_MS) {
                viewModel.uiState.value.step in setOf(ProductStep.REVIEW, ProductStep.NO_FINDINGS, ProductStep.ERROR)
            }

            val finalState = viewModel.uiState.value
            assertEquals(finalState.error?.technicalDetails?.code, ProductStep.REVIEW, finalState.step)
            assertNotNull(finalState.reviewFinding)
            assertTrue(finalState.reviewTotal >= 1)
            captureLifecycleScreenshot(application, "03-review-after-return")
        } finally {
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
        prepareSyntheticAnalysis(
            viewModel,
            "Ada Lovelace lives at 1 Test Street. Contact ada@example.test for this synthetic fixture.",
        )
    }

    private fun prepareSyntheticBackgroundAnalysis(viewModel: RedactGuardProductViewModel) {
        val text =
            List(BACKGROUND_FIXTURE_LINES) { index ->
                "Synthetic record $index for Ada Lovelace at $index Test Street; contact ada$index@example.test."
            }.joinToString("\n")
        prepareSyntheticAnalysis(viewModel, text)
    }

    private fun prepareSyntheticAnalysis(
        viewModel: RedactGuardProductViewModel,
        text: String,
    ) {
        viewModel.importText(text)
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

    private fun launchRedactGuardActivity() {
        shell(
            "am start -W -n ${BuildConfig.APPLICATION_ID}/" +
                "io.github.daniele21.redactguard.MainActivity",
        )
    }

    private fun redactGuardIsResumed(): Boolean =
        shell("dumpsys activity activities | grep -m 1 mResumedActivity || true")
            .contains(BuildConfig.APPLICATION_ID)

    private fun clearLifecycleEvidence(application: Application) {
        lifecycleEvidenceDirectory(application).deleteRecursively()
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

    private fun writeLifecycleIdentity(
        application: Application,
        jobId: AnalysisJobId,
        backgroundState: AnalysisJobState?,
    ) {
        val directory = lifecycleEvidenceDirectory(application).apply(File::mkdirs)
        File(directory, "lifecycle-identity.txt").writeText(
            buildString {
                appendLine("analysis_job_id=${jobId.value}")
                appendLine("background_state=${backgroundState?.name ?: "UNKNOWN"}")
                appendLine("implicit_cancel=false")
            },
        )
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
        const val BACKGROUND_FIXTURE_LINES = 400
    }
}
