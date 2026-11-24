package pylexia.astread.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class EpubMetadata(
    val title: String,
    val author: String?,
    val totalPages: Int,
    val localFilePath: String,
    val coverPath: String?
)

data class EpubChapter(
    val title: String,
    val content: String
)

object EpubParser {

    // Batas aman untuk mencegah Memory Exhaustion / Zip Bomb DoS (50 MB)
    private const val MAX_ENTRY_BYTES = 50 * 1024 * 1024L

    fun parseAndSave(context: Context, uri: Uri): EpubMetadata {
        val resolver = context.contentResolver
        var originalFileName = "imported_book.epub"

        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                originalFileName = cursor.getString(nameIndex) ?: originalFileName
            }
        }

        // Sanitasi nama file untuk mencegah Path Traversal (CWE-22)
        val sanitizedName = File(originalFileName).name
            .replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
            .ifBlank { "book.epub" }

        val timestamp = System.currentTimeMillis()
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val targetFile = File(booksDir, "${timestamp}_$sanitizedName")

        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        var title: String? = null
        var author: String? = null
        var totalHtmlWords = 0
        var spineItemCount = 0
        var opfCoverHref: String? = null
        var opfPath: String? = null

        // Pass 1: Identifikasi OPF, Cover Href, dan Hitung Kata
        try {
            targetFile.inputStream().use { fileInput ->
                ZipInputStream(fileInput).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name.lowercase()
                        if (name.endsWith(".opf")) {
                            opfPath = entry.name
                            val opfInfo = parseOpf(zip)
                            if (opfInfo.title != null && title == null) title = opfInfo.title
                            if (opfInfo.author != null && author == null) author = opfInfo.author
                            if (opfInfo.spineCount > 0) spineItemCount = opfInfo.spineCount
                            if (opfInfo.coverHref != null) opfCoverHref = opfInfo.coverHref
                        } else if (name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".htm")) {
                            val text = readLimitedString(zip, MAX_ENTRY_BYTES)
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

        // Pass 2: Ekstraksi Gambar Cover Poster
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
        val coverFile = File(coversDir, "${timestamp}_cover.jpg")
        var extractedCoverPath: String? = null

        val targetCoverName = opfCoverHref?.substringAfterLast('/')?.lowercase()

        try {
            var foundCoverBytes: ByteArray? = null
            var fallbackFirstImageBytes: ByteArray? = null

            targetFile.inputStream().use { fileInput ->
                ZipInputStream(fileInput).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null && foundCoverBytes == null) {
                        val entryName = entry.name.lowercase()
                        val isImage = entryName.endsWith(".jpg") || entryName.endsWith(".jpeg") ||
                                entryName.endsWith(".png") || entryName.endsWith(".webp")

                        if (isImage) {
                            val bytes = readLimitedBytes(zip, MAX_ENTRY_BYTES)
                            if (targetCoverName != null && entryName.endsWith(targetCoverName)) {
                                foundCoverBytes = bytes
                            } else if (entryName.contains("cover")) {
                                foundCoverBytes = bytes
                            } else if (fallbackFirstImageBytes == null) {
                                fallbackFirstImageBytes = bytes
                            }
                        }
                        entry = zip.nextEntry
                    }
                }
            }

            val finalCoverBytes = foundCoverBytes ?: fallbackFirstImageBytes
            if (finalCoverBytes != null && finalCoverBytes.isNotEmpty()) {
                val bitmap = BitmapFactory.decodeByteArray(finalCoverBytes, 0, finalCoverBytes.size)
                if (bitmap != null) {
                    FileOutputStream(coverFile).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                    }
                    extractedCoverPath = coverFile.absolutePath
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val fallbackTitle = sanitizedName.removeSuffix(".epub").replace("_", " ")
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
            localFilePath = targetFile.absolutePath,
            coverPath = extractedCoverPath
        )
    }

    /**
     * Membaca isi teks bab/konten dari file EPUB lokal dengan bounded memory
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

                            val rawHtml = readLimitedString(zip, MAX_ENTRY_BYTES)
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

    private fun readLimitedBytes(input: InputStream, maxBytes: Long): ByteArray {
        val buffer = ByteArray(8192)
        val out = ByteArrayOutputStream()
        var total = 0L
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            total += read
            if (total > maxBytes) {
                val allowed = (maxBytes - (total - read)).toInt().coerceAtLeast(0)
                if (allowed > 0) out.write(buffer, 0, allowed)
                break
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun readLimitedString(input: InputStream, maxBytes: Long): String {
        return readLimitedBytes(input, maxBytes).toString(Charsets.UTF_8)
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

    private data class OpfResult(
        val title: String?,
        val author: String?,
        val spineCount: Int,
        val coverHref: String?
    )

    private fun parseOpf(inputStream: InputStream): OpfResult {
        var title: String? = null
        var author: String? = null
        var spineCount = 0
        var coverId: String? = null
        var coverHref: String? = null
        val itemMap = mutableMapOf<String, String>() // id to href

        try {
            val factory = XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = false
                // Security Hardening: Disable DTD & entity expansion (XXE mitigation)
                runCatching {
                    setFeature("http://xmlpull.org/v1/doc/features.html#process-docdecl", false)
                }
            }
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
                        } else if (currentTag == "meta") {
                            val name = parser.getAttributeValue(null, "name")
                            val content = parser.getAttributeValue(null, "content")
                            if (name.equals("cover", ignoreCase = true) && content != null) {
                                coverId = content
                            }
                        } else if (currentTag == "item") {
                            val id = parser.getAttributeValue(null, "id")
                            val href = parser.getAttributeValue(null, "href")
                            val properties = parser.getAttributeValue(null, "properties")
                            if (id != null && href != null) {
                                itemMap[id] = href
                            }
                            if (properties != null && properties.contains("cover-image") && href != null) {
                                coverHref = href
                            }
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

            if (coverHref == null && coverId != null) {
                coverHref = itemMap[coverId]
            }
        } catch (e: Exception) {
            // Ignore / Fallback gracefully
        }

        return OpfResult(title, author, spineCount, coverHref)
    }
}
