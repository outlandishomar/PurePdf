package com.saverio.pdfviewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PurePdfRed = Color(0xFFCC3333)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPdfScreen(
    pdfUri: Uri,
    onBack: () -> Unit,
    onSplit: (Uri, List<Int>, String) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // Load page thumbnails
    var pageBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loadError by remember { mutableStateOf(false) }
    val selectedPages = remember { mutableStateListOf<Int>() }
    var showNameDialog by remember { mutableStateOf(false) }

    // Render thumbnails on first composition
    LaunchedEffect(pdfUri) {
        withContext(Dispatchers.IO) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    val bitmaps = mutableListOf<Bitmap>()
                    val thumbWidth = 400
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val scale = thumbWidth.toFloat() / page.width
                        val thumbHeight = (page.height * scale).toInt()
                        val bitmap = Bitmap.createBitmap(
                            thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        bitmaps.add(bitmap)
                    }
                    renderer.close()
                    pfd.close()
                    pageBitmaps = bitmaps
                } else {
                    loadError = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadError = true
            }
        }
    }

    // Name dialog
    if (showNameDialog) {
        SplitPdfNameDialog(
            onDismiss = { showNameDialog = false },
            onConfirm = { fileName ->
                showNameDialog = false
                onSplit(pdfUri, selectedPages.toList(), fileName)
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedPages.isEmpty()) "Select Pages"
                               else "${selectedPages.size} Selected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Select All
                    IconButton(onClick = {
                        if (selectedPages.size == pageBitmaps.size) {
                            selectedPages.clear()
                        } else {
                            selectedPages.clear()
                            selectedPages.addAll(pageBitmaps.indices.toList())
                        }
                    }) {
                        Icon(
                            imageVector = if (selectedPages.size == pageBitmaps.size)
                                Icons.Default.Deselect else Icons.Default.SelectAll,
                            contentDescription = "Select/Deselect All"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        if (selectedPages.isEmpty()) {
                            Toast.makeText(context, "Select at least 1 page", Toast.LENGTH_SHORT).show()
                        } else {
                            showNameDialog = true
                        }
                    },
                    enabled = selectedPages.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurePdfRed,
                        contentColor = Color.White,
                        disabledContainerColor = if (isDark) Color(0xFF333333) else Color(0xFFCCCCCC)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp)
                ) {
                    Text(
                        text = "SPLIT PDF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        when {
            loadError -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Failed to load PDF pages",
                        color = if (isDark) Color.LightGray else Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
            pageBitmaps.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PurePdfRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading pages...",
                            color = if (isDark) Color.LightGray else Color.Gray
                        )
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(pageBitmaps) { index, bitmap ->
                        PageThumbnailCard(
                            bitmap = bitmap,
                            pageIndex = index,
                            isSelected = index in selectedPages,
                            isDark = isDark,
                            onClick = {
                                if (index in selectedPages) {
                                    selectedPages.remove(index)
                                } else {
                                    selectedPages.add(index)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageThumbnailCard(
    bitmap: Bitmap,
    pageIndex: Int,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PurePdfRed else if (isDark) Color(0xFF333333) else Color(0xFFDDDDDD)
    val borderWidth = if (isSelected) 3.dp else 1.dp
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color.White

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box {
            Column {
                // Thumbnail
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )
                // Page number label
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${pageIndex}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) PurePdfRed
                                else if (isDark) Color.LightGray else Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Selection checkmark overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PurePdfRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Split PDF Name Dialog ──

@Composable
fun SplitPdfNameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var fileName by remember { mutableStateOf("SplitPDF") }
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        title = {
            Text(
                text = "Name your split PDF",
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        },
        text = {
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("File name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurePdfRed,
                    focusedLabelColor = PurePdfRed,
                    cursorColor = PurePdfRed,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = fileName.trim().ifBlank { "SplitPDF" }
                    onConfirm(name)
                }
            ) {
                Text("Split", color = PurePdfRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isDark) Color.LightGray else Color.Gray)
            }
        }
    )
}
