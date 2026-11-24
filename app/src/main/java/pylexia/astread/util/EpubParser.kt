package pylexia.astread.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class EpubMetadata(
    val title: String,
    val author: String?,
    val totalPages: Int,
    val localFilePath: String
)

object EpubParser {

    fun parseAndSave(context: Context, uri: Uri): EpubMetadata {
        val resolver = context.contentResolver
        var originalFileName = "imported_book.epub"
        
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                originalFileName = cursor.getString(nameIndex) ?: originalFileName
            }
        }

        // Simpan file EPUB ke internal storage aplikasi
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val targetFile = File(booksDir, "${System.currentTimeMillis()}_$originalFileName")

        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        var title: String? = null
        var author: String? = null
        var totalHtmlWords = 0
        var spineItemCount = 0

        // Baca arsip ZIP (EPUB adalah ZIP yang berisi content.opf dan file HTML/XHTML)
        try {
            targetFile.inputStream().use { fileInput ->
                ZipInputStream(fileInput).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name.lowercase()
                        if (name.endsWith(".opf")) {
                            val (opfTitle, opfAuthor, items) = parseOpf(zip)
                            if (opfTitle != null && title == null) title = opfTitle
                            if (opfAuthor != null && author == null) author = opfAuthor
                            if (items > 0) spineItemCount = items
                        } else if (name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".htm")) {
                            // Estimasi kata untuk perhitungan halaman standar (250-300 kata per halaman standar buku)
                            val text = zip.bufferedReader().readText()
                            val plainText = stripHtml(text)
                            val words = plainText.split("\\s+".toRegex()).count { it.isNotBlank() }
                            totalHtmlWords += words
                        }
                        entry = zip.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val fallbackTitle = originalFileName.removeSuffix(".epub").replace("_", " ")
        val finalTitle = title?.takeIf { it.isNotBlank() } ?: fallbackTitle
        
        // Standar industri penerbitan: 1 halaman novel standar = ~275 kata
        // Jika tidak ada kata yang terhitung (misal gambar/enkripsi), fallback ke jumlah spine items * 10 atau minimal 50 hal
        val calculatedPages = when {
            totalHtmlWords > 0 -> (totalHtmlWords / 275).coerceAtLeast(1)
            spineItemCount > 0 -> (spineItemCount * 12).coerceAtLeast(1)
            else -> 100
        }

        return EpubMetadata(
            title = finalTitle,
            author = author?.takeIf { it.isNotBlank() },
            totalPages = calculatedPages,
            localFilePath = targetFile.absolutePath
        )
    }

    private fun stripHtml(html: String): String {
        return html.replace("<[^>]*>".toRegex(), " ")
    }

    private fun parseOpf(inputStream: InputStream): Triple<String?, String?, Int> {
        var title: String? = null
        var author: String? = null
        var spineCount = 0

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name.lowercase()
                        if (currentTag == "itemref") {
                            spineCount++
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty()) {
                            if (currentTag.endsWith("title") && title == null) {
                                title = text
                            } else if ((currentTag.endsWith("creator") || currentTag.endsWith("author")) && author == null) {
                                author = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Abaikan parsing error, fallback ke nama file
        }

        return Triple(title, author, spineCount)
    }
}
