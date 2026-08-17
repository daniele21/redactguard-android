package io.github.daniele21.redactguard.domain.document

internal data class PdfPageText(val pageIndex: Int, val text: String) {
    init {
        require(pageIndex >= 0) { "pageIndex must be non-negative" }
    }

    override fun toString(): String = "PdfPageText(pageIndex=$pageIndex, text=<redacted>)"
}

internal data class PdfReadResult(val pageCount: Int, val pages: List<PdfPageText>, val truncated: Boolean) {
    init {
        require(pageCount >= 0) { "pageCount must be non-negative" }
        require(pages.all { it.pageIndex < pageCount }) { "Returned PDF page is outside document bounds" }
        require(pages.map(PdfPageText::pageIndex).distinct().size == pages.size) { "Duplicate PDF page index" }
    }

    override fun toString(): String =
        "PdfReadResult(pageCount=$pageCount, returnedPages=${pages.size}, truncated=$truncated)"
}

/** Pure deterministic mapping from extracted page text to stable RedactGuard source segments. */
internal object PdfSegmenter {
    fun segment(pages: List<PdfPageText>): List<DocumentSegment> = buildList {
        pages.sortedBy(PdfPageText::pageIndex).forEach { page ->
            normalizePage(page.text).forEachIndexed { blockIndex, block ->
                require(blockIndex < MAX_BLOCKS_PER_PAGE) { "PDF page exceeds stable block identity range" }
                add(
                    DocumentSegment(
                        id = SegmentId.fromIndices(page.pageIndex, blockIndex),
                        pageIndex = page.pageIndex,
                        blockIndex = blockIndex,
                        normalizedText = block,
                    ),
                )
            }
        }
    }

    private fun normalizePage(text: String): List<String> {
        require(text.none(::isUnsupportedControl)) { "Extracted PDF text contains unsupported control characters" }
        val normalizedLines = text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lineSequence()
            .map(String::trimEnd)
            .toList()
        val blocks = mutableListOf<String>()
        val current = mutableListOf<String>()

        fun flush() {
            val block = current.joinToString("\n").trim()
            if (block.isNotEmpty()) blocks += block
            current.clear()
        }

        normalizedLines.forEach { line ->
            if (line.isBlank()) flush() else current += line
        }
        flush()
        return blocks
    }

    private fun isUnsupportedControl(character: Char): Boolean =
        character == '\u0000' ||
            (Character.isISOControl(character) && character != '\n' && character != '\r' && character != '\t')

    private const val MAX_BLOCKS_PER_PAGE = 9_999
}
