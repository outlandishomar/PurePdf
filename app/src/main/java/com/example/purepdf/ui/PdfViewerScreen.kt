package com.example.purepdf.ui

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val PurePdfRed = Color(0xFFCC3333)

/** Internal holder populated synchronously. */
private class PdfData(
    val pfd: ParcelFileDescriptor,
    val renderer: PdfRenderer,
    val cache: PdfRenderCache
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uri: Uri,
    contentResolver: ContentResolver,
    screenWidth: Int,
    onBack: () -> Unit
) {
    // 1. Instant UI: Load synchronously in remember.
    val data = remember(uri) {
        try {
            val pfd = contentResolver.openFileDescriptor(uri, "r")!!
            val renderer = PdfRenderer(pfd)
            PdfData(pfd, renderer, PdfRenderCache(renderer, screenWidth))
        } catch (e: Exception) {
            null
        }
    }

    // Cleanup on IO so back button is instant
    DisposableEffect(data) {
        onDispose {
            data?.let { d ->
                @Suppress("OPT_IN_USAGE")
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    d.cache.close()
                    runCatching { d.renderer.close() }
                    runCatching { d.pfd.close() }
                }
            }
        }
    }

    val listState = rememberLazyListState()
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    var showGoToDialog by remember { mutableStateOf(false) }
    var dragTargetPage by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    var isControlsVisible by remember { mutableStateOf(true) }
    val isFullscreen = remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) isControlsVisible = false
    }

    val context = LocalContext.current

    Scaffold(
        containerColor = Color(0xFF333333),
        topBar = {
            AnimatedVisibility(visible = !isFullscreen.value) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    title = {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            // Page counter removed from here
                        }
                    },
                    actions = { Spacer(Modifier.width(48.dp)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PurePdfRed)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (data != null) {
                // Main list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { isControlsVisible = !isControlsVisible })
                        }
                ) {
                    items(data.cache.pageCount) { index ->
                        PdfPageItem(cache = data.cache, pageIndex = index)
                    }
                }

                // Scrollbar logic with deferred fast scroll
                if (data.cache.pageCount > 1) {
                    val density = LocalDensity.current
                    var trackHeightPx by remember { mutableIntStateOf(0) }
                    val thumbHeight = 48.dp
                    val thumbHeightPx = with(density) { thumbHeight.toPx() }
                    
                    // Thumb always tracks the REAL scroll position
                    val thumbFraction by remember {
                        derivedStateOf {
                            val c = data.cache.pageCount
                            if (c <= 1) 0f else currentPage.toFloat() / (c - 1).toFloat()
                        }
                    }
                    // Drag bubble tracks the drag target position (separate from thumb)
                    val dragFraction by remember(dragTargetPage) {
                        derivedStateOf {
                            val dt = dragTargetPage ?: return@derivedStateOf 0f
                            val c = data.cache.pageCount
                            if (c <= 1) 0f else dt.toFloat() / (c - 1).toFloat()
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxSize()
                            .onSizeChanged { trackHeightPx = it.height }
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .width(36.dp)
                                .align(Alignment.TopEnd)
                                .pointerInput(data.cache.pageCount) {
                                    // maxPositionScrollbar = available track pixels minus thumb
                                    val maxPos = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
                                    var startY = 0f   // rawY at drag start
                                    var scrolled = 0f  // accumulated offset, clamped

                                    detectVerticalDragGestures(
                                        onDragStart = { offset ->
                                            startY = offset.y
                                            scrolled = offset.y - thumbHeightPx / 2f
                                            if (scrolled < 0f) scrolled = 0f
                                            else if (scrolled > maxPos) scrolled = maxPos
                                            val pageN = ((data.cache.pageCount - 1) * scrolled / maxPos).toInt()
                                                .coerceIn(0, data.cache.pageCount - 1)
                                            dragTargetPage = pageN
                                        },
                                        onVerticalDrag = { change, _ ->
                                            change.consume()
                                            val newY = change.position.y
                                            scrolled = newY - thumbHeightPx / 2f
                                            if (scrolled < 0f) scrolled = 0f
                                            else if (scrolled > maxPos) scrolled = maxPos
                                            val pageN = ((data.cache.pageCount - 1) * scrolled / maxPos).toInt()
                                                .coerceIn(0, data.cache.pageCount - 1)
                                            dragTargetPage = pageN
                                        },
                                        onDragEnd = {
                                            val target = dragTargetPage ?: 0
                                            dragTargetPage = null
                                            scope.launch { listState.scrollToItem(target) }
                                        },
                                        onDragCancel = {
                                            val target = dragTargetPage ?: 0
                                            dragTargetPage = null
                                            scope.launch { listState.scrollToItem(target) }
                                        }
                                    )
                                }
                        ) {
                            // ── Visual state animations ──
                            val isDragging = dragTargetPage != null
                            val currentWidth by animateDpAsState(
                                targetValue = if (isDragging) 30.dp else 6.dp,
                                label = "thumbWidth"
                            )
                            val usable = trackHeightPx - thumbHeightPx

                            // Use drag position if dragging, otherwise real scroll position
                            val activeFraction = if (isDragging) dragFraction else thumbFraction
                            val activeOffsetY = with(density) { (activeFraction * usable).toDp() }

                            // ── The Expanding Thumb ──
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 4.dp)
                                    .offset(y = activeOffsetY)
                                    .width(currentWidth)
                                    .height(thumbHeight)
                                    .clip(RoundedCornerShape(50))
                                    .background(PurePdfRed)
                            )

                            // ── Drag bubble: visible only while dragging ──
                            if (isDragging) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 40.dp) // space for the expanded thumb
                                        .offset(y = activeOffsetY + (thumbHeight / 2) - 20.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(PurePdfRed)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${dragTargetPage!! + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Floating Page Counter (Top Left) ──
            if (data != null) {
                AnimatedVisibility(
                    visible = isControlsVisible,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = if (isFullscreen.value) 28.dp else 56.dp),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.5f),
                        onClick = { showGoToDialog = true }
                    ) {
                        Text(
                            text = "${currentPage + 1} / ${data.cache.pageCount}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ── Floating Action Bar ──
            AnimatedVisibility(
                visible = isControlsVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + 
                        androidx.compose.animation.slideInVertically(animationSpec = androidx.compose.animation.core.tween(300), initialOffsetY = { it }),
                exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250)) + 
                       androidx.compose.animation.slideOutVertically(animationSpec = androidx.compose.animation.core.tween(250), targetOffsetY = { it + 300 })
            ) {
                    // Floating Thin Capsule (Top)
                    Surface(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                            .wrapContentWidth(),
                        shape = RoundedCornerShape(50),
                        color = PurePdfRed
                    ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share PDF"))
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = androidx.compose.ui.Modifier.size(28.dp))
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                val activity = context as? android.app.Activity

                                androidx.compose.material3.IconButton(onClick = {
                                    isFullscreen.value = !isFullscreen.value
                                    activity?.let { act ->
                                        val window = act.window
                                        val insetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
                                        if (isFullscreen.value) {
                                            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                                            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                        } else {
                                            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                                        }
                                    }
                                }) {
                                    androidx.compose.material3.Icon(
                                        painter = if (isFullscreen.value) {
                                            androidx.compose.ui.res.painterResource(id = com.example.purepdf.R.drawable.exitfull3)
                                        } else {
                                            androidx.compose.ui.res.painterResource(id = com.example.purepdf.R.drawable.enterfull3)
                                        },
                                        contentDescription = "Toggle Fullscreen",
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = androidx.compose.ui.Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
        }
    

    if (showGoToDialog && data != null) {
        GoToPageDialog(
            pageCount = data.cache.pageCount,
            onDismiss = { showGoToDialog = false },
            onGoToPage = { page ->
                showGoToDialog = false
                scope.launch { listState.scrollToItem(page) }
            }
        )
    }
}

// ── Page Item (Mutex-protected rendering on IO, white placeholder) ──

@Composable
private fun PdfPageItem(cache: PdfRenderCache, pageIndex: Int) {
    var bitmap by remember(pageIndex) { mutableStateOf(cache.getCachedPage(pageIndex)) }

    LaunchedEffect(pageIndex) {
        if (bitmap == null) {
            cache.requestPage(pageIndex) { ready ->
                if (ready == pageIndex) bitmap = cache.getCachedPage(pageIndex)
            }
        }
    }

    val bmp = bitmap
    if (bmp != null && !bmp.isRecycled) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Page ${pageIndex + 1}",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        )
    } else {
        // Fallback to A4 aspect ratio placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.414f)
                .padding(bottom = 2.dp)
                .background(Color.White)
        )
    }
}

// ── Go To Page Dialog ──

@Composable
private fun GoToPageDialog(
    pageCount: Int,
    onDismiss: () -> Unit,
    onGoToPage: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val submit = {
        val num = text.toIntOrNull()
        if (num != null && num in 1..pageCount) onGoToPage(num - 1) else isError = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to page") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() }; isError = false },
                placeholder = { Text("1 – $pageCount") },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurePdfRed, cursorColor = PurePdfRed
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = submit) {
                Text("Go", color = PurePdfRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
