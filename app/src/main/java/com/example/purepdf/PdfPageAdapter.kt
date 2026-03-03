package com.example.purepdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

/**
 * Renders each page of a [PdfRenderer] on demand and displays it in an [ImageView].
 *
 * Pages are rendered at full display width to keep things sharp. The [PdfRenderer]
 * is NOT owned here – the caller (MainActivity) is responsible for closing it.
 *
 * @param renderer  An open [PdfRenderer] for the current PDF.
 * @param pageWidth The width (px) to render each page at – typically the screen/RecyclerView width.
 */
class PdfPageAdapter(
    private val renderer: PdfRenderer,
    private val pageWidth: Int
) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPage)
    }

    override fun getItemCount(): Int = renderer.pageCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        // openPage must be balanced by a close() call – always use try/finally.
        val page = renderer.openPage(position)
        try {
            // Scale height proportionally to preserve the page's aspect ratio.
            val pageHeight = (page.height.toFloat() / page.width.toFloat() * pageWidth).toInt()
            val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
            // Fill with white first – PdfRenderer renders onto whatever is in the buffer,
            // which can be transparent/dark in dark mode, producing a black page.
            bitmap.eraseColor(Color.WHITE)

            // RENDER_MODE_FOR_DISPLAY applies anti-aliasing optimised for screens.
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            holder.imageView.setImageBitmap(bitmap)
        } finally {
            page.close()
        }
    }

    /**
     * Recycle the bitmap held by a view that is leaving the screen to reduce memory pressure.
     */
    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        holder.imageView.setImageBitmap(null)
    }
}
