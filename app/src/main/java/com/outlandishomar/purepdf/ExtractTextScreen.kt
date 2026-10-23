package com.outlandishomar.purepdf

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PurePdfRed = Color(0xFFCC3333)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractTextScreen(
    pdfUri: Uri,
    pdfFileName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    // State
    var editableFileName by remember { mutableStateOf(pdfFileName) }
    var totalPages by remember { mutableStateOf(0) }
    var allPagesSelected by remember { mutableStateOf(true) }
    var fromPage by remember { mutableStateOf("1") }
    var toPage by remember { mutableStateOf("1") }
    var isExtracting by remember { mutableStateOf(false) }
    var extractedText by remember { mutableStateOf<String?>(null) }

    // Get total page count
    LaunchedEffect(pdfUri) {
        withContext(Dispatchers.IO) {
            try {
                PDFBoxResourceLoader.init(context)
                val inputStream = context.contentResolver.openInputStream(pdfUri)
                if (inputStream != null) {
                    val doc = PDDocument.load(inputStream)
                    totalPages = doc.numberOfPages
                    doc.close()
                    inputStream.close()
                    toPage = totalPages.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val bgColor = MaterialTheme.colorScheme.surface
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color.LightGray else Color.Gray
    val inputBgColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0F0)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (extractedText != null) "Extracted Text - $editableFileName" else "Extract Text",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (extractedText != null) extractedText = null
                        else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->

        if (extractedText != null) {
            // Phase 2: Show extracted text
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Action buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy to Clipboard
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Extracted Text", extractedText))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(PurePdfRed)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            "COPY TO\nCLIPBOARD",
                            color = PurePdfRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    // Save as Text File
                    OutlinedButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val outputDir = java.io.File(context.filesDir, "extracted_texts")
                                    if (!outputDir.exists()) outputDir.mkdirs()
                                    val cleanName = editableFileName.removeSuffix(".pdf")
                                    val outputFile = java.io.File(outputDir, "$cleanName.txt")
                                    outputFile.writeText(extractedText ?: "")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Saved: ${outputFile.name}", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(PurePdfRed)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            "SAVE AS\nTEXT FILE",
                            color = PurePdfRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Share Text
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, extractedText)
                                putExtra(Intent.EXTRA_SUBJECT, "Extracted Text - $pdfFileName")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Text"))
                        },
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(PurePdfRed)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            "SHARE\nTEXT",
                            color = PurePdfRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Text content box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1A2530) else Color(0xFFF0F4F8))
                        .border(1.dp, if (isDark) Color(0xFF2A3540) else Color(0xFFD0D8E0), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = extractedText ?: "",
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // Phase 1: Page selection
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // File Name
                Text(
                    text = "File Name",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PurePdfRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    OutlinedTextField(
                        value = editableFileName,
                        onValueChange = { editableFileName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4A90D9),
                            unfocusedBorderColor = if (isDark) Color(0xFF4A90D9) else Color(0xFF6AAFE6),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = Color(0xFF4A90D9),
                            focusedContainerColor = if (isDark) Color(0xFF1A2530) else Color(0xFFE8F0FE),
                            unfocusedContainerColor = if (isDark) Color(0xFF1A2530) else Color(0xFFE8F0FE)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Select Pages header
                Text(
                    text = "Select Pages",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                // All Pages radio
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = allPagesSelected,
                        onClick = { allPagesSelected = true },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = PurePdfRed,
                            unselectedColor = subTextColor
                        )
                    )
                    Text(
                        text = "All Pages",
                        fontSize = 16.sp,
                        color = textColor,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Page Range radio
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = !allPagesSelected,
                        onClick = { allPagesSelected = false },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = PurePdfRed,
                            unselectedColor = subTextColor
                        )
                    )
                    Text(
                        text = "Page Range",
                        fontSize = 16.sp,
                        color = textColor,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Page range inputs (shown when Page Range is selected)
                if (!allPagesSelected) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("From page", fontSize = 13.sp, color = subTextColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = fromPage,
                                onValueChange = { fromPage = it.filter { c -> c.isDigit() } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurePdfRed,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    cursorColor = PurePdfRed
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("To page", fontSize = 13.sp, color = subTextColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = toPage,
                                onValueChange = { toPage = it.filter { c -> c.isDigit() } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurePdfRed,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    cursorColor = PurePdfRed
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Extract Text button
                Button(
                    onClick = {
                        isExtracting = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                PDFBoxResourceLoader.init(context)
                                val inputStream = context.contentResolver.openInputStream(pdfUri)
                                if (inputStream != null) {
                                    val doc = PDDocument.load(inputStream)
                                    val stripper = PDFTextStripper()

                                    val start: Int
                                    val end: Int
                                    if (allPagesSelected) {
                                        start = 1
                                        end = doc.numberOfPages
                                    } else {
                                        start = (fromPage.toIntOrNull() ?: 1).coerceIn(1, doc.numberOfPages)
                                        end = (toPage.toIntOrNull() ?: doc.numberOfPages).coerceIn(start, doc.numberOfPages)
                                    }

                                    val sb = StringBuilder()
                                    for (page in start..end) {
                                        stripper.startPage = page
                                        stripper.endPage = page
                                        val text = stripper.getText(doc)
                                        sb.append("=== Page $page ===\n\n")
                                        sb.append(text.trim())
                                        sb.append("\n\n")
                                    }

                                    doc.close()
                                    inputStream.close()

                                    withContext(Dispatchers.Main) {
                                        extractedText = sb.toString()
                                        isExtracting = false
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Failed to extract text", Toast.LENGTH_SHORT).show()
                                    isExtracting = false
                                }
                            }
                        }
                    },
                    enabled = !isExtracting && totalPages > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurePdfRed,
                        contentColor = Color.White,
                        disabledContainerColor = if (isDark) Color(0xFF333333) else Color(0xFFCCCCCC)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isExtracting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("EXTRACTING...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else {
                        Text("EXTRACT TEXT", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
