package pylexia.astread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import pylexia.astread.data.db.AppDatabase
import pylexia.astread.data.model.BookEntity
import pylexia.astread.data.repository.BookRepository
import pylexia.astread.ui.screens.BookshelfScreen
import pylexia.astread.ui.screens.ReaderScreen
import pylexia.astread.ui.theme.AstReadTheme
import pylexia.astread.ui.viewmodel.BookshelfViewModel
import pylexia.astread.ui.viewmodel.BookshelfViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: BookshelfViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        val repository = BookRepository(db.bookDao(), db.sessionDao())
        BookshelfViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AstReadTheme {
                val books by viewModel.books.collectAsState()
                var activeBookId by remember { mutableStateOf<Long?>(null) }
                val selectedBook = books.find { it.id == activeBookId }

                if (selectedBook != null) {
                    BackHandler {
                        activeBookId = null
                    }
                    ReaderScreen(
                        book = selectedBook,
                        onBack = { activeBookId = null },
                        onProgressChange = { newPage ->
                            viewModel.updateProgress(selectedBook.id, newPage)
                        }
                    )
                } else {
                    BookshelfScreen(
                        viewModel = viewModel,
                        onBookClick = { bookId ->
                            activeBookId = bookId
                        }
                    )
                }
            }
        }
    }
}
