package pylexia.astread.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pylexia.astread.data.model.BookEntity
import pylexia.astread.data.model.BookStatus
import pylexia.astread.data.repository.BookRepository
import pylexia.astread.util.EpubParser

class BookshelfViewModel(private val repository: BookRepository) : ViewModel() {

    val books: StateFlow<List<BookEntity>> = repository.allBooks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun importEpub(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val metadata = withContext(Dispatchers.IO) {
                    EpubParser.parseAndSave(context, uri)
                }

                val newBook = BookEntity(
                    title = metadata.title,
                    author = metadata.author,
                    totalPages = metadata.totalPages,
                    currentPage = 0,
                    coverPath = metadata.coverPath,
                    epubFilePath = metadata.localFilePath,
                    status = BookStatus.READING
                )
                repository.insertBook(newBook)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isImporting.value = false
            }
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
