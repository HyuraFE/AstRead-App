package fun.pylexia.astread.data.db

import androidx.room.*
import fun.pylexia.astread.data.model.BookEntity
import fun.pylexia.astread.data.model.BookStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE status = :status ORDER BY updatedAt DESC")
    fun getBooksByStatus(status: BookStatus): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun getBookById(id: Long): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("UPDATE books SET currentPage = :page, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: Long, page: Int, timestamp: Long = System.currentTimeMillis())
}
