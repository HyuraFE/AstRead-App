package pylexia.astread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import pylexia.astread.data.db.AppDatabase
import pylexia.astread.data.repository.BookRepository
import pylexia.astread.ui.screens.BookshelfScreen
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
                BookshelfScreen(viewModel = viewModel)
            }
        }
    }
}
