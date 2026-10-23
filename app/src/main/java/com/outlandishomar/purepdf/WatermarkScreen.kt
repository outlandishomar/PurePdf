package com.outlandishomar.purepdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb

private val PurePdfRed = Color(0xFFCC3333)

data class WatermarkConfig(
    val type: String, // "text" or "image"
    val text: String = "",
    val imageBitmap: Bitmap? = null,
    val opacity: Float = 0.74f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val position: String = "Center",
    val colorArgb: Int = android.graphics.Color.RED
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkScreen(
    pdfUri: Uri,
    pdfFileName: String,
    onBack: () -> Unit,
    onApply: (Uri, WatermarkConfig) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // Phase: "choose" or "preview"
    var phase by remember { mutableStateOf("choose") }
    var watermarkType by remember { mutableStateOf("text") }
    var watermarkText by remember { mutableStateOf("WATERMARK") }
    var watermarkImage by remember { mutableStateOf<Bitmap?>(null) }

    // Preview adjustments
    var opacity by remember { mutableFloatStateOf(0.74f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var position by remember { mutableStateOf("Center") }
    var positionExpanded by remember { mutableStateOf(false) }

    var textColor by remember { mutableStateOf(PurePdfRed) }
    var showColorPicker by remember { mutableStateOf(false) }

    val defaultColors = listOf(PurePdfRed, Color.Black, Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF9C27B0))
    val colors = remember { mutableStateListOf(*defaultColors.toTypedArray()) }
    if (textColor !in colors) {
        if (!colors.contains(textColor)) colors.add(textColor)
    }

    // PDF first page preview
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = textColor,
            onColorSelected = { c -> textColor = c },
            onDismiss = { showColorPicker = false }
        )
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val stream = context.contentResolver.openInputStream(it)
            watermarkImage = BitmapFactory.decodeStream(stream)
            stream?.close()
        }
    }

    // Load first page for preview
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

    val positions = listOf("Center", "Top Left", "Top Right", "Bottom Left", "Bottom Right", "Top Center", "Bottom Center")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(if (phase == "choose") "Add Watermark" else "Preview", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { if (phase == "preview") phase = "choose" else onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (phase == "choose") {
            // Phase 1: Choose watermark type
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // File info
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF253545) else Color(0xFFF0F0F0))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(PurePdfRed), contentAlignment = Alignment.Center) {
                            Text("PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(pdfFileName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (isDark) Color.White else Color.Black)
                    }
                }

                Text("Watermark Type", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = if (isDark) Color.White else Color.Black)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = watermarkType == "text",
                        onClick = { watermarkType = "text" },
                        label = { Text("Text Watermark") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurePdfRed, selectedLabelColor = Color.White)
                    )
                    FilterChip(selected = watermarkType == "image",
                        onClick = { watermarkType = "image" },
                        label = { Text("Image Watermark") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurePdfRed, selectedLabelColor = Color.White)
                    )
                }

                if (watermarkType == "text") {
                    OutlinedTextField(
                        value = watermarkText, onValueChange = { watermarkText = it },
                        label = { Text("Watermark Text") }, singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurePdfRed, focusedLabelColor = PurePdfRed, cursorColor = PurePdfRed,
                            focusedTextColor = if (isDark) Color.White else Color.Black, unfocusedTextColor = if (isDark) Color.White else Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Text Color", fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color.Black)
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
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(c)
                                    .then(if (c == textColor) Modifier.border(3.dp, if (isDark) Color.White else Color.Black, androidx.compose.foundation.shape.CircleShape) else Modifier)
                                    .clickable { textColor = c }
                            )
                        }
                        // Custom color button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                                .clickable { showColorPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(Icons.Default.Add, contentDescription = "Custom Color", tint = Color.White)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(PurePdfRed)),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (watermarkImage != null) "Image Selected ✓" else "Pick Image", color = PurePdfRed, fontWeight = FontWeight.SemiBold) }

                    if (watermarkImage != null) {
                        Image(bitmap = watermarkImage!!.asImageBitmap(), contentDescription = "Watermark",
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { if (watermarkType == "text" && watermarkText.isNotBlank() || watermarkType == "image" && watermarkImage != null) phase = "preview" },
                    colors = ButtonDefaults.buttonColors(containerColor = PurePdfRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("NEXT", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        } else {
            // Phase 2: Preview with adjustments
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())) {
                // File info bar
                Card(
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF253545) else Color(0xFFF0F0F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(PurePdfRed), contentAlignment = Alignment.Center) {
                            Text("PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Preview", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (isDark) Color.LightGray else Color.Gray)
                            Text(pdfFileName, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (isDark) Color.White else Color.Black)
                        }
                    }
                }

                // Preview area
                Card(
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFEEEEEE)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Preview", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isDark) Color.White else Color.Black)
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = getBoxAlignment(position)) {
                            pageBitmap?.let {
                                Image(bitmap = it.asImageBitmap(), contentDescription = "Page",
                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            }
                            // Watermark overlay
                            if (watermarkType == "text") {
                                Text(
                                    text = watermarkText,
                                    color = textColor.copy(alpha = opacity),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (20 * scale).sp,
                                    modifier = Modifier.graphicsLayer(rotationZ = rotation)
                                )
                            } else {
                                watermarkImage?.let { img ->
                                    Image(
                                        bitmap = img.asImageBitmap(), contentDescription = "Watermark",
                                        modifier = Modifier.size((80 * scale).dp).graphicsLayer(rotationZ = rotation, alpha = opacity),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }

                // Apply button
                Button(
                    onClick = {
                        onApply(pdfUri, WatermarkConfig(watermarkType, watermarkText, watermarkImage, opacity, scale, rotation, position, textColor.toArgb()))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurePdfRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp)
                ) { Text("APPLY", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

                Spacer(Modifier.height(16.dp))

                // Adjustment controls
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Real-time Adjustments", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = if (isDark) Color.White else Color.Black)
                        Spacer(Modifier.height(12.dp))

                        // Position dropdown
                        Text("Position", fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color.Black)
                        ExposedDropdownMenuBox(expanded = positionExpanded, onExpandedChange = { positionExpanded = it }) {
                            OutlinedTextField(
                                value = position, onValueChange = {}, readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = if (isDark) Color.White else Color.Black,
                                    unfocusedTextColor = if (isDark) Color.White else Color.Black
                                )
                            )
                            ExposedDropdownMenu(expanded = positionExpanded, onDismissRequest = { positionExpanded = false }) {
                                positions.forEach { pos ->
                                    DropdownMenuItem(text = { Text(pos) }, onClick = { position = pos; positionExpanded = false })
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        SliderRow("Opacity", opacity, 0f, 1f, "${(opacity * 100).toInt()}%", isDark) { opacity = it }
                        SliderRow("Size", scale, 0.3f, 3f, "${String.format("%.1f", scale)}x", isDark) { scale = it }
                        SliderRow("Rotation", rotation, 0f, 360f, "${rotation.toInt()}°", isDark) { rotation = it }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, min: Float, max: Float, display: String, isDark: Boolean, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isDark) Color.White else Color.Black, modifier = Modifier.width(80.dp))
        Slider(value = value, onValueChange = onChange, valueRange = min..max,
            colors = SliderDefaults.colors(thumbColor = PurePdfRed, activeTrackColor = PurePdfRed),
            modifier = Modifier.weight(1f))
        Text(display, fontSize = 13.sp, color = if (isDark) Color.LightGray else Color.Gray, modifier = Modifier.width(45.dp))
    }
}

private fun getBoxAlignment(position: String): Alignment {
    return when (position) {
        "Top Left" -> Alignment.TopStart
        "Top Right" -> Alignment.TopEnd
        "Bottom Left" -> Alignment.BottomStart
        "Bottom Right" -> Alignment.BottomEnd
        "Top Center" -> Alignment.TopCenter
        "Bottom Center" -> Alignment.BottomCenter
        else -> Alignment.Center
    }
}


