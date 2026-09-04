package io.github.daniele21.redactguard.infrastructure.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@JvmInline
internal value class DocumentSourceRef(
    val value: Long,
) {
    init {
        require(value > 0) { "Document source reference must be positive" }
    }
}

internal data class DocumentSource(
    val locator: String,
    val displayName: String,
) {
    init {
        require(locator.isNotBlank()) { "document source locator must not be blank" }
        require(displayName.isNotBlank()) { "displayName must not be blank" }
    }

    override fun toString(): String = "DocumentSource(locator=<redacted>, displayName=<redacted>)"
}

internal fun interface DocumentSourceResolver {
    fun resolve(sourceRef: DocumentSourceRef): DocumentSource?
}

/** Process-local owner of transient Storage Access Framework capabilities. */
internal class DocumentSourceRegistry(
    context: Context,
) : DocumentSourceResolver,
    AutoCloseable {
    private val applicationContext = context.applicationContext
    private val nextOrdinal = AtomicLong(1)
    private val sources = ConcurrentHashMap<Long, DocumentSource>()

    fun register(uri: Uri): DocumentSourceRef {
        require(uri.scheme == "content" || uri.scheme == "file") { "Only content/file document sources are supported" }
        val sourceRef = DocumentSourceRef(nextOrdinal.getAndIncrement())
        val source = DocumentSource(locator = uri.toString(), displayName = resolveDisplayName(uri))
        check(sources.putIfAbsent(sourceRef.value, source) == null) { "Duplicate document source capability" }
        return sourceRef
    }

    override fun resolve(sourceRef: DocumentSourceRef): DocumentSource? = sources[sourceRef.value]

    fun release(sourceRef: DocumentSourceRef): Boolean = sources.remove(sourceRef.value) != null

    override fun close() {
        sources.clear()
    }

    private fun resolveDisplayName(uri: Uri): String {
        if (uri.scheme == "content") {
            try {
                applicationContext.contentResolver
                    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (index >= 0) {
                                cursor.getString(index)?.takeIf(String::isNotBlank)?.let { return it }
                            }
                        }
                    }
            } catch (_: SecurityException) {
                // Display-name lookup is optional; read authorization is checked when extraction starts.
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank) ?: DEFAULT_DISPLAY_NAME
    }

    companion object {
        const val PDF_MIME_TYPE = "application/pdf"
        private const val DEFAULT_DISPLAY_NAME = "document.pdf"
    }
}
