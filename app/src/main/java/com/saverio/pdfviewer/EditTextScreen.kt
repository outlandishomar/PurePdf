package com.saverio.pdfviewer

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PurePdfRed = Color(0xFFCC3333)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTextScreen(
    pdfUri: Uri,
    pdfFileName: String,
    onBack: () -> Unit,
    onSaveAsNew: (String, String) -> Unit,
    onOverwriteOriginal: (Uri, String) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var isLoading by remember { mutableStateOf(true) }
    var editedText by remember { mutableStateOf("") }
    var loadError by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    // Extract all text from PDF
    LaunchedEffect(pdfUri) {
        withContext(Dispatchers.IO) {
            try {
                PDFBoxResourceLoader.init(context)
                val inputStream = context.contentResolver.openInputStream(pdfUri)
                if (inputStream != null) {
                    val doc = PDDocument.load(inputStream)
                    val stripper = PDFTextStripper()
                    val text = stripper.getText(doc)
                    doc.close()
                    inputStream.close()
                    withContext(Dispatchers.Main) {
                        editedText = text.trim()
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadError = true
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    loadError = true
                    isLoading = false
                }
            }
        }
    }

    // Save options dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
            title = {
                Text(
                    "Save Options",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            showSaveDialog = false
                            showNameDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurePdfRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Save as New PDF", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showSaveDialog = false
                            onOverwriteOriginal(pdfUri, editedText)
                        },
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(PurePdfRed)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Overwrite Original", fontWeight = FontWeight.Bold, color = PurePdfRed)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = if (isDark) Color.LightGray else Color.Gray)
                }
            }
        )
    }

    // Name dialog for Save as New
    if (showNameDialog) {
        var fileName by remember { mutableStateOf(pdfFileName.removeSuffix(".pdf") + "_edited") }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
            title = {
                Text(
                    "Save as",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black
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
                        focusedTextColor = if (isDark) Color.White else Color.Black,
                        unfocusedTextColor = if (isDark) Color.White else Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    onSaveAsNew(editedText, fileName.trim().ifBlank { "EditedPDF" })
                }) {
                    Text("Save", color = PurePdfRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel", color = if (isDark) Color.LightGray else Color.Gray)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text("Edit Text", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isLoading && !loadError) {
                        IconButton(onClick = { showSaveDialog = true }) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = PurePdfRed)
                        }
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
        when {
            isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PurePdfRed)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Extracting text...",
                            color = if (isDark) Color.LightGray else Color.Gray
                        )
                    }
                }
            }
            loadError -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Failed to extract text from PDF",
                        color = if (isDark) Color.LightGray else Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
            else -> {
                // Detect text direction from first letter character
                val isRtl = remember(editedText) {
                    editedText.firstOrNull { it.isLetter() }?.let {
                        val dir = Character.getDirectionality(it)
                        dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                                dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
                    } ?: false
                }
                val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black,
                            cursorColor = PurePdfRed,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 15.sp,
                            lineHeight = 24.sp
                        )
                    )
                }
            }
        }
    }
}
