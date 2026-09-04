package io.github.daniele21.redactguard

import android.app.Application
import android.app.Instrumentation
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.daniele21.localllm.contracts.ConsumerCapabilityResult
import io.github.daniele21.localllm.contracts.ConsumerGenerationInput
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobId
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobResponse
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobSnapshot
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobState
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobRequestId
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobSubmitRequest
import io.github.daniele21.localllm.contracts.ConsumerOutputConstraint
import io.github.daniele21.localllm.contracts.ConsumerOutputConstraintKind
import io.github.daniele21.localllm.contracts.ConsumerPrepareRequest
import io.github.daniele21.localllm.contracts.ConsumerPrepareResult
import io.github.daniele21.localllm.contracts.ConsumerPreparedSelection
import io.github.daniele21.localllm.contracts.ConsumerReasoningPreference
import io.github.daniele21.localllm.contracts.ConsumerSelectionRequest
import io.github.daniele21.localllm.contracts.SessionKind
import io.github.daniele21.localllm.contracts.TaskDefinition
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.localllm.contracts.toExecutionIdentity
import io.github.daniele21.localllm.transport.binder.client.BinderConsumerLocalLlmClient
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionState
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeHostConfig
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobId
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobState
import io.github.daniele21.redactguard.domain.analysis.AnalysisProtocol
import io.github.daniele21.redactguard.ui.ProductStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ProcessLocalProductAnalysisOwner.get(application)
        val fault = HarnessEmulatorE2eFaultControl
        val viewModel = RedactGuardProductViewModel(application)
        var secondaryClient: BinderConsumerLocalLlmClient? = null
        viewModel.connectHarness()
        awaitHarnessReady(viewModel)

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

        fault.resetGenerationGate(application)
        fault.pauseGeneration(application)
        try {
            prepareSyntheticAnalysis(viewModel)
            viewModel.startAnalysis()

            awaitValue("accepted process-local analysis job") {
                owner.currentSnapshot()?.takeIf { snapshot -> snapshot.state == AnalysisJobState.ACTIVE }
            }
            val firstLogicalJobId =
                awaitValue("accepted first Harness logical job") {
                    fault.acceptedLogicalJobId(owner)
                }
            val firstBlocked = fault.awaitGenerationBlocked(application)
            assertTrue(firstBlocked.paused)
            assertEquals(1, firstBlocked.waitingRequests)

            val secondClient = createSecondaryConsumerClient(application).also { secondaryClient = it }
            secondClient.connect()
            await("secondary Consumer Binder registration", READY_TIMEOUT_MS) {
                secondClient.connectionSnapshot.state == SharedRuntimeConnectionState.CONNECTED
            }
            val secondPrepared = prepareSecondaryConsumer(secondClient)
            val secondAccepted = submitSecondarySerializationProbe(secondClient, secondPrepared)
            assertEquals(ConsumerInferenceJobState.PREPARING, secondAccepted.state)
            assertNull(secondAccepted.errorCode)
            SystemClock.sleep(SERIALIZATION_STABILITY_WINDOW_MS)

            val blockedWithSecondAccepted = fault.generationGateStatus(application)
            val secondWhileBlocked = secondaryLogicalJobSnapshot(secondClient, secondAccepted.jobId)
            assertTrue(blockedWithSecondAccepted.paused)
            assertEquals(1, blockedWithSecondAccepted.waitingRequests)
            assertEquals(ConsumerInferenceJobState.PREPARING, secondWhileBlocked.state)
            assertNull(secondWhileBlocked.errorCode)
            recordSerializationTrace(
                "serialized-while-first-blocked",
                "first_job_id=${firstLogicalJobId.value};second_job_id=${secondAccepted.jobId.value};" +
                    "second_state=${secondWhileBlocked.state.name};" +
                    "second_error_code=${secondWhileBlocked.errorCode?.name ?: "none"};" +
                    "host_waiters=${blockedWithSecondAccepted.waitingRequests}",
            )

            fault.releaseGeneration(application)

            val observedProgressTitles = linkedSetOf<String>()
            await("cross-process local analysis", ANALYSIS_TIMEOUT_MS) {
                observedProgressTitles += viewModel.analysisProgress.value.title
                viewModel.uiState.value.step in setOf(ProductStep.REVIEW, ProductStep.NO_FINDINGS, ProductStep.ERROR)
            }
            val secondTerminal =
                awaitValue("second independent Consumer logical job success", ANALYSIS_TIMEOUT_MS) {
                    val snapshot = secondaryLogicalJobSnapshot(secondClient, secondAccepted.jobId)
                    when (snapshot.state) {
                        ConsumerInferenceJobState.SUCCEEDED -> snapshot

                        ConsumerInferenceJobState.FAILED_FINAL,
                        ConsumerInferenceJobState.CANCELLED,
                        ConsumerInferenceJobState.INTERRUPTED,
                        -> throw AssertionError(
                            "Secondary logical job terminated as ${snapshot.state.name}; " +
                                "error=${snapshot.errorCode?.name ?: "none"}",
                        )

                        else -> null
                    }
                }
            val cleaned =
                awaitValue("serialized Host generation cleanup") {
                    fault.generationGateStatus(application).takeIf { status -> status.waitingRequests == 0 }
                }

            assertEquals(ConsumerInferenceJobState.SUCCEEDED, secondTerminal.state)
            assertNull(secondTerminal.errorCode)
            assertEquals(0, cleaned.waitingRequests)
            recordSerializationTrace(
                "serialized-complete",
                "first_job_id=${firstLogicalJobId.value};second_job_id=${secondTerminal.jobId.value};" +
                    "second_state=${secondTerminal.state.name};" +
                    "second_error_code=${secondTerminal.errorCode?.name ?: "none"};" +
                    "host_waiters=${cleaned.waitingRequests}",
            )

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
        } finally {
            runCatching { fault.releaseGeneration(application) }
            runCatching { fault.resetGenerationGate(application) }
            runCatching { secondaryClient?.close() }
        }
    }

    @Test
    fun analysisSurvivesViewModelStoreClearAndReattachesSameProcessJob() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ProcessLocalProductAnalysisOwner.get(application)
        val firstStore = ViewModelStore()
        val first = createViewModel(firstStore, application)
        first.connectHarness()

        awaitHarnessReady(first)
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
        val fault = HarnessEmulatorE2eFaultControl
        viewModel.connectHarness()
        clearLifecycleEvidence(application)

        fault.resetGenerationGate(application)
        fault.pauseGeneration(application)
        try {
            awaitHarnessReady(viewModel)
            prepareSyntheticAnalysis(viewModel)
            viewModel.startAnalysis()

            val accepted =
                awaitValue("accepted background process-local analysis job") {
                    owner.currentSnapshot()?.takeIf { snapshot -> snapshot.state == AnalysisJobState.ACTIVE }
                }
            val acceptedJobId = accepted.jobId
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
            assertEquals(acceptedJobId, backgroundSnapshot?.jobId)
            assertEquals(AnalysisJobState.ACTIVE, backgroundSnapshot?.state)
            assertFalse(
                backgroundSnapshot?.state == AnalysisJobState.CANCEL_REQUESTED ||
                    backgroundSnapshot?.state == AnalysisJobState.CANCELLED,
            )
            writeLifecycleIdentity(
                application = application,
                jobId = acceptedJobId,
                logicalJobId = logicalJobId.value,
                backgroundState = backgroundSnapshot?.state,
                gateWaitingRequests = blocked.waitingRequests,
            )

            launchRedactGuardActivity()
            await("RedactGuard activity resumed after app switch") { redactGuardIsResumed() }
            val reattachedSnapshot = owner.currentSnapshot()
            assertNotNull(reattachedSnapshot)
            assertEquals(acceptedJobId, reattachedSnapshot?.jobId)
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
        } finally {
            runCatching { fault.releaseGeneration(application) }
            runCatching { fault.resetGenerationGate(application) }
            store.clear()
        }
    }

    private fun createSecondaryConsumerClient(application: Application): BinderConsumerLocalLlmClient =
        BinderConsumerLocalLlmClient.create(
            context = application.applicationContext,
            hostConfig =
                SharedRuntimeHostConfig.create(
                    BuildConfig.SHARED_RUNTIME_HOST_PACKAGE,
                    BuildConfig.SHARED_RUNTIME_HOST_SERVICE,
                ),
            clientBuildId = "redactguard-e2e-secondary",
        )

    private fun prepareSecondaryConsumer(client: BinderConsumerLocalLlmClient): ConsumerPreparedSelection {
        val capabilities =
            when (val result = client.capabilities(SECONDARY_USE_CASE)) {
                is ConsumerCapabilityResult.Available -> {
                    result.capabilities
                }

                is ConsumerCapabilityResult.Rejected -> {
                    throw AssertionError("Secondary capabilities rejected: ${result.code.name}")
                }
            }
        val preset =
            capabilities.defaultPreset
                ?: capabilities.presets.singleOrNull()?.ref
                ?: throw AssertionError("Secondary Consumer has no unambiguous default preset")
        val result =
            client.prepare(
                ConsumerPrepareRequest(
                    useCaseId = SECONDARY_USE_CASE,
                    selection =
                        ConsumerSelectionRequest(
                            capabilityRevision = capabilities.capabilityRevision,
                            preset = preset,
                            reasoning = ConsumerReasoningPreference.DISABLED,
                            outputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
                            sessionKind = SessionKind.STATELESS,
                        ),
                ),
            )
        return when (result) {
            is ConsumerPrepareResult.Prepared -> {
                result.selection
            }

            is ConsumerPrepareResult.Rejected -> {
                throw AssertionError("Secondary prepare rejected: ${result.failure.code.name}")
            }
        }
    }

    private fun submitSecondarySerializationProbe(
        client: BinderConsumerLocalLlmClient,
        prepared: ConsumerPreparedSelection,
    ): ConsumerInferenceJobSnapshot {
        val response =
            client.submitLogicalGeneration(
                ConsumerLogicalJobSubmitRequest(
                    clientRequestId = ConsumerLogicalJobRequestId("e2e-serialization-${SystemClock.elapsedRealtime()}"),
                    useCaseId = SECONDARY_USE_CASE,
                    preparedId = prepared.preparedId,
                    expectedExecution = prepared.toExecutionIdentity(),
                    input = ConsumerGenerationInput.Text("Serialization probe contact queue@example.test."),
                    outputConstraint = ConsumerOutputConstraint.JsonSchema(AnalysisProtocol.outputJsonSchema),
                    taskDefinitions =
                        listOf(
                            TaskDefinition(
                                id = "email-address",
                                description = "Detect email addresses in the supplied text.",
                            ),
                        ),
                ),
            )
        return when (response) {
            is ConsumerInferenceJobResponse.Available -> {
                response.snapshot
            }

            is ConsumerInferenceJobResponse.Rejected -> {
                throw AssertionError("Secondary logical submit rejected: ${response.failure.code.name}")
            }
        }
    }

    private fun secondaryLogicalJobSnapshot(
        client: BinderConsumerLocalLlmClient,
        jobId: ConsumerInferenceJobId,
    ): ConsumerInferenceJobSnapshot =
        when (val response = client.logicalJob(jobId, SECONDARY_USE_CASE)) {
            is ConsumerInferenceJobResponse.Available -> {
                response.snapshot
            }

            is ConsumerInferenceJobResponse.Rejected -> {
                throw AssertionError("Secondary logical query rejected: ${response.failure.code.name}")
            }
        }

    private fun recordSerializationTrace(
        stage: String,
        details: String,
    ) {
        val rendered = "RG_MULTI_JOB_SERIALIZATION stage=$stage;$details"
        println(rendered)
        InstrumentationRegistry.getInstrumentation().sendStatus(
            0,
            Bundle().apply { putString(Instrumentation.REPORT_KEY_STREAMRESULT, "$rendered\n") },
        )
    }

    private fun createViewModel(
        store: ViewModelStore,
        application: Application,
    ): RedactGuardProductViewModel =
        ViewModelProvider(
            store,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application),
        )[RedactGuardProductViewModel::class.java]

    private fun awaitHarnessReady(viewModel: RedactGuardProductViewModel) {
        try {
            await("initial Harness readiness", READY_TIMEOUT_MS) {
                viewModel.uiState.value.connection.analysisReady
            }
        } catch (timeout: AssertionError) {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val owner = ProcessLocalProductAnalysisOwner.get(application)
            val runtimeState = owner.connectionState.value
            val negotiated = owner.runtime.connectionSnapshot
            val diagnostics =
                shell("logcat -d -s RG_LOCAL_AI:I '*:S'")
                    .lineSequence()
                    .toList()
                    .takeLast(40)
                    .joinToString(" | ")
            throw AssertionError(
                "Timed out waiting for initial Harness readiness; " +
                    "runtime=$runtimeState negotiatedMinor=${negotiated.negotiatedMinor} " +
                    "features=${negotiated.enabledFeatures.sorted()} ui=${viewModel.uiState.value} " +
                    "preset=${viewModel.presetUiState.value} diagnostics=$diagnostics",
                timeout,
            )
        }
    }

    private fun prepareSyntheticAnalysis(viewModel: RedactGuardProductViewModel) {
        prepareSyntheticAnalysis(
            viewModel,
            "Ada Lovelace lives at 1 Test Street. Contact ada@example.test for this synthetic fixture.",
        )
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
        logicalJobId: String,
        backgroundState: AnalysisJobState?,
        gateWaitingRequests: Int,
    ) {
        val directory = lifecycleEvidenceDirectory(application).apply(File::mkdirs)
        File(directory, "lifecycle-identity.txt").writeText(
            buildString {
                appendLine("analysis_job_id=${jobId.value}")
                appendLine("logical_job_id=$logicalJobId")
                appendLine("background_state=${backgroundState?.name ?: "UNKNOWN"}")
                appendLine("gate_waiting_requests=$gateWaitingRequests")
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
        val SECONDARY_USE_CASE = UseCaseId("document-pii-detection")
        const val POLL_INTERVAL_MS = 50L
        const val DEFAULT_TIMEOUT_MS = 8_000L
        const val READY_TIMEOUT_MS = 15_000L
        const val ANALYSIS_TIMEOUT_MS = 30_000L
        const val SERIALIZATION_STABILITY_WINDOW_MS = 300L
    }
}
