package io.github.daniele21.redactguard

import android.app.Application
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.redactguard.ui.ProductStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

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

        viewModel.importText("Ada Lovelace lives at 1 Test Street. Contact ada@example.test for this synthetic fixture.")
        await("pasted text definitions") {
            viewModel.uiState.value.step == ProductStep.DEFINITIONS &&
                viewModel.uiState.value.definitions.isNotEmpty()
        }

        val firstChoice = viewModel.uiState.value.definitions.first()
        if (!firstChoice.selected) viewModel.toggleDefinition(firstChoice.id)
        assertTrue(viewModel.uiState.value.definitions.any { it.selected })
        assertTrue(viewModel.uiState.value.connection.analysisReady)

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

    private fun await(label: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
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
