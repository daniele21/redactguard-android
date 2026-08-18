package io.github.daniele21.redactguard.infrastructure.document

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import io.github.daniele21.redactguard.domain.document.PdfPageText
import io.github.daniele21.redactguard.domain.document.PdfReadResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal fun interface PdfTextReader {
    suspend fun read(locator: String): PdfReadResult
}

/** Client for the isolated parser process. Binder carries metadata; extracted text travels through a pipe. */
internal class IsolatedPdfTextReader(
    context: Context,
) : PdfTextReader {
    private val applicationContext = context.applicationContext

    override suspend fun read(locator: String): PdfReadResult {
        val uri = Uri.parse(locator)
        val session = bindParserService()
        var source: ParcelFileDescriptor? = null
        var outputRead: ParcelFileDescriptor? = null
        var outputWrite: ParcelFileDescriptor? = null
        val completion = CompletableDeferred<ParserCompletion>()
        val replyMessenger =
            Messenger(
                Handler(Looper.getMainLooper()) { message ->
                    if (message.what != IsolatedPdfParserService.MESSAGE_COMPLETE) return@Handler false
                    completion.complete(
                        ParserCompletion(
                            result = message.data.getInt(IsolatedPdfParserService.KEY_RESULT),
                            errorType = message.data.getString(IsolatedPdfParserService.KEY_ERROR_TYPE),
                            errorStep = message.data.getString(IsolatedPdfParserService.KEY_ERROR_STEP),
                        ),
                    )
                    true
                },
            )

        try {
            source = openReadOnly(uri)
            val pipe = ParcelFileDescriptor.createPipe()
            outputRead = pipe[0]
            outputWrite = pipe[1]
            session.messenger.send(
                Message.obtain(null, IsolatedPdfParserService.MESSAGE_PARSE).apply {
                    replyTo = replyMessenger
                    data =
                        Bundle().apply {
                            putParcelable(IsolatedPdfParserService.KEY_INPUT, source)
                            putParcelable(IsolatedPdfParserService.KEY_OUTPUT, outputWrite)
                            putInt(IsolatedPdfParserService.KEY_MAX_PAGES, MAX_PAGES)
                            putInt(IsolatedPdfParserService.KEY_MAX_CHARACTERS, MAX_CHARACTERS)
                        }
                },
            )
            source.close()
            source = null
            outputWrite.close()
            outputWrite = null

            val terminal = withTimeout(PARSE_TIMEOUT_MS) { completion.await() }
            if (terminal.result != IsolatedPdfParserService.RESULT_OK) {
                throw PdfParserException(
                    parserErrorType = terminal.errorType ?: "UnknownError",
                    parserStep = terminal.errorStep ?: PdfParserStep.UNKNOWN.name,
                )
            }
            val descriptor = requireNotNull(outputRead)
            val result =
                withContext(Dispatchers.IO) {
                    ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
                        DataInputStream(stream.buffered()).use(::readFrame)
                    }
                }
            outputRead = null
            return result
        } finally {
            source?.close()
            outputRead?.close()
            outputWrite?.close()
            session.unbind()
        }
    }

    private suspend fun bindParserService(): ParserSession =
        suspendCancellableCoroutine { continuation ->
            var bound = false
            lateinit var connection: ServiceConnection
            connection =
                object : ServiceConnection {
                    override fun onServiceConnected(
                        name: ComponentName?,
                        service: IBinder?,
                    ) {
                        if (!continuation.isActive) {
                            if (bound) safeUnbind(connection)
                            return
                        }
                        if (service == null) {
                            continuation.resumeWithException(IOException("Isolated PDF parser connected without Binder"))
                            return
                        }
                        continuation.resume(ParserSession(Messenger(service), connection))
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(IOException("Isolated PDF parser disconnected during bind"))
                        }
                    }

                    override fun onBindingDied(name: ComponentName?) {
                        if (continuation.isActive) continuation.resumeWithException(IOException("Isolated PDF parser binding died"))
                    }
                }
            bound =
                applicationContext.bindService(
                    Intent(applicationContext, IsolatedPdfParserService::class.java),
                    connection,
                    Context.BIND_AUTO_CREATE,
                )
            if (!bound) {
                continuation.resumeWithException(IOException("Unable to bind isolated PDF parser"))
                return@suspendCancellableCoroutine
            }
            continuation.invokeOnCancellation { if (bound) safeUnbind(connection) }
        }

    private fun openReadOnly(uri: Uri): ParcelFileDescriptor {
        if (uri.scheme == "file") {
            val path = uri.path ?: throw IOException("File URI has no path")
            return ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        }
        return applicationContext.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Unable to open PDF source")
    }

    private fun readFrame(input: DataInputStream): PdfReadResult {
        if (input.readInt() != IsolatedPdfParserService.FRAME_MAGIC) throw IOException("Invalid parser frame")
        val pageCount = input.readInt()
        val truncated = input.readBoolean()
        val returnedPages = input.readInt()
        if (pageCount < 0 || returnedPages !in 0..MAX_PAGES) throw IOException("Invalid parser bounds")
        var returnedCharacters = 0
        val pages = ArrayList<PdfPageText>(returnedPages)
        repeat(returnedPages) {
            val pageIndex = input.readInt()
            val byteCount = input.readInt()
            if (pageIndex !in 0 until pageCount || byteCount !in 0..MAX_PAGE_UTF8_BYTES) {
                throw IOException("Invalid parser page frame")
            }
            val bytes = ByteArray(byteCount)
            input.readFully(bytes)
            val text = bytes.toString(Charsets.UTF_8)
            returnedCharacters += text.length
            if (returnedCharacters > MAX_CHARACTERS) throw IOException("Parser exceeded character bound")
            pages += PdfPageText(pageIndex, text)
        }
        return PdfReadResult(pageCount, pages, truncated)
    }

    private fun safeUnbind(connection: ServiceConnection) {
        try {
            applicationContext.unbindService(connection)
        } catch (_: IllegalArgumentException) {
            // Already unbound by cancellation/death cleanup.
        }
    }

    private inner class ParserSession(
        val messenger: Messenger,
        private val connection: ServiceConnection,
    ) {
        fun unbind() = safeUnbind(connection)
    }

    private data class ParserCompletion(
        val result: Int,
        val errorType: String?,
        val errorStep: String?,
    )

    private companion object {
        const val MAX_PAGES = 200
        const val MAX_CHARACTERS = 1_000_000
        const val PARSE_TIMEOUT_MS = 30_000L
        const val MAX_PAGE_UTF8_BYTES = 4_000_000
    }
}

internal enum class PdfParserStep {
    INITIALIZE,
    LOAD_DOCUMENT,
    EXTRACT_TEXT,
    WRITE_RESULT,
    UNKNOWN,
}

internal class PdfParserException(
    val parserErrorType: String,
    val parserStep: String,
) : IOException("Isolated PDF parser failed at $parserStep with $parserErrorType")
