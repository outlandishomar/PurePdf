package com.example.purepdf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purepdf.R
import java.text.SimpleDateFormat
import java.util.*

// ── Brand color ──
private val PurePdfRed = Color(0xFFCC3333)

// ── Data model ──
data class RecentPdf(
    val uri: String,
    val displayName: String,
    val lastOpenedEpochMillis: Long
)

// ── Root ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recentList: List<RecentPdf>,
    onOpenFilePicker: () -> Unit,
    onRecentItemClick: (RecentPdf) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isGenerating = true
            coroutineScope.launch {
                val success = generatePdfFromImages(context, uris)
                isGenerating = false
                if (success) {
                    android.widget.Toast.makeText(context, "PDF Created Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Failed to create PDF", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var query by remember { mutableStateOf("") }
    val filteredList = remember(recentList, query) {
        if (query.isBlank()) recentList
        else recentList.filter { it.displayName.contains(query, ignoreCase = true) }
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = { PurePdfTopBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenFilePicker,
                containerColor = PurePdfRed,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.filesicon1),
                    contentDescription = "Open PDF",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            RecentSearchBar(
                query = query,
                onQueryChange = { query = it }
            )

            // Image to PDF Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = PurePdfRed)
                } else {
                    ExtendedFloatingActionButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        containerColor = PurePdfRed.copy(alpha = 0.1f),
                        contentColor = PurePdfRed,
                        icon = { Icon(painter = painterResource(id = R.drawable.filesicon1), contentDescription = null) },
                        text = { Text("Create PDF from Images", fontWeight = FontWeight.SemiBold) },
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    )
                }
            }

            // Content
            if (filteredList.isEmpty() && query.isBlank()) {
                EmptyRecentState(modifier = Modifier.weight(1f))
            } else if (filteredList.isEmpty()) {
                // Search produced no results
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching documents",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                RecentSection(
                    items = filteredList,
                    onItemClick = onRecentItemClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Top App Bar ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurePdfTopBar() {
    TopAppBar(
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "PurePdf",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PurePdfRed,
            titleContentColor = Color.White
        )
    )
}

// ── Search Bar ──

@Composable
private fun RecentSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text("Search documents", color = Color.Gray)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = PurePdfRed
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = PurePdfRed,
            unfocusedBorderColor = Color.LightGray,
            cursorColor = PurePdfRed
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

// ── Recently Viewed Section ──

@Composable
private fun RecentSection(
    items: List<RecentPdf>,
    onItemClick: (RecentPdf) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Recently viewed",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items, key = { it.uri }) { pdf ->
                RecentItemCard(pdf = pdf, onClick = { onItemClick(pdf) })
            }
        }
    }
}

// ── Individual Card ──

@Composable
private fun RecentItemCard(
    pdf: RecentPdf,
    onClick: () -> Unit
) {
    val dateText = remember(pdf.lastOpenedEpochMillis) {
        val sdf = SimpleDateFormat("hh:mm a | MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(pdf.lastOpenedEpochMillis))
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_document),
                    contentDescription = null,
                    tint = PurePdfRed.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
            }

            // Info
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = pdf.displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = PurePdfRed.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ── Empty State ──

@Composable
private fun EmptyRecentState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_document),
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No recent files",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap the button to open a PDF",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Helpers ──

suspend fun generatePdfFromImages(
    context: android.content.Context,
    uris: List<android.net.Uri>
): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val a4Width = 595
        val a4Height = 842

        for (uri in uris) {
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            // Scale to fit A4 while maintaining aspect ratio
            val scale = minOf(a4Width.toFloat() / bitmap.width, a4Height.toFloat() / bitmap.height)
            val scaledWidth = bitmap.width * scale
            val scaledHeight = bitmap.height * scale
            
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(a4Width, a4Height, pdfDocument.pages.size + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            
            val canvas = page.canvas
            val left = (a4Width - scaledWidth) / 2f
            val top = (a4Height - scaledHeight) / 2f
            
            val matrix = android.graphics.Matrix()
            matrix.postScale(scale, scale)
            matrix.postTranslate(left, top)
            
            canvas.drawBitmap(bitmap, matrix, null)
            
            pdfDocument.finishPage(page)
            bitmap.recycle()
        }

        val fileName = "Generated_PDF_${System.currentTimeMillis()}.pdf"
        val file = java.io.File(context.filesDir, fileName)
        java.io.FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
