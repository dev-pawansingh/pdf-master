package com.pawansingh.pdfeditor.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pawansingh.pdfeditor.R
import com.pawansingh.pdfeditor.views.PdfPageWithOverlayView

class PdfPageAdapter(
    private val pages: List<Bitmap>,
    private val onAddTextRequest: (Int, Float, Float) -> Unit,
    private val onTextAdded: (Int, String, Float, Float) -> Unit,
    private val onTextSelected: (PdfPageWithOverlayView.TextAnnotation?) -> Unit
) : RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {

    private val overlayViews = mutableMapOf<Int, PdfPageWithOverlayView>()
    private var currentMode = PdfPageWithOverlayView.Mode.HAND

    inner class PdfPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pageImage: android.widget.ImageView = itemView.findViewById(R.id.pdfPageImage)
        val pageNumber: android.widget.TextView = itemView.findViewById(R.id.pageNumber)
        val overlayView: PdfPageWithOverlayView = itemView.findViewById(R.id.overlayView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pdf_page, parent, false)
        return PdfPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        if (position < 0 || position >= pages.size) return

        val bitmap = pages[position]
        holder.pageImage.setImageBitmap(bitmap)
        holder.pageNumber.text = "Page ${position + 1}"

        overlayViews[position] = holder.overlayView
        holder.overlayView.setMode(currentMode)

        holder.overlayView.onAddTextRequest = { x, y ->
            onAddTextRequest(position, x, y)
        }

        holder.overlayView.onTextSelected = { textAnnotation ->
            onTextSelected(textAnnotation)
        }

        holder.overlayView.onTouchHandled = { isHandled ->
            holder.itemView.parent?.requestDisallowInterceptTouchEvent(isHandled)
        }
    }

    override fun getItemCount(): Int = pages.size

    fun setModeForAllPages(mode: PdfPageWithOverlayView.Mode) {
        currentMode = mode
        overlayViews.values.forEach { it.setMode(mode) }
    }

    fun addTextToPage(pageIndex: Int, text: String, x: Float, y: Float) {
        overlayViews[pageIndex]?.addTextAnnotation(text, x, y)
        onTextAdded(pageIndex, text, x, y)
    }

    fun getOverlayView(pageIndex: Int): PdfPageWithOverlayView? {
        return overlayViews[pageIndex]
    }
}