package io.github.daniele21.redactguard.domain.analysis

import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.document.SegmentId
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiDefinitionSource
import io.github.daniele21.redactguard.domain.pii.PiiTypeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AnalysisResultValidationTest {
    private val email =
        PiiDefinition(
            PiiTypeId.parse("email"),
            "Email",
            "Personal email address",
            null,
            PiiDefinitionSource.BUILT_IN,
        )

    @Test
    fun `parser accepts exact schema and rejects trailing prose`() {
        val parsed = AnalysisResultParser.parse("""{"schemaVersion":1,"findings":[{"typeId":"email","surface":"a@example.test","segmentId":"p0001-b0001"}]}""")
        assertEquals(1, parsed.size)
        assertEquals("a@example.test", parsed.single().surface)

        val error =
            assertThrows(AnalysisResultException::class.java) {
                AnalysisResultParser.parse("""{"schemaVersion":1,"findings":[]} explanation""")
            }
        assertEquals(AnalysisResultFailureCode.MALFORMED_JSON, error.code)
    }

    @Test
    fun `fragment finding maps to exact canonical source offset without searching sibling text`() {
        val canonical = segment("same same alice@example.test end")
        val first = AnalysisSegmentData("p0001-b0001-f0001", "same same ")
        val second = AnalysisSegmentData("p0001-b0001-f0002", "alice@example.test end")
        val chunks =
            listOf(
                AnalysisChunk(0, listOf(first), "payload-1"),
                AnalysisChunk(1, listOf(second), "payload-2"),
            )
        val result =
            FindingValidator.validate(
                listOf(UnvalidatedFinding("email", "alice@example.test", second.segmentId)),
                chunks,
                listOf(canonical),
                listOf(email),
            ) as FindingValidationResult.Valid

        val finding = result.findings.single()
        assertEquals(10, finding.source.range.startInclusive)
        assertEquals(28, finding.source.range.endExclusive)
        assertEquals(canonical.id, finding.source.segmentId)
    }

    @Test
    fun `repeated surface inside one model-visible segment is ambiguous`() {
        val canonical = segment("a@example.test and a@example.test")
        val visible = AnalysisSegmentData(canonical.id.value, canonical.normalizedText)
        val result =
            FindingValidator.validate(
                listOf(UnvalidatedFinding("email", "a@example.test", visible.segmentId)),
                listOf(AnalysisChunk(0, listOf(visible), "payload")),
                listOf(canonical),
                listOf(email),
            )
        assertEquals(
            FindingValidationResult.Rejected(FindingValidationFailureCode.AMBIGUOUS_SURFACE_IN_SEGMENT),
            result,
        )
    }

    @Test
    fun `fragment source index fails closed when fragments do not reconstruct canonical text`() {
        val canonical = segment("abcdef")
        val result =
            FindingValidator.validate(
                emptyList(),
                listOf(
                    AnalysisChunk(
                        0,
                        listOf(
                            AnalysisSegmentData("p0001-b0001-f0001", "abc"),
                            AnalysisSegmentData("p0001-b0001-f0002", "XYZ"),
                        ),
                        "payload",
                    ),
                ),
                listOf(canonical),
                listOf(email),
            )
        assertEquals(FindingValidationResult.Rejected(FindingValidationFailureCode.SOURCE_INDEX_MISMATCH), result)
    }

    private fun segment(text: String) =
        DocumentSegment(
            id = SegmentId.fromIndices(0, 0),
            pageIndex = 0,
            blockIndex = 0,
            normalizedText = text,
        )
}
