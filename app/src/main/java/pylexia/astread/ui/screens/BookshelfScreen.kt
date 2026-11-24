package pylexia.astread.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pylexia.astread.data.model.BookEntity
import pylexia.astread.ui.theme.GoldAccent
import pylexia.astread.ui.theme.MidnightBackground
import pylexia.astread.ui.theme.MidnightSurface
import pylexia.astread.ui.theme.TextSecondary
import pylexia.astread.ui.viewmodel.BookshelfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    viewModel: BookshelfViewModel,
    onBookClick: (Long) -> Unit = {}
) {
    val books by viewModel.books.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AstRead",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = GoldAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "· Local Library",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GoldAccent,
                contentColor = MidnightBackground
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Book")
            }
        },
        containerColor = MidnightBackground
    ) { padding ->
        if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum ada buku di rak.",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Tekan tombol + untuk menambahkan bacaan baru",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(books, key = { it.id }) { book ->
                    BookItemCard(
                        book = book,
                        onClick = { onBookClick(book.id) },
                        onUpdatePage = { newPage -> viewModel.updateProgress(book.id, newPage) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddBookDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, author, pages, vols ->
                    viewModel.addBook(title, author, pages, vols)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun BookItemCard(
    book: BookEntity,
    onClick: () -> Unit,
    onUpdatePage: (Int) -> Unit
) {
    val progressPercent = if (book.totalPages > 0) {
        ((book.currentPage.toFloat() / book.totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else 0
    val remainingPages = (book.totalPages - book.currentPage).coerceAtLeast(0)

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    if (!book.author.isNullOrBlank()) {
                        Text(
                            text = book.author,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$progressPercent%",
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = GoldAccent,
                trackColor = Color(0xFF262A3D)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${book.currentPage} / ${book.totalPages} hal",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = if (remainingPages == 0) "Tamat 🎉" else "Sisa $remainingPages hal",
                    fontSize = 12.sp,
                    color = if (remainingPages == 0) GoldAccent else TextSecondary
                )
            }
        }
    }
}

@Composable
fun AddBookDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, author: String?, totalPages: Int, totalVolumes: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var pagesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Buku / Light Novel", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Buku") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Penulis (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pagesText,
                    onValueChange = { pagesText = it.filter { char -> char.isDigit() } },
                    label = { Text("Total Halaman") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pages = pagesText.toIntOrNull() ?: 1
                    if (title.isNotBlank()) {
                        onConfirm(title, author, pages, 1)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                Text("Simpan", color = MidnightBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        },
        containerColor = MidnightSurface
    )
}
