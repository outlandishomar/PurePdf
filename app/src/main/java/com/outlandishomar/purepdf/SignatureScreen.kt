package com.outlandishomar.purepdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val PurePdfRed = Color(0xFFCC3333)

data class SignaturePath(val points: List<Offset>, val color: Color, val width: Float)

data class SignatureResult(
    val bitmap: Bitmap,
    val offsetXRatio: Float, val offsetYRatio: Float,
    val scale: Float, val rotation: Float,
    val pageIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreen(
    pdfUri: Uri,
    pdfFileName: String,
    onBack: () -> Unit,
    onApply: (Uri, SignatureResult) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var phase by remember { mutableStateOf("draw") } // "draw" or "place"
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Drawing state
    val paths = remember { mutableStateListOf<SignaturePath>() }
    var currentPoints by remember { mutableStateOf(listOf<Offset>()) }
    var strokeColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableFloatStateOf(5f) }

    // Page state
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Placement state
    var sigOffsetX by remember { mutableFloatStateOf(100f) }
    var sigOffsetY by remember { mutableFloatStateOf(200f) }
    var sigScale by remember { mutableFloatStateOf(1f) }
    var sigRotation by remember { mutableFloatStateOf(0f) }
    var previewSize by remember { mutableStateOf(IntSize(1, 1)) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

    var showColorPicker by remember { mutableStateOf(false) }

    val defaultColors = listOf(Color.Black, Color(0xFF2196F3), Color.Red, Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF9C27B0))
    // Make sure strokeColor is not restricted
    val colors = remember { mutableStateListOf(*defaultColors.toTypedArray()) }
    if (strokeColor !in colors) {
        if (!colors.contains(strokeColor)) colors.add(strokeColor)
    }

    val sizes = listOf(4f to 8.dp, 8f to 14.dp, 16f to 20.dp)

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = strokeColor,
            onColorSelected = { c -> strokeColor = c },
            onDismiss = { showColorPicker = false }
        )
    }

    // Load first page
    LaunchedEffect(pdfUri) {
        withContext(Dispatchers.IO) {
            try {
                val fd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return@withContext
                val renderer = PdfRenderer(fd)
                val page = renderer.openPage(0)
                val s = 2f
                val bmp = Bitmap.createBitmap((page.width * s).toInt(), (page.height * s).toInt(), Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                fd.close()
                pageBitmap = bmp
            } catch (_: Exception) {}
        }
    }

    if (phase == "draw") {
        // Drawing dialog
        Dialog(onDismissRequest = onBack) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Create Signature", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        color = if (isDark) Color.White else Color.Black)
                    Spacer(Modifier.height(12.dp))

                    // Color picker
                    Text("Color :", fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color.Black)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        colors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .then(if (c == strokeColor) Modifier.border(3.dp, PurePdfRed, CircleShape) else Modifier)
                                    .clickable { strokeColor = c }
                            )
                        }
                        // Custom color button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                                .clickable { showColorPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(Icons.Default.Add, contentDescription = "Custom Color", tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Size picker
                    Text("Size :", fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color.Black)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        sizes.forEach { (w, dotSize) ->
                            Box(contentAlignment = Alignment.Center, modifier = Modifier
                                .size(40.dp)
                                .then(if (w == strokeWidth) Modifier.border(2.dp, PurePdfRed, CircleShape) else Modifier)
                                .clickable { strokeWidth = w }) {
                                Box(Modifier.size(dotSize).clip(CircleShape).background(if (isDark) Color.White else Color(0xFF333333)))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Drawing canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .border(1.dp, if (isDark) Color.Gray else Color.LightGray, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .onSizeChanged { canvasSize = it }
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(strokeColor, strokeWidth) {
                                    detectDragGestures(
                                        onDragStart = { offset -> currentPoints = listOf(offset) },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentPoints = currentPoints + change.position
                                        },
                                        onDragEnd = {
                                            paths.add(SignaturePath(currentPoints.toList(), strokeColor, strokeWidth))
                                            currentPoints = emptyList()
                                        }
                                    )
                                }
                        ) {
                            fun drawSignaturePath(pts: List<Offset>, color: Color, width: Float) {
                                if (pts.size < 2) return
                                val path = Path()
                                path.moveTo(pts[0].x, pts[0].y)
                                for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)
                                drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                            paths.forEach { drawSignaturePath(it.points, it.color, it.width) }
                            if (currentPoints.size > 1) drawSignaturePath(currentPoints, strokeColor, strokeWidth)
                        }

                        if (paths.isEmpty() && currentPoints.isEmpty()) {
                            Text("Sign here", color = Color.LightGray, modifier = Modifier.align(Alignment.Center))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Buttons
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { paths.clear(); currentPoints = emptyList() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0),
                                contentColor = if(isDark) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.weight(1f).height(45.dp)
                        ) { Text("CLEAR", fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Bold) }

                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0),
                                contentColor = if(isDark) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.weight(1f).height(45.dp)
                        ) { Text("CANCEL", fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Bold) }

                        Button(
                            onClick = {
                                if (paths.isNotEmpty() || currentPoints.isNotEmpty()) {
                                    val finalPaths = paths.toMutableList()
                                    if (currentPoints.isNotEmpty()) {
                                        finalPaths.add(SignaturePath(currentPoints.toList(), strokeColor, strokeWidth))
                                        currentPoints = emptyList()
                                    }
                                    val bitmap = pathsToBitmap(finalPaths, canvasSize.width, canvasSize.height)
                                    if (bitmap != null) {
                                        signatureBitmap = bitmap
                                        phase = "place"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurePdfRed),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.weight(1f).height(45.dp)
                        ) { Text("SAVE", fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    } else {
        // Phase 2: Place signature on page
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = { Text("Place Signature", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = { phase = "draw" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                Button(
                    onClick = {
                        signatureBitmap?.let { sig ->
                            val result = SignatureResult(
                                bitmap = sig,
                                offsetXRatio = if (previewSize.width > 0) sigOffsetX / previewSize.width else 0f,
                                offsetYRatio = if (previewSize.height > 0) sigOffsetY / previewSize.height else 0f,
                                scale = sigScale, rotation = sigRotation, pageIndex = 0
                            )
                            onApply(pdfUri, result)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurePdfRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp)
                ) { Text("APPLY SIGNATURE", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).background(if (isDark) Color(0xFF1A1A1A) else Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .onSizeChanged { previewSize = it }
                ) {
                    // PDF page
                    pageBitmap?.let { bmp ->
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = "Page",
                            modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                    }

                    // Draggable signature
                    signatureBitmap?.let { sig ->
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(sigOffsetX.roundToInt(), sigOffsetY.roundToInt()) }
                                .graphicsLayer(rotationZ = sigRotation)
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, rot ->
                                        sigOffsetX += pan.x
                                        sigOffsetY += pan.y
                                        sigScale *= zoom
                                        sigRotation += rot
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .border(1.dp, Color(0xFF2196F3).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                            ) {
                                Image(bitmap = sig.asImageBitmap(), contentDescription = "Signature",
                                    modifier = Modifier
                                        .width(150.dp * sigScale)
                                        .wrapContentHeight(),
                                    contentScale = ContentScale.FillWidth)
                            }

                            // Close/remove button
                            IconButton(
                                onClick = { phase = "draw" },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                    .background(PurePdfRed, CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun pathsToBitmap(paths: List<SignaturePath>, bgWidth: Int, bgHeight: Int): Bitmap? {
    if (paths.isEmpty()) return null
    var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
    var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
    paths.forEach { p ->
        p.points.forEach { pt ->
            if (pt.x < minX) minX = pt.x
            if (pt.x > maxX) maxX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.y > maxY) maxY = pt.y
        }
    }
    val margin = 20f
    minX = maxOf(0f, minX - margin)
    minY = maxOf(0f, minY - margin)
    maxX = minOf(bgWidth.toFloat(), maxX + margin)
    maxY = minOf(bgHeight.toFloat(), maxY + margin)
    
    val width = (maxX - minX).toInt().coerceAtLeast(1)
    val height = (maxY - minY).toInt().coerceAtLeast(1)
    
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    for (sp in paths) {
        if (sp.points.size < 2) continue
        val paint = android.graphics.Paint().apply {
            color = sp.color.toArgb()
            strokeWidth = sp.width
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
        val path = android.graphics.Path()
        path.moveTo(sp.points[0].x - minX, sp.points[0].y - minY)
        for (i in 1 until sp.points.size) {
            path.lineTo(sp.points[i].x - minX, sp.points[i].y - minY)
        }
        canvas.drawPath(path, paint)
    }
    return bitmap
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Color", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(red, green, blue))
                )
                Spacer(Modifier.height(16.dp))
                Text("Red", fontWeight = FontWeight.SemiBold)
                Slider(value = red, onValueChange = { red = it }, colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red))
                Text("Green", fontWeight = FontWeight.SemiBold)
                Slider(value = green, onValueChange = { green = it }, colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green))
                Text("Blue", fontWeight = FontWeight.SemiBold)
                Slider(value = blue, onValueChange = { blue = it }, colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue))
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(Color(red, green, blue)); onDismiss() }) { Text("OK", color = PurePdfRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) }
        }
    )
}
