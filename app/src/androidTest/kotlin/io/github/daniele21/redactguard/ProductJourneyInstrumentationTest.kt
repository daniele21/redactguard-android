package io.github.daniele21.redactguard

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.redactguard.domain.analysis.AnalysisChunk
import io.github.daniele21.redactguard.domain.analysis.AnalysisLimits
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimePort
import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisRequest
import io.github.daniele21.redactguard.domain.analysis.SequentialDocumentAnalyzer
import io.github.daniele21.redactguard.domain.analysis.ValidatedFinding
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.RedactGuardBuiltInPiiDefinitions
import io.github.daniele21.redactguard.domain.redaction.OccurrenceId
import io.github.daniele21.redactguard.domain.redaction.RedactionPlan
import io.github.daniele21.redactguard.domain.redaction.RedactionPlanResult
import io.github.daniele21.redactguard.domain.redaction.RedactionPlanner
import io.github.daniele21.redactguard.domain.redaction.ReviewOccurrence
import io.github.daniele21.redactguard.infrastructure.document.AndroidDocumentExtractor
import io.github.daniele21.redactguard.infrastructure.document.AndroidRedactedPdfExporter
import io.github.daniele21.redactguard.infrastructure.document.DocumentSourceRef
import io.github.daniele21.redactguard.infrastructure.document.DocumentSourceRegistry
import io.github.daniele21.redactguard.infrastructure.document.ExtractedDocument
import io.github.daniele21.redactguard.infrastructure.document.IsolatedPdfTextReader
import io.github.daniele21.redactguard.infrastructure.document.PlainTextDocumentExtractor
import io.github.daniele21.redactguard.ui.ProductFailureProjector
import io.github.daniele21.redactguard.ui.ProductRetryTarget
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
internal class ProductJourneyInstrumentationTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun pastedTextJourneyAnalyzesReviewsExportsAndReopensSanitizedPdf() =
        runBlocking {
            val output = tempFile("pasted-output", ".pdf")
            try {
                val extracted = PlainTextDocumentExtractor.extract(SYNTHETIC_TEXT)
                val findings = analyze(extracted, DeterministicAnalysisRuntime()).getOrThrow()
                assertEquals(1, findings.size)

                val plan = acceptedPlan(extracted, findings)
                val receipt =
                    AndroidRedactedPdfExporter(context).export(
                        Uri.fromFile(output),
                        extracted.descriptor,
                        extracted.segments,
                        plan,
                    )
                assertTrue(receipt.byteCount > 0)
                assertEquals(1, receipt.pageCount)

                val reopened = IsolatedPdfTextReader(context).read(Uri.fromFile(output).toString())
                assertEquals(1, reopened.pageCount)
                assertFalse(reopened.pages.joinToString("\n") { it.text }.contains(SURFACE))
            } finally {
                output.delete()
            }
            assertFalse(output.exists())
        }

    @Test
    fun textPdfJourneyImportsThroughIsolatedParserAnalyzesExportsAndReopens() =
        runBlocking {
            val source = tempFile("source", ".pdf")
            val output = tempFile("pdf-output", ".pdf")
            val registry = DocumentSourceRegistry(context)
            var sourceRef: DocumentSourceRef? = null
            try {
                writeTextPdf(source, SYNTHETIC_TEXT)
                val registeredSourceRef = registry.register(Uri.fromFile(source))
                sourceRef = registeredSourceRef
                val extracted = AndroidDocumentExtractor(registry, IsolatedPdfTextReader(context)).extract(registeredSourceRef)
                assertEquals(1, extracted.descriptor.pageCount)
                assertTrue(extracted.segments.any { it.normalizedText.contains(SURFACE) })

                val findings = analyze(extracted, DeterministicAnalysisRuntime()).getOrThrow()
                assertEquals(1, findings.size)
                val plan = acceptedPlan(extracted, findings)
                val receipt =
                    AndroidRedactedPdfExporter(context).export(
                        Uri.fromFile(output),
                        extracted.descriptor,
                        extracted.segments,
                        plan,
                    )
                assertTrue(receipt.byteCount > 0)

                val reopened = IsolatedPdfTextReader(context).read(Uri.fromFile(output).toString())
                assertEquals(extracted.descriptor.pageCount, reopened.pageCount)
                val reopenedText = reopened.pages.joinToString("\n") { it.text }
                assertFalse(reopenedText.contains(SURFACE))
            } finally {
                sourceRef?.let(registry::release)
                registry.close()
                source.delete()
                output.delete()
            }
            assertFalse(source.exists())
            assertFalse(output.exists())
        }

    @Test
    fun localAiUnavailableProjectsActionableRecoveryAndRetrySucceeds() {
        val extracted = PlainTextDocumentExtractor.extract(SYNTHETIC_TEXT)
        val runtime = DeterministicAnalysisRuntime(available = false)

        val first = analyze(extracted, runtime)
        assertTrue(first.isFailure)
        val failure = AnalysisFailureMapper.fromThrowable(first.exceptionOrNull()!!, "emulator-e2e-recovery")
        assertEquals(ProductFailureKind.HOST_UNAVAILABLE, failure.kind)
        val projected = ProductFailureProjector.project(failure)
        assertEquals(ProductRetryTarget.ANALYSIS, projected.retryTarget)
        assertTrue(projected.message.isNotBlank())

        runtime.available = true
        val retry = analyze(extracted, runtime).getOrThrow()
        assertEquals(1, retry.size)
        assertEquals(SURFACE, retry.single().surface)
    }

    private fun analyze(
        extracted: ExtractedDocument,
        runtime: AnalysisRuntimePort,
    ): Result<List<ValidatedFinding>> {
        var result: Result<List<ValidatedFinding>>? = null
        SequentialDocumentAnalyzer(runtime).analyze(
            operationId = AnalysisOperationId("emulator-e2e-${UUID.randomUUID()}"),
            request = DocumentAnalysisRequest(extracted.segments, listOf(EMAIL_DEFINITION)),
        ) { outcome -> result = outcome }
        return checkNotNull(result) { "Deterministic runtime must complete synchronously" }
    }

    private fun acceptedPlan(
        extracted: ExtractedDocument,
        findings: List<ValidatedFinding>,
    ): RedactionPlan {
        val reviewed =
            findings.map { finding ->
                ReviewOccurrence(
                    id = OccurrenceId(finding.typeId, finding.source),
                    surface = finding.surface,
                ).accept()
            }
        val result = RedactionPlanner.build(extracted.segments, listOf(EMAIL_DEFINITION), reviewed)
        assertTrue(result is RedactionPlanResult.Ready)
        return (result as RedactionPlanResult.Ready).plan.also { plan ->
            assertEquals(findings.size, plan.acceptedCount)
            assertTrue(plan.renderedSegments.none { it.text.contains(SURFACE) })
        }
    }

    private fun writeTextPdf(
        file: File,
        text: String,
    ) {
        val document = PdfDocument()
        try {
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            page.canvas.drawText(text, 48f, 72f, Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f })
            document.finishPage(page)
            FileOutputStream(file).use(document::writeTo)
        } finally {
            document.close()
        }
        assertTrue(file.length() > 0)
    }

    private fun tempFile(
        prefix: String,
        suffix: String,
    ): File = File(context.cacheDir, "redactguard-e2e-$prefix-${UUID.randomUUID()}$suffix")

    private class DeterministicAnalysisRuntime(
        var available: Boolean = true,
    ) : AnalysisRuntimePort {
        override fun prepare(
            operationId: AnalysisOperationId,
            onResult: (Result<AnalysisLimits>) -> Unit,
        ) {
            if (available) {
                onResult(Result.success(AnalysisLimits(maxInputCharacters = 20_000, maxJsonSchemaCharacters = 20_000)))
            } else {
                onResult(Result.failure(AnalysisRuntimeException(AnalysisRuntimeFailureCode.HOST_UNAVAILABLE)))
            }
        }

        override fun generate(
            operationId: AnalysisOperationId,
            chunk: AnalysisChunk,
            onResult: (Result<String>) -> Unit,
        ) {
            val findings =
                chunk.segments.mapNotNull { segment ->
                    if (!segment.text.contains(SURFACE)) return@mapNotNull null
                    "{\"typeId\":\"email\",\"surface\":\"$SURFACE\",\"segmentId\":\"${segment.segmentId}\"}"
                }
            onResult(
                Result.success(
                    "{\"schemaVersion\":1,\"findings\":[${findings.joinToString(",")}]}",
                ),
            )
        }

        override fun cancel(
            operationId: AnalysisOperationId,
            onCancelled: () -> Unit,
        ) = onCancelled()

        override fun close(operationId: AnalysisOperationId) = Unit
    }

    private companion object {
        const val SURFACE = "alice@example.test"
        const val SYNTHETIC_TEXT = "Contact $SURFACE for follow up."
        val EMAIL_DEFINITION: PiiDefinition =
            RedactGuardBuiltInPiiDefinitions.all.single { it.id.value == "email" }
    }
}
