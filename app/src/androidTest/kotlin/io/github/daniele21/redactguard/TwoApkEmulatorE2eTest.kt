package io.github.daniele21.redactguard

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobState
import io.github.daniele21.redactguard.ui.ProductStep
import java.io.FileInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

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

    private fun createViewModel(
        store: ViewModelStore,
        application: Application,
    ): RedactGuardProductViewModel =
        ViewModelProvider(
            store,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application),
        )[RedactGuardProductViewModel::class.java]

    private fun prepareSyntheticAnalysis(viewModel: RedactGuardProductViewModel) {
        viewModel.importText("Ada Lovelace lives at 1 Test Street. Contact ada@example.test for this synthetic fixture.")
        await("pasted text definitions") {
            viewModel.uiState.value.step == ProductStep.DEFINITIONS &&
                viewModel.uiState.value.definitions.isNotEmpty()
        }

        val firstChoice = viewModel.uiState.value.definitions.first()
        if (!firstChoice.selected) viewModel.toggleDefinition(firstChoice.id)
        assertTrue(viewModel.uiState.value.definitions.any { it.selected })
        assertTrue(viewModel.uiState.value.connection.analysisReady)
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
        const val ANALYSIS_TIMEOUT_MS = 20_000L
    }
}
