package io.github.daniele21.redactguard.infrastructure.document

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.DataOutputStream

/** Parses untrusted text-bearing PDFs in an Android isolated process with bounded output. */
internal class IsolatedPdfParserService : Service() {
    private val messenger by lazy { Messenger(IncomingHandler(Looper.getMainLooper(), applicationContext)) }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    private class IncomingHandler(
        looper: Looper,
        private val applicationContext: Context,
    ) : Handler(looper) {
        override fun handleMessage(message: Message) {
            if (message.what != MESSAGE_PARSE) {
                super.handleMessage(message)
                return
            }

            val input = message.data.parcelFileDescriptor(KEY_INPUT)
            val output = message.data.parcelFileDescriptor(KEY_OUTPUT)
            val replyTo = message.replyTo
            val maxPages = message.data.getInt(KEY_MAX_PAGES, 0)
            val maxCharacters = message.data.getInt(KEY_MAX_CHARACTERS, 0)
            if (input == null || output == null || replyTo == null || maxPages <= 0 || maxCharacters <= 0) {
                input?.close()
                output?.close()
                sendCompletion(replyTo, RESULT_ERROR, "InvalidRequest", PdfParserStep.INITIALIZE.name)
                return
            }

            Thread(
                {
                    var step = PdfParserStep.INITIALIZE
                    try {
                        PDFBoxResourceLoader.init(applicationContext)
                        step = PdfParserStep.LOAD_DOCUMENT
                        val result =
                            ParcelFileDescriptor.AutoCloseInputStream(input).use { stream ->
                                PDDocument.load(stream).use { document ->
                                    step = PdfParserStep.EXTRACT_TEXT
                                    extractBounded(document, maxPages, maxCharacters)
                                }
                            }
                        // Completion is sent before pipe output so the caller starts draining the bounded pipe.
                        // This avoids blocking the isolated writer on the pipe buffer for larger text PDFs.
                        sendCompletion(replyTo, RESULT_OK, null, null)
                        step = PdfParserStep.WRITE_RESULT
                        ParcelFileDescriptor.AutoCloseOutputStream(output).use { stream ->
                            DataOutputStream(stream.buffered()).use { data -> writeFrame(data, result) }
                        }
                    } catch (throwable: Throwable) {
                        try {
                            output.close()
                        } catch (_: Exception) {
                            // Descriptor is already best-effort cleanup owned by this parse request.
                        }
                        sendCompletion(replyTo, RESULT_ERROR, throwable.javaClass.simpleName, step.name)
                    }
                },
                "redactguard-pdf-parser",
            ).start()
        }

        private fun extractBounded(
            document: PDDocument,
            maxPages: Int,
            maxCharacters: Int,
        ): IsolatedResult {
            val pageCount = document.numberOfPages
            val pagesToRead = minOf(pageCount, maxPages)
            val pages = ArrayList<IsolatedPage>(pagesToRead)
            var remainingCharacters = maxCharacters
            var truncated = pageCount > pagesToRead

            for (pageIndex in 0 until pagesToRead) {
                if (remainingCharacters == 0) {
                    truncated = true
                    break
                }
                val pageText =
                    PDFTextStripper()
                        .apply {
                            sortByPosition = true
                            startPage = pageIndex + 1
                            endPage = pageIndex + 1
                        }.getText(document)
                val boundedText = pageText.take(remainingCharacters)
                if (boundedText.length < pageText.length) truncated = true
                pages += IsolatedPage(pageIndex, boundedText)
                remainingCharacters -= boundedText.length
            }
            return IsolatedResult(pageCount, pages, truncated)
        }

        private fun writeFrame(
            output: DataOutputStream,
            result: IsolatedResult,
        ) {
            output.writeInt(FRAME_MAGIC)
            output.writeInt(result.pageCount)
            output.writeBoolean(result.truncated)
            output.writeInt(result.pages.size)
            result.pages.forEach { page ->
                val bytes = page.text.toByteArray(Charsets.UTF_8)
                output.writeInt(page.pageIndex)
                output.writeInt(bytes.size)
                output.write(bytes)
            }
        }

        private fun sendCompletion(
            replyTo: Messenger?,
            result: Int,
            errorType: String?,
            errorStep: String?,
        ) {
            if (replyTo == null) return
            try {
                replyTo.send(
                    Message.obtain(null, MESSAGE_COMPLETE).apply {
                        data =
                            Bundle().apply {
                                putInt(KEY_RESULT, result)
                                if (errorType != null) putString(KEY_ERROR_TYPE, errorType)
                                if (errorStep != null) putString(KEY_ERROR_STEP, errorStep)
                            }
                    },
                )
            } catch (_: Exception) {
                // Caller death is a normal lifecycle outcome; no content is logged.
            }
        }
    }

    private data class IsolatedResult(
        val pageCount: Int,
        val pages: List<IsolatedPage>,
        val truncated: Boolean,
    )

    private data class IsolatedPage(
        val pageIndex: Int,
        val text: String,
    )

    companion object {
        const val MESSAGE_PARSE = 1
        const val MESSAGE_COMPLETE = 2
        const val RESULT_OK = 0
        const val RESULT_ERROR = 1
        const val KEY_INPUT = "input"
        const val KEY_OUTPUT = "output"
        const val KEY_MAX_PAGES = "maxPages"
        const val KEY_MAX_CHARACTERS = "maxCharacters"
        const val KEY_RESULT = "result"
        const val KEY_ERROR_TYPE = "errorType"
        const val KEY_ERROR_STEP = "errorStep"
        const val FRAME_MAGIC = 0x52444750
    }
}

@Suppress("DEPRECATION")
private fun Bundle.parcelFileDescriptor(key: String): ParcelFileDescriptor? = getParcelable(key)
