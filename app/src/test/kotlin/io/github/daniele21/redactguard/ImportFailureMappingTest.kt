package io.github.daniele21.redactguard

import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import io.github.daniele21.redactguard.infrastructure.document.DocumentExtractionException
import io.github.daniele21.redactguard.infrastructure.document.DocumentExtractionFailureCode
import io.github.daniele21.redactguard.infrastructure.document.PlainTextExtractionFailureCode
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportFailureMappingTest {
    @Test
    fun `every extraction failure preserves a distinct canonical cause`() {
        val mapped = DocumentExtractionFailureCode.entries.associateWith { ImportFailureMapper.fromExtractionCode(it).kind }

        assertEquals(ProductFailureKind.SOURCE_NOT_FOUND, mapped[DocumentExtractionFailureCode.SOURCE_NOT_FOUND])
        assertEquals(ProductFailureKind.SOURCE_UNREADABLE, mapped[DocumentExtractionFailureCode.SOURCE_UNREADABLE])
        assertEquals(ProductFailureKind.ENCRYPTED_PDF, mapped[DocumentExtractionFailureCode.ENCRYPTED_PDF])
        assertEquals(ProductFailureKind.MALFORMED_PDF, mapped[DocumentExtractionFailureCode.MALFORMED_PDF])
        assertEquals(ProductFailureKind.PARSER_FAILED, mapped[DocumentExtractionFailureCode.PARSER_FAILED])
        assertEquals(ProductFailureKind.LIMIT_EXCEEDED, mapped[DocumentExtractionFailureCode.LIMIT_EXCEEDED])
        assertEquals(ProductFailureKind.EMPTY_PDF, mapped[DocumentExtractionFailureCode.EMPTY_PDF])
        assertEquals(ProductFailureKind.IMAGE_ONLY_PDF, mapped[DocumentExtractionFailureCode.IMAGE_ONLY_PDF])
        assertEquals(DocumentExtractionFailureCode.entries.size, mapped.values.toSet().size)
    }

    @Test
    fun `image only PDF is RG PDF 008 and cannot collapse into a generic unsupported bucket`() {
        val failure = ImportFailureMapper.fromExtractionCode(DocumentExtractionFailureCode.IMAGE_ONLY_PDF)

        assertEquals("RG-PDF-008", failure.code)
        assertEquals(ProductFailureKind.IMAGE_ONLY_PDF, failure.kind)
    }

    @Test
    fun `pasted text failures use their own stable namespace`() {
        assertEquals(
            "RG-TXT-001",
            ImportFailureMapper.fromPlainTextCode(PlainTextExtractionFailureCode.EMPTY_TEXT).code,
        )
        assertEquals(
            "RG-TXT-002",
            ImportFailureMapper.fromPlainTextCode(PlainTextExtractionFailureCode.LIMIT_EXCEEDED).code,
        )
        assertEquals(
            "RG-TXT-003",
            ImportFailureMapper.fromPlainTextCode(PlainTextExtractionFailureCode.INVALID_TEXT).code,
        )
    }

    @Test
    fun `safe parser identities survive to technical diagnostics`() {
        val failure =
            ImportFailureMapper.fromThrowable(
                DocumentExtractionException(
                    code = DocumentExtractionFailureCode.PARSER_FAILED,
                    parserErrorType = "IOException",
                    parserStep = "LOAD_DOCUMENT",
                ),
                operationId = "op-1",
            )

        assertEquals(ProductFailureKind.PARSER_FAILED, failure.kind)
        assertEquals("LOAD_DOCUMENT", failure.diagnostic?.step)
        assertEquals("IOException", failure.diagnostic?.type)
    }

    @Test
    fun `unexpected import failure uses explicit system fallback`() {
        val failure = ImportFailureMapper.fromThrowable(IllegalStateException("unexpected"))

        assertEquals(ProductFailureKind.UNKNOWN_INTERNAL, failure.kind)
        assertEquals("RG-SYS-001", failure.code)
    }
}
