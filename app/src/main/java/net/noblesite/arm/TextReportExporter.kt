package net.noblesite.arm

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExportedReport(
    val fileName: String,
    val uri: Uri
)

class TextReportExporter(
    private val context: Context
) {
    suspend fun export(summary: SessionSummary): ExportedReport {
        return withContext(Dispatchers.IO) {
            val fileName = reportFileName(summary)
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/A.R.M."
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = requireNotNull(
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ) {
                "Unable to create report in Downloads."
            }

            try {
                resolver.openOutputStream(uri).use { outputStream ->
                    requireNotNull(outputStream) {
                        "Unable to open report output stream."
                    }.write(buildSessionTextReport(summary).encodeToByteArray())
                }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                ExportedReport(
                    fileName = fileName,
                    uri = uri
                )
            } catch (throwable: Throwable) {
                resolver.delete(uri, null, null)
                throw throwable
            }
        }
    }
}
