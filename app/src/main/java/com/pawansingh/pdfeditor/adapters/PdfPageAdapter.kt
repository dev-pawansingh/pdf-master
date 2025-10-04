package com.pawansingh.pdfeditor.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pawansingh.pdfeditor.R
import com.pawansingh.pdfeditor.views.PdfPageWithOverlayView

class PdfPageAdapter(
    private val pages: List<Bitmap>,
    private val onAddTextRequest: (Int, Float, Float) -> Unit,
    private val onTextAdded: (Int, String, Float, Float) -> Unit
) : RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {

    private val overlayViews = mutableMapOf<Int, PdfPageWithOverlayView>()
    private var currentMode = PdfPageWithOverlayView.Mode.HAND

    inner class PdfPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pageImage: ImageView = itemView.findViewById(R.id.pdfPageImage)
        val pageNumber: TextView = itemView.findViewById(R.id.pageNumber)
        val overlayView: PdfPageWithOverlayView = itemView.findViewById(R.id.overlayView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return PdfPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        val bitmap = pages[position]

        // Set the PDF page bitmap to ImageView
        holder.pageImage.setImageBitmap(bitmap)
        holder.pageNumber.text = "Page ${position + 1}"

        // Store overlay view reference
        overlayViews[position] = holder.overlayView

        // Set current mode
        holder.overlayView.setMode(currentMode)

        // Setup text request callback using setter method instead of property
        holder.overlayView.setOnAddTextRequestListener { x, y ->
            onAddTextRequest(position, x, y)
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

// Extension function to set the listener
fun PdfPageWithOverlayView.setOnAddTextRequestListener(listener: (Float, Float) -> Unit) {
    this.onAddTextRequest = listener
}