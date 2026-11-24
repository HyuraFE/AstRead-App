package fun.pylexia.astread.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fun.pylexia.astread.data.model.BookEntity
import fun.pylexia.astread.data.model.BookStatus
import fun.pylexia.astread.data.repository.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookshelfViewModel(private val repository: BookRepository) : ViewModel() {

    val books: StateFlow<List<BookEntity>> = repository.allBooks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addBook(title: String, author: String?, totalPages: Int, totalVolumes: Int = 1) {
        viewModelScope.launch {
            val newBook = BookEntity(
                title = title.trim(),
                author = author?.trim()?.ifEmpty { null },
                totalPages = totalPages.coerceAtLeast(1),
                totalVolumes = totalVolumes.coerceAtLeast(1),
                status = BookStatus.READING
            )
            repository.insertBook(newBook)
        }
    }

    fun updateProgress(bookId: Long, newPage: Int) {
        viewModelScope.launch {
            repository.updateProgress(bookId, newPage)
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
        }
    }
}

class BookshelfViewModelFactory(private val repository: BookRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookshelfViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookshelfViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
