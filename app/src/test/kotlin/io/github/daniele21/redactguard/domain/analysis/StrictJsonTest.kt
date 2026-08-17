package io.github.daniele21.redactguard.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StrictJsonTest {
    private val reader = StrictJsonReader()

    @Test
    fun `parses bounded structured json without repair`() {
        val parsed = reader.parse("{\"schemaVersion\":1,\"findings\":[]}")
        assertTrue(parsed is JsonValue.ObjectValue)
        val objectValue = parsed as JsonValue.ObjectValue
        assertEquals(JsonValue.IntegerValue(1), objectValue.fields["schemaVersion"])
    }

    @Test
    fun `duplicate keys fail closed`() {
        val failure = assertThrows(StrictJsonException::class.java) {
            reader.parse("{\"a\":1,\"a\":2}")
        }
        assertEquals(JsonFailureCode.DUPLICATE_KEY, failure.code)
    }

    @Test
    fun `floating point and trailing prose are rejected`() {
        assertThrows(StrictJsonException::class.java) { reader.parse("{\"value\":1.5}") }
        assertThrows(StrictJsonException::class.java) { reader.parse("{\"value\":1} explanation") }
    }
}
