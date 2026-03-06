package com.saverio.pdfviewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PurePdfRed = Color(0xFFCC3333)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onThemeChanged: (String) -> Unit,
    currentTheme: String
) {
    val isDark = isSystemInDarkTheme()
    var showThemeDialog by remember { mutableStateOf(false) }

    val themeLabel = when (currentTheme) {
        "light" -> "Light"
        "dark" -> "Dark"
        else -> "Device Default"
    }

    // Theme picker dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
            title = {
                Text(
                    "Choose Theme",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black
                )
            },
            text = {
                Column {
                    ThemeOption(
                        label = "Device Default",
                        selected = currentTheme == "system",
                        isDark = isDark,
                        onClick = {
                            onThemeChanged("system")
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        label = "Light",
                        selected = currentTheme == "light",
                        isDark = isDark,
                        onClick = {
                            onThemeChanged("light")
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        label = "Dark",
                        selected = currentTheme == "dark",
                        isDark = isDark,
                        onClick = {
                            onThemeChanged("dark")
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel", color = if (isDark) Color.LightGray else Color.Gray)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 8.dp)
        ) {
            // Languages
            SettingsItemDrawable(
                iconRes = R.drawable.language_icon,
                title = "Languages",
                subtitle = "Choose your preferred language",
                isDark = isDark,
                onClick = { /* placeholder - not functional yet */ }
            )

            HorizontalDivider(
                color = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE0E0E0),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Theme
            SettingsItemVector(
                icon = Icons.Default.DarkMode,
                title = "Theme",
                subtitle = themeLabel,
                isDark = isDark,
                onClick = { showThemeDialog = true }
            )

            HorizontalDivider(
                color = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE0E0E0),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Share App
            SettingsItemDrawable(
                iconRes = R.drawable.navigate_icon,
                title = "Share App",
                subtitle = "Share with friends and family",
                isDark = isDark,
                onClick = { /* placeholder - not functional yet */ }
            )

            HorizontalDivider(
                color = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE0E0E0),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // About The App
            SettingsItemDrawable(
                iconRes = R.drawable.information_icon,
                title = "About The App",
                subtitle = "App info and credits",
                isDark = isDark,
                onClick = { /* placeholder - not functional yet */ }
            )
        }
    }
}

@Composable
private fun SettingsItemDrawable(
    iconRes: Int,
    title: String,
    subtitle: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            tint = PurePdfRed,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (isDark) Color.White else Color.Black
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = if (isDark) Color.LightGray else Color.Gray
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isDark) Color.Gray else Color.LightGray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SettingsItemVector(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = PurePdfRed,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (isDark) Color.White else Color.Black
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = if (isDark) Color.LightGray else Color.Gray
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isDark) Color.Gray else Color.LightGray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = PurePdfRed,
                unselectedColor = if (isDark) Color.LightGray else Color.Gray
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            color = if (isDark) Color.White else Color.Black
        )
    }
}
