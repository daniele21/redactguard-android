package io.github.daniele21.redactguard.infrastructure.document

import io.github.daniele21.redactguard.domain.document.PdfPageText
import io.github.daniele21.redactguard.domain.document.PdfReadResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AndroidDocumentExtractorTest {
    @Test
    fun `successful extraction maps bounded pages into stable source segments`() =
        runBlocking {
            val sourceRef = DocumentSourceRef(1)
            val source = DocumentSource("file:///synthetic.pdf", "synthetic.pdf")
            val extractor =
                AndroidDocumentExtractor(
                    sourceResolver = DocumentSourceResolver { if (it == sourceRef) source else null },
                    reader =
                        PdfTextReader {
                            PdfReadResult(1, listOf(PdfPageText(0, "First block\n\nSecond block")), truncated = false)
                        },
                )

            val document = extractor.extract(sourceRef)
            assertEquals(1, document.descriptor.pageCount)
            assertEquals(listOf("p0001-b0001", "p0001-b0002"), document.segments.map { it.id.value })
        }

    @Test
    fun `truncation fails closed rather than silently analyzing partial document`() {
        val sourceRef = DocumentSourceRef(1)
        val source = DocumentSource("file:///synthetic.pdf", "synthetic.pdf")
        val extractor =
            AndroidDocumentExtractor(
                sourceResolver = DocumentSourceResolver { source },
                reader = PdfTextReader { PdfReadResult(2, listOf(PdfPageText(0, "partial")), truncated = true) },
            )

        val error =
            assertThrows(DocumentExtractionException::class.java) {
                runBlocking { extractor.extract(sourceRef) }
            }
        assertEquals(DocumentExtractionFailureCode.LIMIT_EXCEEDED, error.code)
    }

    @Test
    fun `generic parser IOException is parser failure not malformed PDF`() {
        val sourceRef = DocumentSourceRef(1)
        val source = DocumentSource("file:///valid-looking.pdf", "valid-looking.pdf")
        val extractor =
            AndroidDocumentExtractor(
                sourceResolver = DocumentSourceResolver { source },
                reader =
                    PdfTextReader {
                        throw PdfParserException(
                            parserErrorType = "IOException",
                            parserStep = PdfParserStep.LOAD_DOCUMENT.name,
                        )
                    },
            )

        val error =
            assertThrows(DocumentExtractionException::class.java) {
                runBlocking { extractor.extract(sourceRef) }
            }

        assertEquals(DocumentExtractionFailureCode.PARSER_FAILED, error.code)
        assertEquals("IOException", error.parserErrorType)
        assertEquals("LOAD_DOCUMENT", error.parserStep)
    }

    @Test
    fun `known password failure remains encrypted PDF`() {
        val sourceRef = DocumentSourceRef(1)
        val source = DocumentSource("file:///encrypted.pdf", "encrypted.pdf")
        val extractor =
            AndroidDocumentExtractor(
                sourceResolver = DocumentSourceResolver { source },
                reader =
                    PdfTextReader {
                        throw PdfParserException(
                            parserErrorType = "InvalidPasswordException",
                            parserStep = PdfParserStep.LOAD_DOCUMENT.name,
                        )
                    },
            )

        val error =
            assertThrows(DocumentExtractionException::class.java) {
                runBlocking { extractor.extract(sourceRef) }
            }

        assertEquals(DocumentExtractionFailureCode.ENCRYPTED_PDF, error.code)
    }
}
