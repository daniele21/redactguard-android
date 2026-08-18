package io.github.daniele21.redactguard.infrastructure.document

import io.github.daniele21.redactguard.domain.document.DocumentDescriptor
import io.github.daniele21.redactguard.domain.document.DocumentSegment

/** Source-neutral application-facing result shared by PDF and pasted-text ingestion. */
internal data class ExtractedDocument(
    val descriptor: DocumentDescriptor,
    val segments: List<DocumentSegment>,
) {
    init {
        require(segments.isNotEmpty()) { "Extracted document must contain canonical segments" }
    }

    override fun toString(): String =
        "ExtractedDocument(pageCount=${descriptor.pageCount}, segmentCount=${segments.size}, displayName=<redacted>)"
}
