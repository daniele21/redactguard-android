package io.github.daniele21.redactguard.domain.analysis

internal sealed interface JsonValue {
    data class ObjectValue(
        val fields: Map<String, JsonValue>,
    ) : JsonValue

    data class ArrayValue(
        val values: List<JsonValue>,
    ) : JsonValue

    data class StringValue(
        val value: String,
    ) : JsonValue

    data class IntegerValue(
        val value: Long,
    ) : JsonValue

    data class BooleanValue(
        val value: Boolean,
    ) : JsonValue

    data object NullValue : JsonValue
}

internal enum class JsonFailureCode {
    INPUT_TOO_LARGE,
    INVALID_JSON,
    DEPTH_EXCEEDED,
    CONTAINER_TOO_LARGE,
    STRING_TOO_LARGE,
    DUPLICATE_KEY,
}

internal class StrictJsonException(
    val code: JsonFailureCode,
) : IllegalArgumentException("Invalid RedactGuard JSON: $code")

/** Dependency-free bounded parser for untrusted model output. No repair or permissive coercion is allowed. */
internal class StrictJsonReader(
    private val maxInputCharacters: Int = MAX_INPUT_CHARACTERS,
    private val maxDepth: Int = MAX_DEPTH,
    private val maxContainerEntries: Int = MAX_CONTAINER_ENTRIES,
    private val maxStringCharacters: Int = MAX_STRING_CHARACTERS,
) {
    fun parse(input: String): JsonValue {
        if (input.length > maxInputCharacters) fail(JsonFailureCode.INPUT_TOO_LARGE)
        val cursor = JsonCursor(input)
        val value = parseValue(cursor, 0)
        cursor.skipWhitespace()
        if (!cursor.isAtEnd()) fail()
        return value
    }

    private fun parseValue(
        cursor: JsonCursor,
        depth: Int,
    ): JsonValue {
        if (depth > maxDepth) fail(JsonFailureCode.DEPTH_EXCEEDED)
        cursor.skipWhitespace()
        val next = cursor.peek()
        return when {
            next == '{' -> parseObject(cursor, depth + 1)
            next == '[' -> parseArray(cursor, depth + 1)
            next == '"' -> JsonValue.StringValue(parseString(cursor))
            next == 't' -> cursor.consumeLiteral("true", JsonValue.BooleanValue(true))
            next == 'f' -> cursor.consumeLiteral("false", JsonValue.BooleanValue(false))
            next == 'n' -> cursor.consumeLiteral("null", JsonValue.NullValue)
            next == '-' || next?.isDigit() == true -> JsonValue.IntegerValue(parseInteger(cursor))
            else -> fail()
        }
    }

    private fun parseObject(
        cursor: JsonCursor,
        depth: Int,
    ): JsonValue.ObjectValue {
        cursor.expect('{')
        cursor.skipWhitespace()
        if (cursor.consumeIf('}')) return JsonValue.ObjectValue(emptyMap())
        val fields = linkedMapOf<String, JsonValue>()
        while (true) {
            if (fields.size >= maxContainerEntries) fail(JsonFailureCode.CONTAINER_TOO_LARGE)
            cursor.skipWhitespace()
            if (cursor.peek() != '"') fail()
            val key = parseString(cursor)
            if (key in fields) fail(JsonFailureCode.DUPLICATE_KEY)
            cursor.skipWhitespace()
            cursor.expect(':')
            fields[key] = parseValue(cursor, depth)
            cursor.skipWhitespace()
            when {
                cursor.consumeIf('}') -> return JsonValue.ObjectValue(fields)
                cursor.consumeIf(',') -> Unit
                else -> fail()
            }
        }
    }

    private fun parseArray(
        cursor: JsonCursor,
        depth: Int,
    ): JsonValue.ArrayValue {
        cursor.expect('[')
        cursor.skipWhitespace()
        if (cursor.consumeIf(']')) return JsonValue.ArrayValue(emptyList())
        val values = mutableListOf<JsonValue>()
        while (true) {
            if (values.size >= maxContainerEntries) fail(JsonFailureCode.CONTAINER_TOO_LARGE)
            values += parseValue(cursor, depth)
            cursor.skipWhitespace()
            when {
                cursor.consumeIf(']') -> return JsonValue.ArrayValue(values)
                cursor.consumeIf(',') -> Unit
                else -> fail()
            }
        }
    }

    private fun parseString(cursor: JsonCursor): String {
        cursor.expect('"')
        val value = StringBuilder()
        while (true) {
            val character = cursor.take() ?: fail()
            when {
                character == '"' -> return value.toString()
                character == '\\' -> appendEscaped(cursor, value)
                character.code < 0x20 -> fail()
                else -> value.append(character)
            }
            if (value.length > maxStringCharacters) fail(JsonFailureCode.STRING_TOO_LARGE)
        }
    }

    private fun appendEscaped(
        cursor: JsonCursor,
        value: StringBuilder,
    ) {
        when (val escape = cursor.take()) {
            '"', '\\', '/' -> value.append(escape)
            'b' -> value.append('\b')
            'f' -> value.append('\u000C')
            'n' -> value.append('\n')
            'r' -> value.append('\r')
            't' -> value.append('\t')
            'u' -> appendUnicodeEscape(cursor, value)
            else -> fail()
        }
    }

    private fun appendUnicodeEscape(
        cursor: JsonCursor,
        value: StringBuilder,
    ) {
        val first = readHexCodeUnit(cursor)
        if (first.isHighSurrogate()) {
            cursor.expect('\\')
            cursor.expect('u')
            val second = readHexCodeUnit(cursor)
            if (!second.isLowSurrogate()) fail()
            value.append(first).append(second)
        } else {
            if (first.isLowSurrogate()) fail()
            value.append(first)
        }
    }

    private fun readHexCodeUnit(cursor: JsonCursor): Char {
        var code = 0
        repeat(4) {
            val digit = cursor.take()?.digitToIntOrNull(16) ?: fail()
            code = code * 16 + digit
        }
        return code.toChar()
    }

    private fun parseInteger(cursor: JsonCursor): Long {
        val start = cursor.position
        cursor.consumeIf('-')
        when (val first = cursor.peek()) {
            '0' -> cursor.take()
            in '1'..'9' -> cursor.consumeDigits()
            else -> fail()
        }
        if (cursor.peek() in listOf('.', 'e', 'E')) fail()
        return cursor.substring(start, cursor.position).toLongOrNull() ?: fail()
    }

    private companion object {
        const val MAX_INPUT_CHARACTERS = 262_144
        const val MAX_DEPTH = 8
        const val MAX_CONTAINER_ENTRIES = 512
        const val MAX_STRING_CHARACTERS = 4_096
    }
}

private class JsonCursor(
    private val input: String,
) {
    var position: Int = 0
        private set

    fun isAtEnd() = position == input.length

    fun peek(): Char? = input.getOrNull(position)

    fun take(): Char? = input.getOrNull(position)?.also { position += 1 }

    fun consumeIf(expected: Char): Boolean =
        if (peek() == expected) {
            position += 1
            true
        } else {
            false
        }

    fun expect(expected: Char) {
        if (!consumeIf(expected)) fail()
    }

    fun skipWhitespace() {
        while (peek() in listOf(' ', '\n', '\r', '\t')) position += 1
    }

    fun consumeDigits() {
        while (peek()?.isDigit() == true) position += 1
    }

    fun consumeLiteral(
        literal: String,
        value: JsonValue,
    ): JsonValue {
        literal.forEach(::expect)
        return value
    }

    fun substring(
        start: Int,
        end: Int,
    ) = input.substring(start, end)
}

private fun fail(code: JsonFailureCode = JsonFailureCode.INVALID_JSON): Nothing = throw StrictJsonException(code)
