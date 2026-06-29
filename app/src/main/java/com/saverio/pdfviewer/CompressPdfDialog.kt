package com.saverio.pdfviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val PurePdfRed = Color(0xFFCC3333)

@Composable
fun CompressPdfDialog(
    fileName: String,
    fileSize: Long,
    onDismiss: () -> Unit,
    onCompress: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var selectedQuality by remember { mutableStateOf("high") }

    val fileSizeText = when {
        fileSize >= 1024 * 1024 -> String.format("%.1f MB", fileSize / (1024.0 * 1024.0))
        fileSize >= 1024 -> "${fileSize / 1024} kB"
        else -> "$fileSize bytes"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1A2332) else Color.White
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Compress, contentDescription = null,
                            tint = PurePdfRed, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Compress PDF", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            color = if (isDark) Color.White else Color.Black)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close",
                            tint = PurePdfRed, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // File info card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF253545) else Color(0xFFF5F5F5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PurePdfRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PDF", color = PurePdfRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(fileName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                color = if (isDark) Color.White else Color.Black,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(fileSizeText, fontSize = 12.sp,
                                color = if (isDark) Color.LightGray else Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text("Compression Quality", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (isDark) Color.White else Color.Black)
                Spacer(Modifier.height(8.dp))

                QualityOption("High Quality", "(Smaller reduction)", selectedQuality == "high", isDark) { selectedQuality = "high" }
                QualityOption("Medium Quality", "(Balanced)", selectedQuality == "medium", isDark) { selectedQuality = "medium" }
                QualityOption("Low Quality", "(Maximum compression)", selectedQuality == "low", isDark) { selectedQuality = "low" }

                Spacer(Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = if (isDark) Color.LightGray else Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onCompress(selectedQuality) },
                        colors = ButtonDefaults.buttonColors(containerColor = PurePdfRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Compress", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityOption(title: String, sub: String, selected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = PurePdfRed, unselectedColor = if (isDark) Color.LightGray else Color.Gray))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (isDark) Color.White else Color.Black)
            Text(sub, fontSize = 12.sp, color = if (isDark) Color.LightGray else Color.Gray)
        }
    }
}
