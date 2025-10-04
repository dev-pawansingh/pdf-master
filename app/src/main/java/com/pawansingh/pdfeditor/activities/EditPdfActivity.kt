package com.pawansingh.pdfeditor.activities

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.pawansingh.pdfeditor.R
import com.pawansingh.pdfeditor.adapters.PdfPageAdapter
import com.pawansingh.pdfeditor.views.PdfPageWithOverlayView

class EditPdfActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var textInputCard: com.google.android.material.card.MaterialCardView
    private lateinit var textInputEditText: EditText

    private var currentPdfUri: Uri? = null
    private val pdfBitmaps = mutableListOf<Bitmap>()
    private var pdfLoaded = false
    private var currentMode = PdfPageWithOverlayView.Mode.HAND
    private var currentPageIndex = 0
    private var currentTouchX = 0f
    private var currentTouchY = 0f
    private lateinit var adapter: PdfPageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_pdf)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.pdfRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        textInputCard = findViewById(R.id.textInputCard)
        textInputEditText = findViewById(R.id.textInputEditText)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        toolbar.setNavigationOnClickListener { finish() }

        // Setup tool buttons
        setupToolButtons()
        setupTextInputDialog()

        // Load PDF from intent
        val pdfUri = intent.getParcelableExtra<Uri>("pdfUri")
        if (pdfUri != null) {
            currentPdfUri = pdfUri
            loadPdfPages(pdfUri)
        } else {
            Toast.makeText(this, "No PDF selected", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupToolButtons() {
        // Hand mode for scrolling/selection
        findViewById<ImageButton>(R.id.btnHighlight).setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.HAND)
            Toast.makeText(this, "Hand mode - Scroll and select", Toast.LENGTH_SHORT).show()
        }

        // Text mode
        findViewById<ImageButton>(R.id.btnText).setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.TEXT)
            Toast.makeText(this, "Text mode - Tap to add text", Toast.LENGTH_SHORT).show()
        }

        // Draw mode
        findViewById<ImageButton>(R.id.btnDraw).setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.DRAW)
            Toast.makeText(this, "Draw mode", Toast.LENGTH_SHORT).show()
        }

        // Eraser mode
        findViewById<ImageButton>(R.id.btnEraser).setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.ERASER)
            Toast.makeText(this, "Eraser mode", Toast.LENGTH_SHORT).show()
        }

        // Move text mode
        findViewById<ImageButton>(R.id.btnSave).setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.MOVE_TEXT)
            Toast.makeText(this, "Move text mode - Drag text to reposition", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setMode(mode: PdfPageWithOverlayView.Mode) {
        currentMode = mode
        adapter.setModeForAllPages(mode)
    }

    private fun setupTextInputDialog() {
        findViewById<Button>(R.id.btnCancelText).setOnClickListener {
            textInputCard.visibility = View.GONE
        }

        findViewById<Button>(R.id.btnConfirmText).setOnClickListener {
            val text = textInputEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                // Add text to the current page at the touch position
                adapter.addTextToPage(currentPageIndex, text, currentTouchX, currentTouchY)
                Toast.makeText(this, "Text added to page ${currentPageIndex + 1}", Toast.LENGTH_SHORT).show()
            }
            textInputCard.visibility = View.GONE
            textInputEditText.text.clear()
        }
    }

    private fun onAddTextRequest(pageIndex: Int, x: Float, y: Float) {
        currentPageIndex = pageIndex
        currentTouchX = x
        currentTouchY = y
        showTextInputDialog()
    }

    private fun onTextAdded(pageIndex: Int, text: String, x: Float, y: Float) {
        // Text was added to the overlay view
        // You can save this information if needed
    }

    private fun showTextInputDialog() {
        textInputEditText.text.clear()
        textInputCard.visibility = View.VISIBLE
        textInputEditText.requestFocus()

        // Show keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(textInputEditText, 0)
    }

    private fun loadPdfPages(uri: Uri) {
        progressBar.visibility = ProgressBar.VISIBLE

        try {
            // Clear previous bitmaps
            pdfBitmaps.forEach { it.recycle() }
            pdfBitmaps.clear()

            val inputParcel = contentResolver.openFileDescriptor(uri, "r")
            inputParcel?.let { pfd ->
                val renderer = PdfRenderer(pfd)

                // Render all pages to bitmaps
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(
                        page.width,
                        page.height,
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfBitmaps.add(bitmap)
                    page.close()
                }
                renderer.close()
                pfd.close()

                // Setup RecyclerView with pages
                adapter = PdfPageAdapter(
                    pdfBitmaps,
                    onAddTextRequest = { pageIndex, x, y -> onAddTextRequest(pageIndex, x, y) },
                    onTextAdded = { pageIndex, text, x, y -> onTextAdded(pageIndex, text, x, y) }
                )
                recyclerView.adapter = adapter

                pdfLoaded = true
                Toast.makeText(this, "PDF loaded: ${pdfBitmaps.size} pages", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            pdfLoaded = false
        } finally {
            progressBar.visibility = ProgressBar.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up bitmaps to prevent memory leaks
        pdfBitmaps.forEach { it.recycle() }
        pdfBitmaps.clear()
    }
}