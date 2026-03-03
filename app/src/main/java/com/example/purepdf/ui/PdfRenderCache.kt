package com.example.purepdf.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.util.LruCache
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Mutex-protected, sequential PDF page rendering with an 8-page LruCache.
 *
 * - ALL rendering goes through a single [Mutex] so only one page renders at a time.
 * - Render width is capped at [MAX_RENDER_WIDTH] for performance.
 */
class PdfRenderCache(
    private val renderer: PdfRenderer,
    screenWidth: Int
) {
    companion object {
        const val MAX_RENDER_WIDTH = 900
    }

    val pageCount: Int = renderer.pageCount
    private val renderWidth = screenWidth.coerceAtMost(MAX_RENDER_WIDTH)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pdfMutex = Mutex()

    private val cache: LruCache<Int, Bitmap> = object : LruCache<Int, Bitmap>(8) {
        override fun entryRemoved(evicted: Boolean, key: Int, old: Bitmap, new: Bitmap?) {
            if (evicted && new == null) old.recycle()
        }
    }

    fun getCachedPage(index: Int): Bitmap? = cache.get(index)

    /**
     * Render [pageIndex] sequentially (Mutex-locked) on IO.
     * Calls [onReady] on the main thread when done.
     */
    fun requestPage(pageIndex: Int, onReady: (Int) -> Unit) {
        // Already cached — notify immediately
        if (cache.get(pageIndex) != null) {
            scope.launch(Dispatchers.Main) { onReady(pageIndex) }
            return
        }
        scope.launch {
            val bmp = pdfMutex.withLock {
                try {
                    val page = renderer.openPage(pageIndex)
                    try {
                        val ratio = page.height.toFloat() / page.width.toFloat()
                        val h = (ratio * renderWidth).toInt()
                        val bmp = Bitmap.createBitmap(renderWidth, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    } finally { page.close() }
                } catch (_: Exception) { null }
            }
            bmp?.let { cache.put(pageIndex, it) }
            if (bmp != null) withContext(Dispatchers.Main) { onReady(pageIndex) }
        }
    }

    fun close() {
        scope.cancel()
        cache.evictAll()
    }
}
