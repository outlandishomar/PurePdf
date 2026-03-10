package com.outlandishomar.purepdf

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.provider.OpenableColumns
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

// Simple data class for recent files
data class RecentPdf(val uri: String, val name: String, val timestamp: Long, val isFavorite: Boolean = false)

object RecentsManager {
    private const val RECENT_FILES = "recents"
    
    fun saveRecent(context: Context, uri: Uri, name: String) {
        val prefs = context.getSharedPreferences(RECENT_FILES, Context.MODE_PRIVATE)
        val currentList = getRecents(context).toMutableList()
        val existingItem = currentList.find { it.uri == uri.toString() }
        val isFav = existingItem?.isFavorite ?: false
        
        // Remove existing item to move to top
        currentList.removeAll { it.uri == uri.toString() }
        
        // Add new item to top
        currentList.add(0, RecentPdf(uri.toString(), name, System.currentTimeMillis(), isFav))
        
        // Keep only top 20
        if (currentList.size > 20) {
            currentList.removeLast()
        }
        
        // Serialize
        val serializedString = currentList.joinToString(";;;") { "${it.uri}|${it.name}|${it.timestamp}|${it.isFavorite}" }
        prefs.edit().putString(RECENT_FILES, serializedString).apply()
    }
    
    fun getRecents(context: Context): List<RecentPdf> {
        val prefs = context.getSharedPreferences(RECENT_FILES, Context.MODE_PRIVATE)
        val recentString = prefs.getString(RECENT_FILES, "") ?: ""
        if (recentString.isBlank()) return emptyList()
        
        return recentString.split(";;;").mapNotNull { part ->
            val data = part.split("|")
            if (data.size >= 3) {
                val isFav = if (data.size >= 4) data[3].toBoolean() else false
                RecentPdf(data[0], data[1], data[2].toLongOrNull() ?: 0L, isFav)
            } else null
        }
    }

    fun removeRecent(context: Context, uri: Uri) {
        val prefs = context.getSharedPreferences(RECENT_FILES, Context.MODE_PRIVATE)
        val currentList = getRecents(context).toMutableList()
        currentList.removeAll { it.uri == uri.toString() }
        val serializedString = currentList.joinToString(";;;") { "${it.uri}|${it.name}|${it.timestamp}|${it.isFavorite}" }
        prefs.edit().putString(RECENT_FILES, serializedString).apply()
    }

    fun toggleFavorite(context: Context, uri: Uri) {
        val prefs = context.getSharedPreferences(RECENT_FILES, Context.MODE_PRIVATE)
        val currentList = getRecents(context).toMutableList()
        val index = currentList.indexOfFirst { it.uri == uri.toString() }
        if (index != -1) {
            val item = currentList[index]
            currentList[index] = item.copy(isFavorite = !item.isFavorite)
            val serializedString = currentList.joinToString(";;;") { "${it.uri}|${it.name}|${it.timestamp}|${it.isFavorite}" }
            prefs.edit().putString(RECENT_FILES, serializedString).apply()
        }
    }
}

fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Unknown Document"
}

class MainActivity : AppCompatActivity() {

    val RECENT_FILES = "recents"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val openPdfLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
                val name = getFileName(this, uri)
                RecentsManager.saveRecent(this, uri, name)
                openPDFFile(uri)
            }
        }

        val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                convertImagesToPdf(uris)
            }
        }

        // Merge PDF state - needs to be outside setContent so the launcher callback can set it
        val pendingMergeUris = mutableStateOf<List<Uri>>(emptyList())

        val mergePdfPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.size >= 2) {
                uris.forEach { uri ->
                    try {
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (_: SecurityException) {}
                }
                pendingMergeUris.value = uris
            } else if (uris.isNotEmpty()) {
                Toast.makeText(this, "Please select at least 2 PDFs to merge", Toast.LENGTH_SHORT).show()
            }
        }

        // Split PDF state
        val splitPdfUri = mutableStateOf<Uri?>(null)

        val splitPdfPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {}
                splitPdfUri.value = uri
            }
        }

        // Manage Pages state
        val managePagesUri = mutableStateOf<Uri?>(null)

        val managePagesPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {}
                managePagesUri.value = uri
            }
        }

        // Extract Text state
        val extractTextUri = mutableStateOf<Uri?>(null)
        val extractTextFileName = mutableStateOf("")

        val extractTextPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {}
                extractTextUri.value = uri
                extractTextFileName.value = getFileName(this, uri)
            }
        }





        // Watermark state
        val watermarkUri = mutableStateOf<Uri?>(null)
        val watermarkFileName = mutableStateOf("")
        val watermarkPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
                watermarkUri.value = uri
                watermarkFileName.value = getFileName(this, uri)
            }
        }

        // Signature state
        val signatureUri = mutableStateOf<Uri?>(null)
        val signatureFileName = mutableStateOf("")
        val signaturePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
                signatureUri.value = uri
                signatureFileName.value = getFileName(this, uri)
            }
        }
        
        handleIncomingIntent(intent)

        setContent {
            val prefs = remember { getSharedPreferences("purepdf_settings", MODE_PRIVATE) }
            var themeMode by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }
            // Override the configuration so isSystemInDarkTheme() respects the app setting everywhere
            val currentConfig = LocalConfiguration.current
            val overriddenConfig = remember(darkTheme, currentConfig) {
                Configuration(currentConfig).apply {
                    uiMode = if (darkTheme) {
                        (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or android.content.res.Configuration.UI_MODE_NIGHT_YES
                    } else {
                        (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or android.content.res.Configuration.UI_MODE_NIGHT_NO
                    }
                }
            }
            CompositionLocalProvider(LocalConfiguration provides overriddenConfig) {
                MaterialTheme(
                    colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
                ) {
                var recentItems by remember { mutableStateOf(emptyList<RecentPdf>()) }
                val lifecycleOwner = LocalLifecycleOwner.current
                
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            recentItems = RecentsManager.getRecents(this@MainActivity)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                
                HomeScreen(
                    recentItems = recentItems,
                    onOpenPdfClick = { openPdfLauncher.launch(arrayOf("application/pdf")) },
                    onRecentClick = { item -> 
                        val uri = Uri.parse(item.uri)
                        try {
                            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            RecentsManager.saveRecent(this@MainActivity, uri, item.name)
                            openPDFFile(uri)
                        } catch (e: SecurityException) {
                            Toast.makeText(this@MainActivity, "File unavailable", Toast.LENGTH_SHORT).show()
                            RecentsManager.removeRecent(this@MainActivity, uri)
                            recentItems = RecentsManager.getRecents(this@MainActivity)
                        }
                    },
                    onToggleFavorite = { item ->
                        val uri = Uri.parse(item.uri)
                        RecentsManager.toggleFavorite(this@MainActivity, uri)
                        recentItems = RecentsManager.getRecents(this@MainActivity)
                    },
                    onPhotoToPdfClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    onMergePdfClick = {
                        mergePdfPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    onSplitPdfClick = {
                        splitPdfPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    onManagePagesClick = {
                        managePagesPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    onExtractTextClick = {
                        extractTextPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    onWatermarkClick = {
                        watermarkPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    onSignatureClick = {
                        signaturePickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    onThemeChanged = { newTheme ->
                        prefs.edit().putString("theme", newTheme).apply()
                        themeMode = newTheme
                    },
                    currentTheme = themeMode
                )

                // Show naming dialog when PDFs are picked for merging
                if (pendingMergeUris.value.isNotEmpty()) {
                    MergePdfNameDialog(
                        onDismiss = { pendingMergeUris.value = emptyList() },
                        onConfirm = { fileName ->
                            val urisToMerge = pendingMergeUris.value
                            pendingMergeUris.value = emptyList()
                            mergePdfs(urisToMerge, fileName)
                        }
                    )
                }

                // Show split screen when a PDF is picked for splitting
                if (splitPdfUri.value != null) {
                    SplitPdfScreen(
                        pdfUri = splitPdfUri.value!!,
                        onBack = { splitPdfUri.value = null },
                        onSplit = { uri, selectedPages, fileName ->
                            splitPdfUri.value = null
                            splitPdf(uri, selectedPages, fileName)
                        }
                    )
                }

                // Show manage pages screen
                if (managePagesUri.value != null) {
                    ManagePagesScreen(
                        pdfUri = managePagesUri.value!!,
                        onBack = { managePagesUri.value = null },
                        onSave = { uri, pages, fileName ->
                            managePagesUri.value = null
                            saveManagePages(pages, fileName)
                        }
                    )
                }

                // Show extract text screen
                if (extractTextUri.value != null) {
                    ExtractTextScreen(
                        pdfUri = extractTextUri.value!!,
                        pdfFileName = extractTextFileName.value,
                        onBack = { extractTextUri.value = null }
                    )
                }



                // Watermark screen
                if (watermarkUri.value != null) {
                    WatermarkScreen(
                        pdfUri = watermarkUri.value!!,
                        pdfFileName = watermarkFileName.value,
                        onBack = { watermarkUri.value = null },
                        onApply = { uri, config ->
                            watermarkUri.value = null
                            applyWatermark(uri, config)
                        }
                    )
                }

                // Signature screen
                if (signatureUri.value != null) {
                    SignatureScreen(
                        pdfUri = signatureUri.value!!,
                        pdfFileName = signatureFileName.value,
                        onBack = { signatureUri.value = null },
                        onApply = { uri, result ->
                            signatureUri.value = null
                            applySignature(uri, result)
                        }
                    )
                }
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    private fun handleIncomingIntent(incomingIntent: Intent?) {
        if (incomingIntent?.action == Intent.ACTION_VIEW && incomingIntent.data != null) {
            val uri = incomingIntent.data!!
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            val name = getFileName(this, uri)
            RecentsManager.saveRecent(this, uri, name)
            openPDFFile(uri)
        }
    }

    fun loadFavourites() {
        val recentFiles: String? =
            getSharedPreferences(RECENT_FILES, Context.MODE_PRIVATE).getString(RECENT_FILES, "")
        var recentFilesToSave = ""
        var recentFilesParts = recentFiles?.split("/:::/")
        var i = 0
        var added_i = 0
        while (i < recentFilesParts!!.size) {
            val recentFilesParts2 = recentFilesParts[i].split(":::")

            if (recentFilesParts2.size == 3 && recentFilesParts2[2] == "true") {
                if (added_i > 0) recentFilesToSave += "/:::/"
                recentFilesToSave += "${recentFilesParts2[0]}:::${recentFilesParts2[1]}:::${recentFilesParts2[2]}"
                added_i++
                //println("Recent ${i}:\ndate:${recentFilesParts2[0]}\nuri:${recentFilesParts2[1]}\nfavourite:${recentFilesParts2[2]}\n\n")
            }
            i++
        }
    }

    fun openPDFFile(uri: Uri? = null) {
        val intent = Intent(this@MainActivity, PDFViewer::class.java)
        if (uri != null) {
            intent.data = uri
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val uriToOpen = Bundle()
        uriToOpen.putString("uri", uri?.toString() ?: "") //Your id
        intent.putExtras(uriToOpen) //Put your id to your next Intent
        startActivity(intent)
    }

    private fun convertImagesToPdf(imageUris: List<Uri>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val maxDimension = 1920 // Scale down large images

                imageUris.forEachIndexed { index, uri ->
                    val inputStream = contentResolver.openInputStream(uri) ?: return@forEachIndexed

                    // Decode bounds first to calculate scale factor
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()

                    val origWidth = options.outWidth
                    val origHeight = options.outHeight
                    var sampleSize = 1
                    if (origWidth > maxDimension || origHeight > maxDimension) {
                        val widthRatio = origWidth.toFloat() / maxDimension
                        val heightRatio = origHeight.toFloat() / maxDimension
                        sampleSize = maxOf(widthRatio, heightRatio).toInt().coerceAtLeast(1)
                    }

                    // Decode scaled bitmap
                    val scaledStream = contentResolver.openInputStream(uri) ?: return@forEachIndexed
                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val bitmap = BitmapFactory.decodeStream(scaledStream, null, decodeOptions)
                    scaledStream.close()

                    if (bitmap != null) {
                        val pageInfo = PdfDocument.PageInfo.Builder(
                            bitmap.width, bitmap.height, index + 1
                        ).create()
                        val page = pdfDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(page)
                        bitmap.recycle()
                    }
                }

                // Save to internal storage
                val outputDir = java.io.File(filesDir, "converted_pdfs")
                if (!outputDir.exists()) outputDir.mkdirs()

                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    .format(java.util.Date())
                val outputFile = java.io.File(outputDir, "PhotoToPDF_$timestamp.pdf")

                java.io.FileOutputStream(outputFile).use { fos ->
                    pdfDocument.writeTo(fos)
                }
                pdfDocument.close()

                val fileUri = Uri.fromFile(outputFile)
                RecentsManager.saveRecent(this@MainActivity, fileUri, outputFile.name)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "PDF created: ${outputFile.name}", Toast.LENGTH_SHORT).show()
                    openPDFFile(fileUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mergePdfs(uris: List<Uri>, fileName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val outputPdf = PdfDocument()
                var pageNum = 0

                for (uri in uris) {
                    val pfd = contentResolver.openFileDescriptor(uri, "r") ?: continue
                    val renderer = PdfRenderer(pfd)

                    for (i in 0 until renderer.pageCount) {
                        val srcPage = renderer.openPage(i)
                        val bitmap = Bitmap.createBitmap(
                            srcPage.width, srcPage.height, Bitmap.Config.ARGB_8888
                        )
                        srcPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        srcPage.close()

                        pageNum++
                        val pageInfo = PdfDocument.PageInfo.Builder(
                            bitmap.width, bitmap.height, pageNum
                        ).create()
                        val outPage = outputPdf.startPage(pageInfo)
                        outPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        outputPdf.finishPage(outPage)
                        bitmap.recycle()
                    }
                    renderer.close()
                    pfd.close()
                }

                val outputDir = java.io.File(filesDir, "merged_pdfs")
                if (!outputDir.exists()) outputDir.mkdirs()
                val cleanName = fileName.removeSuffix(".pdf")
                val outputFile = java.io.File(outputDir, "$cleanName.pdf")

                java.io.FileOutputStream(outputFile).use { fos ->
                    outputPdf.writeTo(fos)
                }
                outputPdf.close()

                val fileUri = Uri.fromFile(outputFile)
                RecentsManager.saveRecent(this@MainActivity, fileUri, outputFile.name)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Merged: ${outputFile.name}", Toast.LENGTH_SHORT).show()
                    openPDFFile(fileUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to merge PDFs", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun splitPdf(uri: Uri, selectedPages: List<Int>, fileName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return@launch
                val renderer = PdfRenderer(pfd)
                val outputPdf = PdfDocument()

                val sortedPages = selectedPages.sorted()
                var outPageNum = 0

                for (pageIndex in sortedPages) {
                    if (pageIndex >= renderer.pageCount) continue
                    val srcPage = renderer.openPage(pageIndex)
                    val bitmap = Bitmap.createBitmap(
                        srcPage.width, srcPage.height, Bitmap.Config.ARGB_8888
                    )
                    srcPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    srcPage.close()

                    outPageNum++
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        bitmap.width, bitmap.height, outPageNum
                    ).create()
                    val outPage = outputPdf.startPage(pageInfo)
                    outPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    outputPdf.finishPage(outPage)
                    bitmap.recycle()
                }

                renderer.close()
                pfd.close()

                val outputDir = java.io.File(filesDir, "split_pdfs")
                if (!outputDir.exists()) outputDir.mkdirs()
                val cleanName = fileName.removeSuffix(".pdf")
                val outputFile = java.io.File(outputDir, "$cleanName.pdf")

                java.io.FileOutputStream(outputFile).use { fos ->
                    outputPdf.writeTo(fos)
                }
                outputPdf.close()

                val fileUri = Uri.fromFile(outputFile)
                RecentsManager.saveRecent(this@MainActivity, fileUri, outputFile.name)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Split: ${outputFile.name}", Toast.LENGTH_SHORT).show()
                    openPDFFile(fileUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to split PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveManagePages(pages: List<PageItem>, fileName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val outputPdf = PdfDocument()
                var pageNum = 0

                for (pageItem in pages) {
                    val bitmap = pageItem.bitmap
                    // For saving, use a higher resolution version
                    val saveWidth = bitmap.width * 2
                    val saveHeight = bitmap.height * 2
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, saveWidth, saveHeight, true)

                    pageNum++
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        scaledBitmap.width, scaledBitmap.height, pageNum
                    ).create()
                    val outPage = outputPdf.startPage(pageInfo)
                    outPage.canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
                    outputPdf.finishPage(outPage)
                    if (scaledBitmap !== bitmap) scaledBitmap.recycle()
                }

                val outputDir = java.io.File(filesDir, "managed_pdfs")
                if (!outputDir.exists()) outputDir.mkdirs()
                val cleanName = fileName.removeSuffix(".pdf")
                val outputFile = java.io.File(outputDir, "$cleanName.pdf")

                java.io.FileOutputStream(outputFile).use { fos ->
                    outputPdf.writeTo(fos)
                }
                outputPdf.close()

                val fileUri = Uri.fromFile(outputFile)
                RecentsManager.saveRecent(this@MainActivity, fileUri, outputFile.name)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Saved: ${outputFile.name}", Toast.LENGTH_SHORT).show()
                    openPDFFile(fileUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to save changes", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createPdfFromText(text: String, outputFile: java.io.File) {
        val document = PdfDocument()
        val paint = android.text.TextPaint().apply {
            textSize = 12f
            color = android.graphics.Color.BLACK
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT
        }

        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        val margin = 50
        val textWidth = pageWidth - 2 * margin
        val maxTextHeight = pageHeight - 2 * margin

        if (text.isBlank()) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            document.finishPage(page)
            java.io.FileOutputStream(outputFile).use { document.writeTo(it) }
            document.close()
            return
        }

        var offset = 0
        var pageNum = 0

        while (offset < text.length) {
            pageNum++
            val remaining = text.substring(offset)

            val fullLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.text.StaticLayout.Builder
                    .obtain(remaining, 0, remaining.length, paint, textWidth)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.5f)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                android.text.StaticLayout(
                    remaining, paint, textWidth,
                    android.text.Layout.Alignment.ALIGN_NORMAL, 1.5f, 0f, false
                )
            }

            // Find how many lines fit on this page
            var lastLine = 0
            for (i in 0 until fullLayout.lineCount) {
                if (fullLayout.getLineBottom(i) > maxTextHeight) break
                lastLine = i
            }

            val charEnd = fullLayout.getLineEnd(lastLine)
            val pageText = remaining.substring(0, charEnd)

            val pageLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.text.StaticLayout.Builder
                    .obtain(pageText, 0, pageText.length, paint, textWidth)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.5f)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                android.text.StaticLayout(
                    pageText, paint, textWidth,
                    android.text.Layout.Alignment.ALIGN_NORMAL, 1.5f, 0f, false
                )
            }

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            val page = document.startPage(pageInfo)
            page.canvas.save()
            page.canvas.translate(margin.toFloat(), margin.toFloat())
            pageLayout.draw(page.canvas)
            page.canvas.restore()
            document.finishPage(page)

            offset += charEnd
        }

        java.io.FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
    }

    private fun saveEditedTextAsNew(text: String, fileName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val outputDir = java.io.File(filesDir, "edited_pdfs")
                if (!outputDir.exists()) outputDir.mkdirs()
                val cleanName = fileName.removeSuffix(".pdf")
                val outputFile = java.io.File(outputDir, "$cleanName.pdf")

                createPdfFromText(text, outputFile)

                val fileUri = Uri.fromFile(outputFile)
                RecentsManager.saveRecent(this@MainActivity, fileUri, outputFile.name)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Saved: ${outputFile.name}", Toast.LENGTH_SHORT).show()
                    openPDFFile(fileUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun overwriteOriginalPdf(uri: Uri, text: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Generate new PDF in cache/temp
                val tempFile = java.io.File(cacheDir, "temp_edit_${System.currentTimeMillis()}.pdf")
                createPdfFromText(text, tempFile)

                // Step 2: Verify temp file was created successfully
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    throw Exception("Failed to create temporary PDF")
                }

                // Step 3: Overwrite the original
                val scheme = uri.scheme
                if (scheme == "file") {
                    // Direct file access
                    val originalFile = java.io.File(uri.path!!)
                    val originalName = originalFile.name
                    val originalParent = originalFile.parentFile
                    originalFile.delete()
                    val targetFile = java.io.File(originalParent, originalName)
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                } else {
                    // Content URI - use contentResolver
                    val outputStream = contentResolver.openOutputStream(uri, "wt")
                    if (outputStream != null) {
                        tempFile.inputStream().use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile.delete()
                    } else {
                        tempFile.delete()
                        throw Exception("Cannot write to original file")
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Original PDF overwritten successfully", Toast.LENGTH_SHORT).show()
                    openPDFFile(uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Clean up temp file
                java.io.File(cacheDir, "temp_edit_*.pdf").let {
                    if (it.exists()) it.delete()
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to overwrite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun applyWatermark(uri: Uri, config: WatermarkConfig) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fd = contentResolver.openFileDescriptor(uri, "r") ?: return@launch
                val renderer = PdfRenderer(fd)
                val document = PdfDocument()
                val scale = 2f

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val w = (page.width * scale).toInt()
                    val h = (page.height * scale).toInt()
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val canvas = android.graphics.Canvas(bmp)
                    val pos = config.position
                    if (config.type == "text") {
                        val paint = android.graphics.Paint().apply {
                            color = config.colorArgb
                            alpha = (config.opacity * 255).toInt()
                            textSize = 24f * config.scale * scale
                            isAntiAlias = true; isFakeBoldText = true
                        }
                        val textW = paint.measureText(config.text)
                        val x = getWatermarkX(pos, w.toFloat(), textW)
                        val y = getWatermarkY(pos, h.toFloat(), paint.textSize)
                        canvas.save()
                        canvas.rotate(config.rotation, x + textW / 2, y)
                        canvas.drawText(config.text, x, y, paint)
                        canvas.restore()
                    } else if (config.imageBitmap != null) {
                        val imgW = (config.imageBitmap.width * config.scale).toInt()
                        val imgH = (config.imageBitmap.height * config.scale).toInt()
                        val scaled = Bitmap.createScaledBitmap(config.imageBitmap, imgW, imgH, true)
                        val paint = android.graphics.Paint().apply { alpha = (config.opacity * 255).toInt() }
                        val x = getWatermarkX(pos, w.toFloat(), imgW.toFloat())
                        val y = getWatermarkY(pos, h.toFloat(), imgH.toFloat())
                        canvas.save()
                        canvas.rotate(config.rotation, x + imgW / 2f, y + imgH / 2f)
                        canvas.drawBitmap(scaled, x, y, paint)
                        canvas.restore()
                    }

                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                    val pdfPage = document.startPage(pageInfo)
                    pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                    document.finishPage(pdfPage)
                    bmp.recycle()
                }
                renderer.close(); fd.close()

                val outputDir = java.io.File(filesDir, "watermarked_pdfs")
                if (!outputDir.exists()) outputDir.mkdirs()
                val name = getFileName(this@MainActivity, uri).removeSuffix(".pdf")
                val outputFile = java.io.File(outputDir, "${name}_watermarked.pdf")
                java.io.FileOutputStream(outputFile).use { document.writeTo(it) }
                document.close()

                val fileUri = Uri.fromFile(outputFile)
                RecentsManager.saveRecent(this@MainActivity, fileUri, outputFile.name)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Watermark applied!", Toast.LENGTH_SHORT).show()
                    openPDFFile(fileUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Failed to apply watermark", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun getWatermarkX(pos: String, pageW: Float, objW: Float): Float {
        return when {
            pos.contains("Left") -> 40f
            pos.contains("Right") -> pageW - objW - 40f
            else -> (pageW - objW) / 2f
        }
    }

    private fun getWatermarkY(pos: String, pageH: Float, objH: Float): Float {
        return when {
            pos.contains("Top") -> objH + 40f
            pos.contains("Bottom") -> pageH - 40f
            else -> pageH / 2f
        }
    }

    private fun applySignature(uri: Uri, result: SignatureResult) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fd = contentResolver.openFileDescriptor(uri, "r") ?: return@launch
                val renderer = PdfRenderer(fd)
                val document = PdfDocument()
                val scale = 2f

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val w = (page.width * scale).toInt()
                    val h = (page.height * scale).toInt()
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    // Apply signature only on the selected page
                    if (i == result.pageIndex) {
                        val canvas = android.graphics.Canvas(bmp)
                        val sigW = (result.bitmap.width * result.scale).toInt()
                        val sigH = (result.bitmap.height * result.scale).toInt()
                        val scaled = Bitmap.createScaledBitmap(result.bitmap, sigW, sigH, true)
                        val x = result.offsetXRatio * w
                        val y = result.offsetYRatio * h
                        canvas.save()
                        canvas.rotate(result.rotation, x + sigW / 2f, y + sigH / 2f)
                        canvas.drawBitmap(scaled, x, y, null)
                        canvas.restore()
                    }

                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                    val pdfPage = document.startPage(pageInfo)
                    pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                    document.finishPage(pdfPage)
                    bmp.recycle()
                }
                renderer.close(); fd.close()

                val outputDir = java.io.File(filesDir, "signed_pdfs")
                if (!outputDir.exists()) outputDir.mkdirs()
                val name = getFileName(this@MainActivity, uri).removeSuffix(".pdf")
                val outputFile = java.io.File(outputDir, "${name}_signed.pdf")
                java.io.FileOutputStream(outputFile).use { document.writeTo(it) }
                document.close()

                val fileUri = Uri.fromFile(outputFile)
                RecentsManager.saveRecent(this@MainActivity, fileUri, outputFile.name)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Signature applied!", Toast.LENGTH_SHORT).show()
                    openPDFFile(fileUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Failed to apply signature", Toast.LENGTH_SHORT).show() }
            }
        }
    }
}