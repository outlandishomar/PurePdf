package com.outlandishomar.purepdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val PurePdfRed = Color(0xFFCC3333)

data class PageItem(
    val index: Int,
    val bitmap: Bitmap,
    val isFromImage: Boolean = false,
    val imageUri: Uri? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePagesScreen(
    pdfUri: Uri,
    onBack: () -> Unit,
    onSave: (Uri, List<PageItem>, String) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var pages by remember { mutableStateOf<List<PageItem>>(emptyList()) }
    var loadError by remember { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showNameDialog by remember { mutableStateOf(false) }

    // Drag state
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val itemPositions = remember { mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>() }

    // Image picker for inserting pages
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val thumbWidth = 400
                    val scale = thumbWidth.toFloat() / bitmap.width
                    val thumbHeight = (bitmap.height * scale).toInt()
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, thumbWidth, thumbHeight, true)
                    if (scaledBitmap !== bitmap) bitmap.recycle()
                    val newPage = PageItem(
                        index = pages.size,
                        bitmap = scaledBitmap,
                        isFromImage = true,
                        imageUri = uri
                    )
                    pages = pages + newPage
                    Toast.makeText(context, "Image added as new page", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load page thumbnails
    LaunchedEffect(pdfUri) {
        withContext(Dispatchers.IO) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    val items = mutableListOf<PageItem>()
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
                        items.add(PageItem(index = i, bitmap = bitmap))
                    }
                    renderer.close()
                    pfd.close()
                    pages = items
                } else {
                    loadError = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadError = true
            }
        }
    }

    if (showNameDialog) {
        ManagePagesNameDialog(
            onDismiss = { showNameDialog = false },
            onConfirm = { fileName ->
                showNameDialog = false
                onSave(pdfUri, pages, fileName)
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Manage Pages", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Add, contentDescription = "Insert Image", tint = PurePdfRed)
                    }
                    if (selectedIndices.isNotEmpty()) {
                        IconButton(onClick = {
                            val toRemove = selectedIndices.toSet()
                            pages = pages.filterIndexed { idx, _ -> idx !in toRemove }
                            selectedIndices.clear()
                            itemPositions.clear()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = PurePdfRed)
                        }
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
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Button(
                    onClick = { showNameDialog = true },
                    enabled = pages.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurePdfRed,
                        contentColor = Color.White,
                        disabledContainerColor = if (isDark) Color(0xFF333333) else Color(0xFFCCCCCC)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp)
                ) {
                    Text("SAVE CHANGES", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    ) { innerPadding ->
        when {
            loadError -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Failed to load PDF pages", color = if (isDark) Color.LightGray else Color.Gray, fontSize = 16.sp)
                }
            }
            pages.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PurePdfRed)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading pages...", color = if (isDark) Color.LightGray else Color.Gray)
                    }
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 12.dp)
                    ) {
                        // Instruction text
                        Text(
                            text = "Tap to select pages for deletion, or drag pages to reorder them. Long press and drag to reorder pages.",
                            fontSize = 13.sp,
                            color = if (isDark) Color.LightGray else Color.Gray,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )

                        // Pages in rows of 2
                        val rows = pages.chunked(2)
                        rows.forEachIndexed { rowIndex, rowPages ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowPages.forEachIndexed { colIndex, pageItem ->
                                    val pageIndex = rowIndex * 2 + colIndex
                                    val isSelected = pageIndex in selectedIndices
                                    val isDragging = draggedIndex == pageIndex

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .alpha(if (isDragging) 0.3f else 1f)
                                            .onGloballyPositioned { coords ->
                                                itemPositions[pageIndex] = coords.boundsInWindow()
                                            }
                                            .pointerInput(pageIndex, pages.size) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggedIndex = pageIndex
                                                        dragOffset = Offset.Zero
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffset += dragAmount
                                                    },
                                                    onDragEnd = {
                                                        // Find target position
                                                        val draggedRect = itemPositions[draggedIndex]
                                                        if (draggedRect != null) {
                                                            val dragCenter = Offset(
                                                                draggedRect.center.x + dragOffset.x,
                                                                draggedRect.center.y + dragOffset.y
                                                            )
                                                            var targetIdx = draggedIndex
                                                            for ((idx, rect) in itemPositions) {
                                                                if (idx != draggedIndex && rect.contains(dragCenter)) {
                                                                    targetIdx = idx
                                                                    break
                                                                }
                                                            }
                                                            if (targetIdx != draggedIndex && targetIdx in pages.indices) {
                                                                val mutable = pages.toMutableList()
                                                                val item = mutable.removeAt(draggedIndex)
                                                                mutable.add(targetIdx, item)
                                                                pages = mutable
                                                                selectedIndices.clear()
                                                                itemPositions.clear()
                                                            }
                                                        }
                                                        draggedIndex = -1
                                                        dragOffset = Offset.Zero
                                                    },
                                                    onDragCancel = {
                                                        draggedIndex = -1
                                                        dragOffset = Offset.Zero
                                                    }
                                                )
                                            }
                                    ) {
                                        DraggablePageCard(
                                            pageItem = pageItem,
                                            displayIndex = pageIndex,
                                            isSelected = isSelected,
                                            isDark = isDark,
                                            onToggleSelect = {
                                                if (pageIndex in selectedIndices) selectedIndices.remove(pageIndex)
                                                else selectedIndices.add(pageIndex)
                                            }
                                        )
                                    }
                                }
                                // Fill empty space in last row
                                if (rowPages.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Dragged item overlay
                    if (draggedIndex >= 0 && draggedIndex < pages.size) {
                        val draggedRect = itemPositions[draggedIndex]
                        if (draggedRect != null) {
                            val density = LocalDensity.current
                            Box(
                                modifier = Modifier
                                    .zIndex(10f)
                                    .offset {
                                        IntOffset(
                                            (draggedRect.left + dragOffset.x).roundToInt(),
                                            (draggedRect.top + dragOffset.y - with(density) { innerPadding.calculateTopPadding().toPx() }).roundToInt()
                                        )
                                    }
                                    .size(
                                        with(density) { draggedRect.width.toDp() },
                                        with(density) { draggedRect.height.toDp() }
                                    )
                                    .alpha(0.85f)
                                    .shadow(8.dp, RoundedCornerShape(12.dp))
                            ) {
                                DraggablePageCard(
                                    pageItem = pages[draggedIndex],
                                    displayIndex = draggedIndex,
                                    isSelected = false,
                                    isDark = isDark,
                                    onToggleSelect = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggablePageCard(
    pageItem: PageItem,
    displayIndex: Int,
    isSelected: Boolean,
    isDark: Boolean,
    onToggleSelect: () -> Unit
) {
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val borderColor = if (isSelected) PurePdfRed else if (isDark) Color(0xFF333333) else Color(0xFFDDDDDD)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isSelected) 3.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column {
            Box {
                Image(
                    bitmap = pageItem.bitmap.asImageBitmap(),
                    contentDescription = "Page ${displayIndex + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .clickable { onToggleSelect() }
                )
                IconButton(
                    onClick = onToggleSelect,
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "Select",
                        tint = if (isSelected) PurePdfRed else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().background(PurePdfRed).padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Page ${displayIndex + 1}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ManagePagesNameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var fileName by remember { mutableStateOf("EditedPDF") }
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        title = { Text("Save edited PDF as", fontWeight = FontWeight.Bold, color = textColor) },
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
            TextButton(onClick = { onConfirm(fileName.trim().ifBlank { "EditedPDF" }) }) {
                Text("Save", color = PurePdfRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isDark) Color.LightGray else Color.Gray)
            }
        }
    )
}
