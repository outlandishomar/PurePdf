package com.saverio.pdfviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import android.provider.OpenableColumns
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.*
val sfProFontFamily = FontFamily(
    Font(R.font.sf_pro_regular, FontWeight.Normal),
    Font(R.font.sf_pro_bold, FontWeight.Bold)
)
// ── Brand color ──
private val PurePdfRed = Color(0xFFCC3333)

// ── Root ──

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    recentItems: List<RecentPdf>,
    onOpenPdfClick: () -> Unit,
    onRecentClick: (RecentPdf) -> Unit,
    onToggleFavorite: (RecentPdf) -> Unit,
    onPhotoToPdfClick: () -> Unit,
    onMergePdfClick: () -> Unit,
    onSplitPdfClick: () -> Unit,
    onManagePagesClick: () -> Unit,
    onExtractTextClick: () -> Unit,
    onWatermarkClick: () -> Unit,
    onSignatureClick: () -> Unit,
    onThemeChanged: (String) -> Unit,
    currentTheme: String
) {
    var query by remember { mutableStateOf("") }
    var selectedPdfForInfo by remember { mutableStateOf<RecentPdf?>(null) }
    var isGridView by remember { mutableStateOf(true) }
    var showOnlyFavorites by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()


    val filteredList = remember(recentItems, query, showOnlyFavorites) {
        var list = if (query.isBlank()) recentItems
        else recentItems.filter { it.name.contains(query, ignoreCase = true) }
        
        if (showOnlyFavorites) {
            list = list.filter { it.isFavorite }
        }
        list
    }

    if (selectedPdfForInfo != null) {
        FileInfoDialog(
            pdf = selectedPdfForInfo!!,
            onDismiss = { selectedPdfForInfo = null }
        )
    }

    if (showSettingsScreen) {
        SettingsScreen(
            onBackClick = { showSettingsScreen = false },
            onThemeChanged = onThemeChanged,
            currentTheme = currentTheme
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = { PurePdfTopBar(onInfoClick = { showSettingsScreen = true }) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onOpenPdfClick,
                    containerColor = PurePdfRed,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.newfile_icon),
                        contentDescription = "Open PDF",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        ) { innerPadding ->
            val pagerState = rememberPagerState(pageCount = { 2 })
            val coroutineScope = rememberCoroutineScope()
            val tabTitles = listOf("Recent", "Tools")
            val tabIcons = listOf(Icons.Default.Description, Icons.Default.Build)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // ── Tab Row ──
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = PurePdfRed,
                    indicator = @Composable { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentSize(Alignment.BottomStart),
                                color = PurePdfRed
                            )
                        }
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (pagerState.currentPage == index) PurePdfRed
                                            else if (isDark) Color.LightGray else Color.Gray
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = title,
                                    tint = if (pagerState.currentPage == index) PurePdfRed
                                           else if (isDark) Color.LightGray else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }

                // ── Horizontal Pager ──
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> RecentTabContent(
                            items = filteredList,
                            query = query,
                            onQueryChange = { query = it },
                            isGridView = isGridView,
                            onViewToggle = { isGridView = it },
                            showOnlyFavorites = showOnlyFavorites,
                            onToggleShowOnlyFavorites = { showOnlyFavorites = it },
                            onItemClick = onRecentClick,
                            onItemLongClick = { pdf -> selectedPdfForInfo = pdf },
                            onToggleFavorite = onToggleFavorite,
                            isDark = isDark
                        )
                        1 -> ToolsPage(
                            onPhotoToPdfClick = onPhotoToPdfClick,
                            onMergePdfClick = onMergePdfClick,
                            onSplitPdfClick = onSplitPdfClick,
                            onManagePagesClick = onManagePagesClick,
                            onExtractTextClick = onExtractTextClick,
                            onWatermarkClick = onWatermarkClick,
                            onSignatureClick = onSignatureClick,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTabContent(
    items: List<RecentPdf>,
    query: String,
    onQueryChange: (String) -> Unit,
    isGridView: Boolean,
    onViewToggle: (Boolean) -> Unit,
    showOnlyFavorites: Boolean,
    onToggleShowOnlyFavorites: (Boolean) -> Unit,
    onItemClick: (RecentPdf) -> Unit,
    onItemLongClick: (RecentPdf) -> Unit,
    onToggleFavorite: (RecentPdf) -> Unit,
    isDark: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        RecentSearchBar(
            query = query,
            onQueryChange = onQueryChange
        )

        // Header Row (Always visible)
        RecentHeaderRow(
            isGridView = isGridView,
            onViewToggle = onViewToggle,
            showOnlyFavorites = showOnlyFavorites,
            onToggleShowOnlyFavorites = onToggleShowOnlyFavorites
        )

        // Content
        if (items.isEmpty() && query.isBlank()) {
            EmptyRecentState(
                showOnlyFavorites = showOnlyFavorites,
                modifier = Modifier.weight(1f)
            )
        } else if (items.isEmpty()) {
            // Search produced no results
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching documents",
                    color = if (isDark) Color.LightGray else Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else {
            RecentSection(
                items = items,
                isGridView = isGridView,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Top App Bar ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurePdfTopBar(onInfoClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "PurePdf",
                fontFamily = sfProFontFamily, // السطر ده اللي هيقلبها أيفون
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        navigationIcon = {
            Icon(
                painter = painterResource(id = R.drawable.danewicon),
                contentDescription = "PurePdf Logo",
                tint = Color.Unspecified,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(32.dp)
            )
        },
        actions = {
            IconButton(onClick = onInfoClick) {
                Icon(
                    painter = painterResource(id = R.drawable.settings_svgrepo_com),
                    contentDescription = "Settings",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
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
}

// ── Search Bar ──

@Composable
private fun RecentSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val textColor = if (isDark) Color.White else Color.Black

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = textColor,
            fontSize = 16.sp
        ),
        placeholder = {
            Text("Search documents", color = if (isDark) Color.LightGray else Color.Gray)
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
            focusedContainerColor = cardColor,
            unfocusedContainerColor = cardColor,
            focusedBorderColor = PurePdfRed,
            unfocusedBorderColor = if (isDark) Color.DarkGray else Color.LightGray,
            cursorColor = PurePdfRed
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

// ── Recently Viewed Section ──

@Composable
private fun RecentHeaderRow(
    isGridView: Boolean,
    onViewToggle: (Boolean) -> Unit,
    showOnlyFavorites: Boolean,
    onToggleShowOnlyFavorites: (Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val iconTint = if (isDark) Color.White else Color.DarkGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recently viewed",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = if (isDark) Color.White else Color.DarkGray,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onToggleShowOnlyFavorites(!showOnlyFavorites) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (showOnlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Quick Favorite Filter",
                tint = if (showOnlyFavorites) PurePdfRed else iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
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

@Composable
private fun RecentSection(
    items: List<RecentPdf>,
    isGridView: Boolean,
    onItemClick: (RecentPdf) -> Unit,
    onItemLongClick: (RecentPdf) -> Unit,
    onToggleFavorite: (RecentPdf) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val iconTint = if (isDark) Color.White else Color.DarkGray
    
    Column(modifier = modifier.fillMaxWidth()) {

        if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.uri }) { pdf ->
                    RecentItemCard(
                        pdf = pdf,
                        onClick = { onItemClick(pdf) },
                        onLongClick = { onItemLongClick(pdf) },
                        onToggleFavorite = { onToggleFavorite(pdf) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.uri }) { pdf ->
                    RecentListItemRow(
                        pdf = pdf,
                        onClick = { onItemClick(pdf) },
                        onLongClick = { onItemLongClick(pdf) },
                        onToggleFavorite = { onToggleFavorite(pdf) }
                    )
                }
            }
        }
    }
}

// ── Individual Card ──

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentItemCard(
    pdf: RecentPdf,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
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
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onToggleFavorite()
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp)
                ) {
                    Icon(
                        imageVector = if (pdf.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (pdf.isFavorite) PurePdfRed else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
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

// ── Empty State ──

@Composable
private fun EmptyRecentState(showOnlyFavorites: Boolean, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showOnlyFavorites) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = PurePdfRed,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No favorite files",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = if (isDark) Color.White else Color.DarkGray
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.pdffileinred),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No recent files",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = if (isDark) Color.White else Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap the button to open a PDF",
                    fontSize = 14.sp,
                    color = if (isDark) Color.LightGray else Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── File Info Dialog ──

@Composable
fun FileInfoDialog(
    pdf: RecentPdf,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color.LightGray else Color.Gray

    val fileInfo = remember(pdf.uri) { getFileInfo(context, pdf.uri) }
    val path = fileInfo.first
    val sizeStr = fileInfo.second

    val dateText = remember(pdf.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a | MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(pdf.timestamp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        title = {
            Text(
                text = "File Info",
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoRow("File Name", pdf.name, textColor, subTextColor)
                InfoRow("Size", sizeStr, textColor, subTextColor)
                InfoRow("Location", path, textColor, subTextColor)
                InfoRow("Last Accessed", dateText, textColor, subTextColor)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PurePdfRed)
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String, textColor: Color, subTextColor: Color) {
    Column {
        Text(text = label, fontSize = 12.sp, color = subTextColor)
        Text(text = value, fontSize = 14.sp, color = textColor)
    }
}

private fun getFileInfo(context: android.content.Context, uriString: String): Pair<String, String> {
    val uri = Uri.parse(uriString)
    var size: Long = 0
    var path: String = uriString

    try {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } else if (uri.scheme == "file") {
            path = uri.path ?: uriString
            val file = java.io.File(path)
            if (file.exists()) size = file.length()
        }
    } catch (e: Exception) {}
    
    val sizeStr = if (size > 0) {
        val mb = size / (1024f * 1024f)
        String.format(Locale.US, "%.2f MB", mb)
    } else "Unknown Size"
    
    return Pair(path, sizeStr)
}

// ── List Item Row ──

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentListItemRow(
    pdf: RecentPdf,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
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

            // Favoriting Icon
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    onToggleFavorite()
                }
            ) {
                Icon(
                    imageVector = if (pdf.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (pdf.isFavorite) PurePdfRed else Color.Gray,
                    modifier = Modifier.size(20.dp)
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
                    contentDescription = "More Info",
                    tint = if (isDark) Color.LightGray else Color.Gray
                )
            }
        }
    }
}

// ── Tools Page ──

@Composable
private fun ToolsPage(
    onPhotoToPdfClick: () -> Unit,
    onMergePdfClick: () -> Unit,
    onSplitPdfClick: () -> Unit,
    onManagePagesClick: () -> Unit,
    onExtractTextClick: () -> Unit,
    onWatermarkClick: () -> Unit,
    onSignatureClick: () -> Unit,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Tools",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = if (isDark) Color.White else Color.DarkGray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ToolCard(
                    title = "Image to PDF",
                    subtitle = "Convert images to PDF",
                    icon = Icons.Default.PhotoLibrary,
                    isDark = isDark,
                    onClick = onPhotoToPdfClick
                )
            }
            item {
                ToolCard(
                    title = "Merge PDF",
                    subtitle = "Combine multiple PDFs",
                    icon = Icons.Default.CallMerge,
                    isDark = isDark,
                    onClick = onMergePdfClick
                )
            }
            item {
                ToolCard(
                    title = "Split PDF",
                    subtitle = "Extract selected pages",
                    icon = Icons.Default.ContentCut,
                    isDark = isDark,
                    onClick = onSplitPdfClick
                )
            }
            item {
                ToolCard(
                    title = "Manage Pages",
                    subtitle = "Reorder, delete, insert",
                    icon = Icons.Default.EditNote,
                    isDark = isDark,
                    onClick = onManagePagesClick
                )
            }
            item {
                ToolCard(
                    title = "Extract Text",
                    subtitle = "Extract text from PDF",
                    icon = Icons.Default.Description,
                    isDark = isDark,
                    onClick = onExtractTextClick
                )
            }

            item {
                ToolCard(title = "Add Watermark", subtitle = "Text or image watermark",
                    icon = Icons.Default.BrandingWatermark, isDark = isDark, onClick = onWatermarkClick)
            }
            item {
                ToolCard(title = "Signature", subtitle = "Draw and place signature",
                    icon = Icons.Default.Draw, isDark = isDark, onClick = onSignatureClick)
            }
        }
    }
}

// ── Tool Card ──

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color.LightGray else Color.Gray
    val iconBgColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PurePdfRed,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = subTextColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Merge PDF Name Dialog ──

@Composable
fun MergePdfNameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var fileName by remember { mutableStateOf("MergedPDF") }
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        title = {
            Text(
                text = "Name your merged PDF",
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
                    val name = fileName.trim().ifBlank { "MergedPDF" }
                    onConfirm(name)
                }
            ) {
                Text("Merge", color = PurePdfRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isDark) Color.LightGray else Color.Gray)
            }
        }
    )
}
