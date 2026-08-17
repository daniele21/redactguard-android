package io.github.daniele21.redactguard.infrastructure.document

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import io.github.daniele21.redactguard.domain.document.DocumentDescriptor
import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.document.SegmentId
import io.github.daniele21.redactguard.domain.redaction.RedactionPlan
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream

internal data class PdfExportReceipt(
    val pageCount: Int,
    val byteCount: Long,
) {
    init {
        require(pageCount > 0) { "pageCount must be positive" }
        require(byteCount > 0) { "byteCount must be positive" }
    }
}

internal enum class PdfExportFailureCode {
    DESTINATION_UNWRITABLE,
    SOURCE_MISMATCH,
    OUTPUT_LIMIT_EXCEEDED,
    WRITER_FAILED,
}

internal class PdfExportException(
    val code: PdfExportFailureCode,
) : RuntimeException("RedactGuard PDF export failed: $code")

/** Pure page composition. The source PDF itself is never copied into the output document. */
internal object RedactedPageComposer {
    fun compose(
        descriptor: DocumentDescriptor,
        segments: List<DocumentSegment>,
        plan: RedactionPlan,
    ): List<String> {
        val rendered = plan.renderedSegments.associate { it.segmentId to it.text }
        if (rendered.keys != segments.map(DocumentSegment::id).toSet()) {
            throw PdfExportException(PdfExportFailureCode.SOURCE_MISMATCH)
        }
        val byPage = segments.groupBy(DocumentSegment::pageIndex)
        return (0 until descriptor.pageCount).map { pageIndex ->
            byPage[pageIndex]
                .orEmpty()
                .sortedBy(DocumentSegment::blockIndex)
                .joinToString(separator = "\n\n") { segment ->
                    rendered[segment.id] ?: throw PdfExportException(PdfExportFailureCode.SOURCE_MISMATCH)
                }
        }
    }
}

/** Android SAF adapter that writes a newly generated normalized PDF and deletes partial output best-effort on failure. */
internal class AndroidRedactedPdfExporter(
    context: Context,
    private val writer: NormalizedPdfWriter = NormalizedPdfWriter(),
) {
    private val applicationContext = context.applicationContext

    fun export(
        destination: Uri,
        descriptor: DocumentDescriptor,
        segments: List<DocumentSegment>,
        plan: RedactionPlan,
    ): PdfExportReceipt {
        val pages = RedactedPageComposer.compose(descriptor, segments, plan)
        return try {
            val rawOutput =
                applicationContext.contentResolver.openOutputStream(destination, "wt")
                    ?: throw PdfExportException(PdfExportFailureCode.DESTINATION_UNWRITABLE)
            val output = CountingOutputStream(rawOutput)
            output.use { writer.write(pages, it) }
            if (output.byteCount <= 0L) throw PdfExportException(PdfExportFailureCode.WRITER_FAILED)
            PdfExportReceipt(pageCount = pages.size, byteCount = output.byteCount)
        } catch (failure: Throwable) {
            runCatching { applicationContext.contentResolver.delete(destination, null, null) }
            throw mapExportFailure(failure)
        }
    }

    private fun mapExportFailure(failure: Throwable): Throwable =
        when (failure) {
            is PdfExportException -> failure
            is SecurityException, is IOException -> PdfExportException(PdfExportFailureCode.DESTINATION_UNWRITABLE)
            is IllegalArgumentException -> PdfExportException(PdfExportFailureCode.OUTPUT_LIMIT_EXCEEDED)
            else -> PdfExportException(PdfExportFailureCode.WRITER_FAILED)
        }
}

internal class NormalizedPdfWriter {
    fun write(
        pages: List<String>,
        output: OutputStream,
        maxCharacters: Int = DEFAULT_MAX_CHARACTERS,
    ) {
        require(pages.isNotEmpty()) { "At least one page is required" }
        require(pages.size <= MAX_PAGES) { "Too many pages" }
        require(maxCharacters > 0) { "maxCharacters must be positive" }
        require(pages.sumOf(String::length) <= maxCharacters) { "Document text exceeds export bound" }

        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textSize = TEXT_SIZE_POINTS
            }
        val document = PdfDocument()
        try {
            pages.forEachIndexed { pageIndex, text ->
                requireSupportedGlyphs(text, paint)
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_POINTS, PAGE_HEIGHT_POINTS, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                try {
                    drawText(text, paint, page.canvas)
                } finally {
                    document.finishPage(page)
                }
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun drawText(
        text: String,
        paint: Paint,
        canvas: android.graphics.Canvas,
    ) {
        var y = TOP_MARGIN_POINTS + TEXT_SIZE_POINTS
        text.lineSequence().forEach { sourceLine ->
            wrapLine(sourceLine, paint).forEach { line ->
                require(y <= PAGE_HEIGHT_POINTS - BOTTOM_MARGIN_POINTS) { "Page text exceeds normalized page height" }
                canvas.drawText(line, LEFT_MARGIN_POINTS, y, paint)
                y += LINE_HEIGHT_POINTS
            }
        }
    }

    private fun wrapLine(
        source: String,
        paint: Paint,
    ): List<String> {
        if (source.isEmpty()) return listOf("")
        val width = PAGE_WIDTH_POINTS - LEFT_MARGIN_POINTS - RIGHT_MARGIN_POINTS
        val lines = mutableListOf<String>()
        var remaining = source
        while (remaining.isNotEmpty()) {
            val count = paint.breakText(remaining, true, width, null).coerceAtLeast(1)
            if (count >= remaining.length) {
                lines += remaining
                break
            }
            val candidate = remaining.take(count)
            val breakAt = candidate.lastIndexOf(' ').takeIf { it > 0 } ?: count
            lines += remaining.take(breakAt).trimEnd()
            remaining = remaining.drop(breakAt).trimStart()
        }
        return lines
    }

    private fun requireSupportedGlyphs(
        text: String,
        paint: Paint,
    ) {
        text.codePoints().forEach { codePoint ->
            val value = String(Character.toChars(codePoint))
            require(value == "\n" || value == "\r" || paint.hasGlyph(value)) { "Unsupported glyph" }
        }
    }

    private companion object {
        const val PAGE_WIDTH_POINTS = 595
        const val PAGE_HEIGHT_POINTS = 842
        const val LEFT_MARGIN_POINTS = 48f
        const val RIGHT_MARGIN_POINTS = 48f
        const val TOP_MARGIN_POINTS = 48f
        const val BOTTOM_MARGIN_POINTS = 48f
        const val TEXT_SIZE_POINTS = 11f
        const val LINE_HEIGHT_POINTS = 15f
        const val MAX_PAGES = 200
        const val DEFAULT_MAX_CHARACTERS = 1_000_000
    }
}

private class CountingOutputStream(
    output: OutputStream,
) : FilterOutputStream(output) {
    var byteCount: Long = 0
        private set

    override fun write(value: Int) {
        out.write(value)
        byteCount += 1
    }

    override fun write(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ) {
        out.write(buffer, offset, length)
        byteCount += length
    }
}
