package io.github.daniele21.redactguard.infrastructure.document

import io.github.daniele21.redactguard.domain.document.PdfPageText
import io.github.daniele21.redactguard.domain.document.PdfReadResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentIngestionConvergenceTest {
    @Test
    fun `equivalent PDF text and pasted text produce the same canonical segments`() =
        runBlocking {
            val sourceRef = DocumentSourceRef(1)
            val source = DocumentSource("file:///synthetic.pdf", "synthetic.pdf")
            val text = "Mario Rossi\r\nEmail mario.rossi@example.test\r\n\r\nTelefono +39 333 1234567"
            val pdfDocument =
                AndroidDocumentExtractor(
                    sourceResolver = DocumentSourceResolver { if (it == sourceRef) source else null },
                    reader = PdfTextReader { PdfReadResult(1, listOf(PdfPageText(0, text)), truncated = false) },
                ).extract(sourceRef)
            val pastedDocument = PlainTextDocumentExtractor.extract(text)

            assertEquals(pdfDocument.segments, pastedDocument.segments)
            assertEquals(1, pdfDocument.descriptor.pageCount)
            assertEquals(1, pastedDocument.descriptor.pageCount)
        }
}
