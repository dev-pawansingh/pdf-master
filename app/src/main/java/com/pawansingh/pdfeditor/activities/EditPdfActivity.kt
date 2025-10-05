package com.pawansingh.pdfeditor.activities

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.pawansingh.pdfeditor.R
import com.pawansingh.pdfeditor.adapters.PdfPageAdapter
import com.pawansingh.pdfeditor.databinding.ActivityEditPdfBinding
import com.pawansingh.pdfeditor.views.PdfPageWithOverlayView

class EditPdfActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPdfBinding
    private val pdfBitmaps = mutableListOf<Bitmap>()
    private lateinit var adapter: PdfPageAdapter
    private var currentMode = PdfPageWithOverlayView.Mode.HAND
    private var selectedTextAnnotation: PdfPageWithOverlayView.TextAnnotation? = null
    private var isBoldActive = false
    private var isItalicActive = false

    // Add the missing variables
    private var currentPageIndex = 0
    private var currentTouchX = 0f
    private var currentTouchY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeViews()
        setupToolButtons()
        setupTextOptions()
        setupTextInputDialog()

        val pdfUri = intent.getParcelableExtra<Uri>("pdfUri")
        if (pdfUri != null) {
            loadPdfPages(pdfUri)
        } else {
            Toast.makeText(this, "No PDF selected", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        binding.pdfRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.pdfRecyclerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                selectedTextAnnotation?.let { textAnn ->
                    val overlay = adapter.getOverlayView(currentPageIndex)
                    overlay?.let { ov ->
                        // Convert RecyclerView touch to overlay coordinates
                        val rvLocation = IntArray(2)
                        overlay.getLocationOnScreen(rvLocation)

                        val touchXOnOverlay = event.rawX - rvLocation[0]
                        val touchYOnOverlay = event.rawY - rvLocation[1]

                        val textRect = ov.getTextRect(textAnn)
                        if (!textRect.contains(touchXOnOverlay, touchYOnOverlay)) {
                            clearTextSelection()
                        }
                    }
                }
            }
            false
        }

    }

    private fun setupToolButtons() {
        updateToolSelection(binding.btnHand)

        binding.btnHand.setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.HAND)
            updateToolSelection(binding.btnHand)
            hideTextOptions()
            clearTextSelection()
        }

        binding.btnText.setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.TEXT)
            updateToolSelection(binding.btnText)
            showTextOptions()
        }

        binding.btnDraw.setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.DRAW)
            updateToolSelection(binding.btnDraw)
            hideTextOptions()
            clearTextSelection()
        }

        binding.btnHighlight.setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.HIGHLIGHT)
            updateToolSelection(binding.btnHighlight)
            hideTextOptions()
            clearTextSelection()
        }

        binding.btnEraser.setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.ERASER)
            updateToolSelection(binding.btnEraser)
            hideTextOptions()
            clearTextSelection()
        }

        binding.btnSave.setOnClickListener {
            Toast.makeText(this, "Save functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateToolSelection(selectedButton: android.widget.ImageButton) {
        val buttons = listOf(
            binding.btnHand, binding.btnText, binding.btnDraw,
            binding.btnHighlight, binding.btnEraser
        )

        buttons.forEach { button ->
            if (button == selectedButton) {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
                button.setColorFilter(ContextCompat.getColor(this, R.color.pdf_red))
            } else {
                button.setBackgroundColor(Color.TRANSPARENT)
                button.setColorFilter(ContextCompat.getColor(this, R.color.primary))
            }
        }
    }

    private fun setupTextOptions() {
        // Font Spinner with default value display
        val fonts = arrayOf("Default", "Serif", "Sans Serif", "Monospace")
        val fontAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fonts) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = "Font: ${fonts[position]}"
                return view
            }
        }
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.fontSpinner.adapter = fontAdapter
        binding.fontSpinner.setSelection(0)

        binding.fontSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val fontFamily = when (position) {
                    1 -> Typeface.SERIF
                    2 -> Typeface.SANS_SERIF
                    3 -> Typeface.MONOSPACE
                    else -> Typeface.DEFAULT
                }
                selectedTextAnnotation?.let {
                    adapter.getOverlayView(currentPageIndex)?.setFontFamily(fontFamily)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Text Size Spinner with default value display
        val textSizes = arrayOf("12", "14", "16", "18", "20", "24", "28", "32", "36", "40")
        val sizeAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, textSizes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = "Size: ${textSizes[position]}"
                return view
            }
        }
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.textSizeSpinner.adapter = sizeAdapter
        binding.textSizeSpinner.setSelection(4) // Default to 20

        binding.textSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val size = textSizes[position].toFloat()
                selectedTextAnnotation?.let {
                    adapter.getOverlayView(currentPageIndex)?.setTextSize(size)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Color Spinner with default value display
        val colors = arrayOf("Black", "Red", "Blue", "Green")
        val colorAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, colors) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = "Color: ${colors[position]}"
                textView.setTextColor(getColorForPosition(position))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = colors[position]
                textView.setTextColor(getColorForPosition(position))
                return view
            }

            private fun getColorForPosition(position: Int): Int {
                return when (position) {
                    0 -> Color.BLACK
                    1 -> Color.RED
                    2 -> Color.BLUE
                    3 -> Color.GREEN
                    else -> Color.BLACK
                }
            }
        }
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.colorSpinner.adapter = colorAdapter
        binding.colorSpinner.setSelection(0)

        binding.colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val color = when (position) {
                    0 -> Color.BLACK
                    1 -> Color.RED
                    2 -> Color.BLUE
                    3 -> Color.GREEN
                    else -> Color.BLACK
                }
                selectedTextAnnotation?.let {
                    adapter.getOverlayView(currentPageIndex)?.setTextColor(color)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Style Buttons
        binding.btnBold.setOnClickListener {
            isBoldActive = !isBoldActive
            updateStyleButtons()
            selectedTextAnnotation?.let {
                adapter.getOverlayView(currentPageIndex)?.setTextBold(isBoldActive)
            }
        }

        binding.btnItalic.setOnClickListener {
            isItalicActive = !isItalicActive
            updateStyleButtons()
            selectedTextAnnotation?.let {
                adapter.getOverlayView(currentPageIndex)?.setTextItalic(isItalicActive)
            }
        }
    }

    private fun updateStyleButtons() {
        binding.btnBold.setBackgroundColor(if (isBoldActive) ContextCompat.getColor(this, R.color.primary) else Color.TRANSPARENT)
        binding.btnItalic.setBackgroundColor(if (isItalicActive) ContextCompat.getColor(this, R.color.primary) else Color.TRANSPARENT)
    }

    private fun showTextOptions() {
        binding.textOptionsPanel.visibility = View.VISIBLE
    }

    private fun hideTextOptions() {
        binding.textOptionsPanel.visibility = View.GONE
    }

    private fun setMode(mode: PdfPageWithOverlayView.Mode) {
        currentMode = mode
        adapter.setModeForAllPages(mode)
        updateTextOptionsVisibility()
    }

    private fun updateTextOptionsVisibility() {
        if (currentMode == PdfPageWithOverlayView.Mode.TEXT) {
            showTextOptions()
        } else {
            hideTextOptions()
        }
    }

    private fun setupTextInputDialog() {
        binding.btnCancelText.setOnClickListener {
            binding.textInputCard.visibility = View.GONE
        }

        binding.btnConfirmText.setOnClickListener {
            val text = binding.textInputEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                adapter.addTextToPage(currentPageIndex, text, currentTouchX, currentTouchY)
                binding.textInputCard.visibility = View.GONE
                binding.textInputEditText.text.clear()
                adapter.setModeForAllPages(PdfPageWithOverlayView.Mode.HAND)
                updateToolSelection(binding.btnHand)
            } else {
                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onAddTextRequest(pageIndex: Int, x: Float, y: Float) {
        currentPageIndex = pageIndex
        currentTouchX = x
        currentTouchY = y
        showTextInputDialog()
    }

    private fun onTextSelected(textAnnotation: PdfPageWithOverlayView.TextAnnotation?) {
        selectedTextAnnotation = textAnnotation
    }

    private fun clearTextSelection() {
        selectedTextAnnotation = null
        adapter.getOverlayView(currentPageIndex)?.clearSelection()
    }

    private fun showTextInputDialog() {
        binding.textInputEditText.text.clear()
        binding.textInputCard.visibility = View.VISIBLE
        binding.textInputEditText.requestFocus()
    }

    private fun loadPdfPages(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE

        try {
            pdfBitmaps.forEach { it.recycle() }
            pdfBitmaps.clear()

            val inputParcel = contentResolver.openFileDescriptor(uri, "r")
            inputParcel?.let { pfd ->
                val renderer = PdfRenderer(pfd)

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfBitmaps.add(bitmap)
                    page.close()
                }
                renderer.close()
                pfd.close()

                adapter = PdfPageAdapter(
                    pdfBitmaps,
                    onAddTextRequest = { pageIndex, x, y -> onAddTextRequest(pageIndex, x, y) },
                    onTextAdded = { pageIndex, text, x, y -> },
                    onTextSelected = { textAnnotation -> onTextSelected(textAnnotation) }
                )
                binding.pdfRecyclerView.adapter = adapter

                Toast.makeText(this, "PDF loaded: ${pdfBitmaps.size} pages", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfBitmaps.forEach { if (!it.isRecycled) it.recycle() }
        pdfBitmaps.clear()
    }
}

// Extension functions
fun Int.isBold(): Boolean = this and Typeface.BOLD != 0
fun Int.isItalic(): Boolean = this and Typeface.ITALIC != 0