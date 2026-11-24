package pylexia.astread.data.repository

import pylexia.astread.data.db.BookDao
import pylexia.astread.data.db.ReadingSessionDao
import pylexia.astread.data.model.BookEntity
import pylexia.astread.data.model.BookStatus
import pylexia.astread.data.model.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

class BookRepository(
    private val bookDao: BookDao,
    private val sessionDao: ReadingSessionDao
) {
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBookById(id: Long): Flow<BookEntity?> = bookDao.getBookById(id)

    suspend fun insertBook(book: BookEntity): Long = bookDao.insertBook(book)

    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    suspend fun deleteBook(book: BookEntity) = bookDao.deleteBook(book)

    suspend fun updateProgress(bookId: Long, page: Int) = bookDao.updateProgress(bookId, page)

    fun getSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>> =
        sessionDao.getSessionsForBook(bookId)

    suspend fun recordSession(
        bookId: Long,
        startPage: Int,
        endPage: Int,
        durationSeconds: Long
    ): Long {
        val deltaPages = (endPage - startPage).coerceAtLeast(0)
        val speed = if (durationSeconds > 0) {
            (deltaPages.toDouble() / durationSeconds.toDouble()) * 3600.0
        } else {
            0.0
        }
        val session = ReadingSessionEntity(
            bookId = bookId,
            startPage = startPage,
            endPage = endPage,
            durationSeconds = durationSeconds,
            speedPagesPerHour = speed
        )
        val id = sessionDao.insertSession(session)
        bookDao.updateProgress(bookId, endPage)
        return id
    }

    suspend fun calculateEtaHours(book: BookEntity): Double? {
        val rollingSpeed = sessionDao.getRollingAverageSpeed() ?: return null
        if (rollingSpeed <= 0.0) return null
        val remainingPages = (book.totalPages - book.currentPage).coerceAtLeast(0)
        return remainingPages / rollingSpeed
    }
}
