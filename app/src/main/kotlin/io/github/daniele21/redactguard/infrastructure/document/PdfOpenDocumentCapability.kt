package io.github.daniele21.redactguard.infrastructure.document

import android.content.Intent
import android.net.Uri

/** Read-only PDF picker capability. Persistable/write grants are intentionally not requested. */
internal class PdfOpenDocumentCapability(
    private val sourceRegistry: DocumentSourceRegistry,
) {
    fun createIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = DocumentSourceRegistry.PDF_MIME_TYPE
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    fun registerResult(uri: Uri?): DocumentSourceRef? {
        if (uri == null) return null
        require(uri.scheme == "content") { "OpenDocument must return a content Uri" }
        return sourceRegistry.register(uri)
    }
}
