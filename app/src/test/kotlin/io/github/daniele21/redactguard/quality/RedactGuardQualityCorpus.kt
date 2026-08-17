package io.github.daniele21.redactguard.quality

import java.security.MessageDigest

internal data class QualityCorpusIdentity(val schemaVersion: Int, val corpusVersion: String, val sha256: String) {
    init {
        require(schemaVersion > 0)
        require(corpusVersion.isNotBlank())
        require(sha256.matches(Regex("[0-9a-f]{64}")))
    }
}

internal enum class QualityCaseTag {
    POSITIVE, NEGATIVE, BUILT_IN, CUSTOM, NO_PII, REPEATED, OVERLAP, NEAR_MISS, INJECTION, ITALIAN_TEXT,
}

internal data class QualitySegment(val id: String, val text: String) {
    init {
        require(id.isNotBlank())
        require(text.startsWith(SYNTHETIC_MARKER))
    }
}

internal data class QualityOccurrence(
    val typeId: String,
    val segmentId: String,
    val startOffset: Int,
    val endOffset: Int,
    val surface: String,
)

internal data class QualityCase(
    val id: String,
    val tags: Set<QualityCaseTag>,
    val selectedTypeIds: Set<String>,
    val segments: List<QualitySegment>,
    val expectedOccurrences: List<QualityOccurrence>,
)

internal data class QualityCorpus(
    val identity: QualityCorpusIdentity,
    val builtInDefinitionSetVersion: Int,
    val customTypeIds: Set<String>,
    val cases: List<QualityCase>,
)

/** Canonical migration copy of the frozen OMBRA v2 corpus. Content identity must not change during extraction. */
internal object RedactGuardSyntheticQualityCorpus {
    const val RESOURCE_PATH = "redactguard-quality/v2/corpus.tsv"
    const val EXPECTED_SHA256 = "a04f79dec42ee4208e4db27512664cc20f66cc863fd80ae4fcdc1019a2f37a5f"

    fun load(): QualityCorpus {
        val bytes = requireNotNull(javaClass.classLoader?.getResourceAsStream(RESOURCE_PATH)) {
            "Missing RedactGuard synthetic quality corpus"
        }.use { it.readBytes() }
        val actualSha256 = sha256(bytes)
        require(actualSha256 == EXPECTED_SHA256) { "Synthetic quality corpus hash changed without explicit version review" }

        val lines = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        val metadata = lines.filter { it.startsWith('#') && !it.startsWith("#caseId") }.associate { line ->
            val separator = line.indexOf('=')
            require(separator > 1)
            line.substring(1, separator) to line.substring(separator + 1)
        }
        require(metadata.getValue("synthetic") == "true")

        val customMetadata = metadata.keys
            .mapNotNull { key -> CUSTOM_METADATA_PATTERN.matchEntire(key)?.let { it.groupValues[1] to it.groupValues[2] } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        require(customMetadata.values.all { it.toSet() == CUSTOM_METADATA_FIELDS })

        return QualityCorpus(
            identity = QualityCorpusIdentity(
                schemaVersion = metadata.getValue("schemaVersion").toInt(),
                corpusVersion = metadata.getValue("corpusVersion"),
                sha256 = actualSha256,
            ),
            builtInDefinitionSetVersion = metadata.getValue("builtInDefinitionSetVersion").toInt(),
            customTypeIds = customMetadata.keys,
            cases = lines.filterNot { it.startsWith('#') }.map(::parseCase),
        )
    }

    private fun parseCase(line: String): QualityCase {
        val columns = line.split('\t')
        require(columns.size == COLUMN_COUNT)
        val segment = QualitySegment(columns[3], columns[4])
        val occurrences = if (columns[5] == "-") emptyList() else columns[5].split(';').map { parseOccurrence(it, segment) }
        return QualityCase(
            id = columns[0],
            tags = columns[1].split(',').mapTo(linkedSetOf()) { QualityCaseTag.valueOf(it) },
            selectedTypeIds = columns[2].split(',').toCollection(linkedSetOf()),
            segments = listOf(segment),
            expectedOccurrences = occurrences,
        )
    }

    private fun parseOccurrence(encoded: String, segment: QualitySegment): QualityOccurrence {
        val fields = encoded.split(':')
        require(fields.size == 3)
        val start = fields[1].toInt()
        val end = fields[2].toInt()
        require(start >= 0 && end > start && end <= segment.text.length)
        return QualityOccurrence(fields[0], segment.id, start, end, segment.text.substring(start, end))
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
        (it.toInt() and 0xff).toString(16).padStart(2, '0')
    }

    private val CUSTOM_METADATA_PATTERN = Regex("custom\\.([a-z0-9-]+)\\.(label|definition)")
    private val CUSTOM_METADATA_FIELDS = setOf("label", "definition")
    private const val COLUMN_COUNT = 6
}

internal const val SYNTHETIC_MARKER = "[DATI SINTETICI]"
