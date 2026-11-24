package com.example.domain.golu2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream

data class PickedDocumentPayload(
    val title: String,
    val mimeType: String,
    val sizeBytes: Long,
    val textPreview: String,
    val fileBase64: String,
    val pdfPageCount: Int = 0,
    val firstPageBitmap: Bitmap? = null,
)

object PdfDocumentHelper {
    private const val MAX_BYTES = 3_800_000

    fun readPickedDocument(context: Context, uri: Uri): Result<PickedDocumentPayload> = runCatching {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        var displayName = "Document"
        var reportedSize = 0L

        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) displayName = c.getString(nameIdx) ?: displayName
                if (sizeIdx >= 0) reportedSize = c.getLong(sizeIdx)
            }
        }

        val rawBytes =
            resolver.openInputStream(uri)?.use { input ->
                val buf = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = input.read(chunk)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_BYTES) {
                        throw IllegalArgumentException("File exceeds 3.8 MB limit for secure processing.")
                    }
                    buf.write(chunk, 0, read)
                }
                buf.toByteArray()
            } ?: throw IllegalStateException("Unable to read selected document.")

        var pdfPages = 0
        var previewBmp: Bitmap? = null
        if (mime.contains("pdf", ignoreCase = true) || displayName.endsWith(".pdf", ignoreCase = true)) {
            runCatching {
                resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        pdfPages = renderer.pageCount
                        if (pdfPages > 0) {
                            renderer.openPage(0).use { page ->
                                val w = page.width.coerceIn(240, 800)
                                val h = (page.height * (w.toFloat() / page.width.coerceAtLeast(1))).toInt().coerceIn(240, 1100)
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                previewBmp = bmp
                            }
                        }
                    }
                }
            }
        }

        val isPlainText = mime.startsWith("text/") || mime.contains("json") || displayName.endsWith(".txt") || displayName.endsWith(".md")
        val textPreview =
            if (isPlainText) {
                rawBytes.toString(Charsets.UTF_8).take(20_000)
            } else {
                ""
            }

        PickedDocumentPayload(
            title = displayName,
            mimeType = mime,
            sizeBytes = if (reportedSize > 0) reportedSize else rawBytes.size.toLong(),
            textPreview = textPreview,
            fileBase64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP),
            pdfPageCount = pdfPages,
            firstPageBitmap = previewBmp,
        )
    }
}
