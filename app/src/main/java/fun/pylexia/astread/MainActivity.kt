package fun.pylexia.astread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import fun.pylexia.astread.data.db.AppDatabase
import fun.pylexia.astread.data.repository.BookRepository
import fun.pylexia.astread.ui.screens.BookshelfScreen
import fun.pylexia.astread.ui.theme.AstReadTheme
import fun.pylexia.astread.ui.viewmodel.BookshelfViewModel
import fun.pylexia.astread.ui.viewmodel.BookshelfViewModelFactory

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
