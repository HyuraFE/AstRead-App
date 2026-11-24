package fun.pylexia.astread.data.db

import androidx.room.*
import fun.pylexia.astread.data.model.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 10): Flow<List<ReadingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity): Long

    @Query("SELECT AVG(speedPagesPerHour) FROM (SELECT speedPagesPerHour FROM reading_sessions ORDER BY createdAt DESC LIMIT 5)")
    suspend fun getRollingAverageSpeed(): Double?
}
