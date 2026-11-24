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

data class EpubChapter(
    val title: String,
    val content: String
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

    /**
     * Membaca isi teks bab/konten dari file EPUB lokal
     */
    fun readChapters(filePath: String): List<EpubChapter> {
        val file = File(filePath)
        if (!file.exists()) return emptyList()

        val chapters = mutableListOf<EpubChapter>()
        try {
            file.inputStream().use { fileInput ->
                ZipInputStream(fileInput).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    var chapterIdx = 1
                    while (entry != null) {
                        val name = entry.name.lowercase()
                        if ((name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".htm"))
                            && !name.contains("cover") && !name.contains("nav") && !name.contains("toc")) {

                            val rawHtml = zip.bufferedReader().readText()
                            val cleanText = cleanHtmlContent(rawHtml)
                            if (cleanText.isNotBlank()) {
                                chapters.add(
                                    EpubChapter(
                                        title = "Bab $chapterIdx",
                                        content = cleanText
                                    )
                                )
                                chapterIdx++
                            }
                        }
                        entry = zip.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return chapters
    }

    private fun cleanHtmlContent(html: String): String {
        return html
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .trim()
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
            // Ignore
        }

        return Triple(title, author, spineCount)
    }
}
