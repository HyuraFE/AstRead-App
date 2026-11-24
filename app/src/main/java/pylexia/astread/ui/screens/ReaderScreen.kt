package pylexia.astread.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pylexia.astread.data.model.BookEntity
import pylexia.astread.ui.theme.GoldAccent
import pylexia.astread.ui.theme.MidnightBackground
import pylexia.astread.ui.theme.MidnightSurface
import pylexia.astread.ui.theme.TextSecondary
import pylexia.astread.util.EpubChapter
import pylexia.astread.util.EpubParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: BookEntity,
    onBack: () -> Unit,
    onProgressChange: (Int) -> Unit
) {
    var chapters by remember { mutableStateOf<List<EpubChapter>>(emptyList()) }
    var currentChapterIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showHud by remember { mutableStateOf(true) }

    LaunchedEffect(book.epubFilePath) {
        val path = book.epubFilePath
        if (path != null) {
            val loaded = withContext(Dispatchers.IO) {
                EpubParser.readChapters(path)
            }
            chapters = loaded
            isLoading = false
        } else {
            isLoading = false
        }
    }

    val currentChapter = chapters.getOrNull(currentChapterIndex)
    val totalChapters = chapters.size.coerceAtLeast(1)
    val chapterPercent = (((currentChapterIndex + 1).toFloat() / totalChapters.toFloat()) * 100).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showHud = !showHud
            }
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldAccent)
            }
        } else if (chapters.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = "Tidak dapat memuat teks buku.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "File EPUB mungkin kosong atau tidak ditemukan.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text("Kembali ke Rak", color = MidnightBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Konten Teks Bab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 90.dp, bottom = 100.dp)
            ) {
                item {
                    Text(
                        text = currentChapter?.title ?: "Bab ${currentChapterIndex + 1}",
                        color = GoldAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                item {
                    Text(
                        text = currentChapter?.content ?: "",
                        color = Color(0xFFEDE8DF),
                        fontSize = 17.sp,
                        lineHeight = 30.sp,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            // Minimal Footer saat HUD sembunyi
            if (!showHud) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentChapter?.title ?: "",
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${currentChapterIndex + 1} / $totalChapters",
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Top App Bar HUD
        AnimatedVisibility(
            visible = showHud,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = MidnightBackground.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Text(
                            text = currentChapter?.title ?: "Bab ${currentChapterIndex + 1}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Bottom Bar Scrubber & Navigation HUD
        AnimatedVisibility(
            visible = showHud && chapters.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = MidnightBackground.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bab ${currentChapterIndex + 1} dari $totalChapters",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$chapterPercent%",
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = currentChapterIndex.toFloat(),
                        onValueChange = { newIdx ->
                            currentChapterIndex = newIdx.toInt()
                            val calculatedPage = ((newIdx + 1) / totalChapters.toFloat() * book.totalPages).toInt()
                            onProgressChange(calculatedPage)
                        },
                        valueRange = 0f..(totalChapters - 1).coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = GoldAccent,
                            activeTrackColor = GoldAccent,
                            inactiveTrackColor = Color(0xFF262A3D)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                if (currentChapterIndex > 0) {
                                    currentChapterIndex--
                                    val page = ((currentChapterIndex + 1) / totalChapters.toFloat() * book.totalPages).toInt()
                                    onProgressChange(page)
                                }
                            },
                            enabled = currentChapterIndex > 0
                        ) {
                            Text("← Bab Sebelumnya", color = if (currentChapterIndex > 0) GoldAccent else TextSecondary.copy(alpha = 0.4f))
                        }

                        TextButton(
                            onClick = {
                                if (currentChapterIndex < totalChapters - 1) {
                                    currentChapterIndex++
                                    val page = ((currentChapterIndex + 1) / totalChapters.toFloat() * book.totalPages).toInt()
                                    onProgressChange(page)
                                }
                            },
                            enabled = currentChapterIndex < totalChapters - 1
                        ) {
                            Text("Bab Selanjutnya →", color = if (currentChapterIndex < totalChapters - 1) GoldAccent else TextSecondary.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}
