package com.outlandishomar.purepdf

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.activity.compose.BackHandler

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
    val context = LocalContext.current

    BackHandler(onBack = onBackClick)

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

    // About App screen navigation
    var showAboutScreen by remember { mutableStateOf(false) }

    if (showAboutScreen) {
        AboutAppScreen(onBackClick = { showAboutScreen = false })
        return
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
                onClick = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Check out PurePdf!")
                        putExtra(Intent.EXTRA_TEXT, "Here is a great PDF app: https://github.com/outlandishomar/PurePdf/releases")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share PurePdf via"))
                }
            )

            HorizontalDivider(
                color = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE0E0E0),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // About App
            SettingsItemDrawable(
                iconRes = R.drawable.information_icon,
                title = "About App",
                subtitle = "App info and credits",
                isDark = isDark,
                onClick = { showAboutScreen = true }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(onBackClick: () -> Unit) {
    BackHandler(onBack = onBackClick)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("About App", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val uriHandler = LocalUriHandler.current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(48.dp)) // push down from top app bar
                Icon(
                    painter = painterResource(id = R.drawable.danewicon),
                    contentDescription = "App Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(130.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PurePdf",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) Color.White else Color.Black
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "PurePdf is a lightweight, all-in-one PDF manager designed with your security in mind.\n\n" +
                           "• Privacy Focused: No unnecessary permissions required. Your files and data stay strictly on your device.\n" +
                           "• Modern UI & Easy to Use: A clean, intuitive design built to give you the smoothest reading and navigation experience.\n" +
                           "• Lots of Tools: Merge, split, extract text, and fully manage your documents with powerful built-in tools, completely for free.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { uriHandler.openUri("https://github.com/outlandishomar/PurePdf") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.github_142),
                        contentDescription = "GitHub Repository",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // Flexible spacer to push the elements to the bottom
                Spacer(modifier = Modifier.weight(1f))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                ) {
                    Text(
                        text = "v3.0",
                        fontSize = 14.sp,
                        color = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                    
                    Text(
                        text = "Privacy Policy",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clickable { uriHandler.openUri("https://outlandishomar.github.io/PurePdf/PRIVACY.html") }
                    )
                }
            }
        }
    }
}
