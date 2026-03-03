package com.example.purepdf

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.purepdf.ui.HomeScreen
import com.example.purepdf.ui.PdfViewerScreen
import com.example.purepdf.ui.RecentPdf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PurePDF"
        private const val A4_WIDTH_PT  = 595
        private const val A4_HEIGHT_PT = 842
        private const val PREFS_NAME = "purepdf_prefs"
        private const val KEY_RECENT_FILES = "recent_files"
        private const val MAX_RECENT = 20
    }

    private lateinit var prefs: SharedPreferences
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var pendingImageUris: List<Uri> = emptyList()

    // Navigation state — no heavy objects held here
    private var isPdfVisible by mutableStateOf(false)
    private var currentPdfUri by mutableStateOf<Uri?>(null)

    private val recentFiles = mutableStateListOf<RecentPdf>()

    private val openPdfLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addToRecent(uri)
            currentPdfUri = uri
            isPdfVisible = true
        }
    }

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            pendingImageUris = uris
            saveConvertedPdfLauncher.launch("converted.pdf")
        }
    }

    private val saveConvertedPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { dest ->
        if (dest != null && pendingImageUris.isNotEmpty()) {
            convertImagesToPdf(pendingImageUris, dest)
            pendingImageUris = emptyList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        loadRecentFiles()

        setContent {
            MaterialTheme {
                val uri = currentPdfUri
                if (isPdfVisible && uri != null) {
                    PdfViewerScreen(
                        uri = uri,
                        contentResolver = contentResolver,
                        screenWidth = resources.displayMetrics.widthPixels,
                        onBack = {
                            isPdfVisible = false
                            currentPdfUri = null
                        }
                    )
                } else {
                    HomeScreen(
                        recentList = recentFiles,
                        onOpenFilePicker = {
                            openPdfLauncher.launch(arrayOf("application/pdf"))
                        },
                        onRecentItemClick = { recent -> openRecentPdf(recent) }
                    )
                }
            }
        }
    }

    // ── Navigation ──

    private fun openRecentPdf(recent: RecentPdf) {
        val uri = Uri.parse(recent.uri)
        try {
            // Quick permission check — don't open the full file here
            contentResolver.openFileDescriptor(uri, "r")?.close()
            addToRecent(uri, recent.displayName)
            currentPdfUri = uri
            isPdfVisible = true
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission lost: ${recent.displayName}", e)
            Toast.makeText(this, "File no longer available", Toast.LENGTH_SHORT).show()
            recentFiles.removeAll { it.uri == recent.uri }
            saveRecentFiles()
        } catch (e: java.io.FileNotFoundException) {
            Log.w(TAG, "File not found: ${recent.displayName}", e)
            Toast.makeText(this, "File no longer available", Toast.LENGTH_SHORT).show()
            recentFiles.removeAll { it.uri == recent.uri }
            saveRecentFiles()
        }
    }

    private fun addToRecent(uri: Uri, knownName: String? = null) {
        val displayName = knownName ?: getFileDisplayName(uri)
        recentFiles.removeAll { it.uri == uri.toString() }
        recentFiles.add(0, RecentPdf(
            uri = uri.toString(),
            displayName = displayName,
            lastOpenedEpochMillis = System.currentTimeMillis()
        ))
        while (recentFiles.size > MAX_RECENT) recentFiles.removeLast()
        saveRecentFiles()
    }

    private fun getFileDisplayName(uri: Uri): String {
        var name = "Document.pdf"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx) ?: name
        }
        return name
    }

    // ── SharedPreferences persistence ──

    private fun saveRecentFiles() {
        val arr = org.json.JSONArray()
        for (pdf in recentFiles) {
            val obj = org.json.JSONObject()
            obj.put("uri", pdf.uri)
            obj.put("name", pdf.displayName)
            obj.put("ts", pdf.lastOpenedEpochMillis)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_RECENT_FILES, arr.toString()).apply()
    }

    private fun loadRecentFiles() {
        val json = prefs.getString(KEY_RECENT_FILES, null) ?: return
        try {
            val arr = org.json.JSONArray(json)
            recentFiles.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                recentFiles.add(RecentPdf(
                    uri = obj.getString("uri"),
                    displayName = obj.getString("name"),
                    lastOpenedEpochMillis = obj.getLong("ts")
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse recent files", e)
        }
    }

    // ── Image to PDF conversion ──

    private fun convertImagesToPdf(imageUris: List<Uri>, destUri: Uri) {
        Toast.makeText(this, "Converting…", Toast.LENGTH_SHORT).show()
        activityScope.launch(Dispatchers.IO) {
            val pdf = PdfDocument()
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            try {
                imageUris.forEachIndexed { idx, uri ->
                    val bmp = decodeSampledBitmap(uri, A4_WIDTH_PT * 2, A4_HEIGHT_PT * 2)
                        ?: return@forEachIndexed
                    try {
                        val (pw, ph) = fitInsideA4(bmp.width, bmp.height)
                        val page = pdf.startPage(
                            PdfDocument.PageInfo.Builder(pw, ph, idx + 1).create())
                        page.canvas.drawColor(android.graphics.Color.WHITE)
                        page.canvas.drawBitmap(bmp, null,
                            RectF(0f, 0f, pw.toFloat(), ph.toFloat()), paint)
                        pdf.finishPage(page)
                    } finally { bmp.recycle() }
                }
                contentResolver.openOutputStream(destUri)?.use { pdf.writeTo(it) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "PDF Saved Successfully!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Conversion failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Conversion failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally { pdf.close() }
        }
    }

    private fun decodeSampledBitmap(uri: Uri, maxW: Int, maxH: Int): Bitmap? = runCatching {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var s = 1
        while (opts.outWidth / s > maxW || opts.outHeight / s > maxH) s *= 2
        val o2 = BitmapFactory.Options().apply { inSampleSize = s }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
    }.getOrNull()

    private fun fitInsideA4(w: Int, h: Int): Pair<Int, Int> {
        val scale = minOf(A4_WIDTH_PT.toFloat() / w, A4_HEIGHT_PT.toFloat() / h, 1f)
        return Pair((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isPdfVisible) {
            isPdfVisible = false
            currentPdfUri = null
        } else {
            super.onBackPressed()
        }
    }
}