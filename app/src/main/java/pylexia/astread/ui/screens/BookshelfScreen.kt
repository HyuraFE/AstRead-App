package pylexia.astread.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import pylexia.astread.data.model.BookEntity
import pylexia.astread.ui.theme.GoldAccent
import pylexia.astread.ui.theme.MidnightBackground
import pylexia.astread.ui.theme.MidnightSurface
import pylexia.astread.ui.theme.TextSecondary
import pylexia.astread.ui.viewmodel.BookshelfViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    viewModel: BookshelfViewModel,
    onBookClick: (Long) -> Unit = {}
) {
    val books by viewModel.books.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val context = LocalContext.current

    // File picker launcher untuk file EPUB (Storage Access Framework)
    val epubPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importEpub(context, it)
        }
    }

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
                onClick = {
                    // Langsung buka document picker khusus EPUB
                    epubPickerLauncher.launch(
                        arrayOf(
                            "application/epub+zip",
                            "application/octet-stream",
                            "*/*"
                        )
                    )
                },
                containerColor = GoldAccent,
                contentColor = MidnightBackground
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MidnightBackground,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = "Import EPUB")
                }
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum ada buku di rak",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tekan tombol + untuk memilih file EPUB dari HP. Judul, halaman, dan poster cover akan diekstrak otomatis.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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

    val coverFile = book.coverUri?.let { File(it) }
    val hasValidCover = coverFile != null && coverFile.exists()

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster Cover EPUB / Light Novel
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(98.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2235)),
                contentAlignment = Alignment.Center
            ) {
                if (hasValidCover) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover ${book.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback visual sampul jika file EPUB tidak menyertakan cover
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = GoldAccent.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = book.title.take(3).uppercase(),
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Detail Buku & Bar Progress
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            maxLines = 2
                        )
                        if (!book.author.isNullOrBlank()) {
                            Text(
                                text = book.author,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GoldAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$progressPercent%",
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = GoldAccent,
                    trackColor = Color(0xFF262A3D)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${book.currentPage} / ${book.totalPages} hal",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = if (remainingPages == 0) "Tamat 🎉" else "Sisa $remainingPages hal",
                        fontSize = 11.sp,
                        color = if (remainingPages == 0) GoldAccent else TextSecondary
                    )
                }
            }
        }
    }
}
