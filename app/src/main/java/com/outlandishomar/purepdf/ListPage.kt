package com.outlandishomar.purepdf

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

private val PurePdfRed = Color(0xFFCC3333)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListPage(
    listName: String,
    onBack: () -> Unit,
    onFileClick: (String) -> Unit,
    onPickFiles: () -> Unit,
    pendingFiles: List<Uri>,
    onClearPendingFiles: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var files by remember { mutableStateOf(ListsManager.getListFiles(context, listName)) }
    var isGridView by remember { mutableStateOf(true) }
    var fileToRemove by remember { mutableStateOf<ListPdfItem?>(null) }

    // Process incoming files from file picker
    LaunchedEffect(pendingFiles) {
        if (pendingFiles.isNotEmpty()) {
            pendingFiles.forEach { uri ->
                val fileName = getFileName(context, uri)
                ListsManager.addFileToList(context, listName, uri.toString(), fileName)
            }
            files = ListsManager.getListFiles(context, listName)
            onClearPendingFiles()
        }
    }

    // Remove file dialog
    if (fileToRemove != null) {
        val target = fileToRemove!!
        val bgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
        val textColor = if (isDark) Color.White else Color.Black

        AlertDialog(
            onDismissRequest = { fileToRemove = null },
            containerColor = bgColor,
            title = {
                Text(
                    target.name,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            ListsManager.removeFileFromList(context, listName, target.uri)
                            files = ListsManager.getListFiles(context, listName)
                            fileToRemove = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Remove from list",
                            color = PurePdfRed,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { fileToRemove = null }) {
                    Text("Cancel", color = if (isDark) Color.LightGray else Color.Gray)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = listName,
                    fontFamily = sfProFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onPickFiles,
                containerColor = PurePdfRed,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Files",
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
            // View toggle header
            ListPageHeaderRow(
                isGridView = isGridView,
                onViewToggle = { isGridView = it },
                isDark = isDark
            )

            if (files.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.pdffileinred),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No files in this list",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = if (isDark) Color.White else Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add PDFs",
                            fontSize = 14.sp,
                            color = if (isDark) Color.LightGray else Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(files, key = { it.uri }) { pdf ->
                            ListPageGridCard(
                                pdf = pdf,
                                onClick = { onFileClick(pdf.uri) },
                                onLongClick = { fileToRemove = pdf }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(files, key = { it.uri }) { pdf ->
                            ListPageListRow(
                                pdf = pdf,
                                onClick = { onFileClick(pdf.uri) },
                                onLongClick = { fileToRemove = pdf }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListPageHeaderRow(
    isGridView: Boolean,
    onViewToggle: (Boolean) -> Unit,
    isDark: Boolean
) {
    val iconTint = if (isDark) Color.White else Color.DarkGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Files",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = if (isDark) Color.White else Color.DarkGray,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onViewToggle(true) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Grid View",
                tint = if (isGridView) PurePdfRed else iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = { onViewToggle(false) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "List View",
                tint = if (!isGridView) PurePdfRed else iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ── Grid Card ──

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListPageGridCard(
    pdf: ListPdfItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val textColor = if (isDark) Color.White else Color.Black
    val thumbBgColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)
    val view = LocalView.current

    val dateText = remember(pdf.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a | MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(pdf.timestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            )
    ) {
        Column {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(thumbBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.pdffileinred),
                    contentDescription = "PDF Icon",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Info
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = pdf.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
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

// ── List Row ──

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListPageListRow(
    pdf: ListPdfItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val textColor = if (isDark) Color.White else Color.Black
    val thumbBgColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)
    val view = LocalView.current

    val dateText = remember(pdf.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a | MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(pdf.timestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(thumbBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.pdffileinred),
                    contentDescription = "PDF Icon",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pdf.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateText,
                    fontSize = 12.sp,
                    color = PurePdfRed.copy(alpha = 0.7f)
                )
            }

            // More Options Icon
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = if (isDark) Color.LightGray else Color.Gray
                )
            }
        }
    }
}
