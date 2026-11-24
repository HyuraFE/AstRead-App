package pylexia.astread.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BookStatus {
    READING,
    COMPLETED,
    ON_HOLD,
    PLAN_TO_READ
}

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String? = null,
    val currentVolume: Int = 1,
    val totalVolumes: Int = 1,
    val currentPage: Int = 0,
    val totalPages: Int,
    val coverUri: String? = null,
    val epubFilePath: String? = null,
    val status: BookStatus = BookStatus.READING,
    val lastReadPositionCfi: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
