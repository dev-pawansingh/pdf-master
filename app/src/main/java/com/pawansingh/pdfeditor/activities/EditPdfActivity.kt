package com.pawansingh.pdfeditor.activities

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

    private var currentPenColor = Color.RED
    private var currentPenSize = 5f

    private var currentHighlightColor = Color.YELLOW
    private var currentHighlightSize = 15f

    private var currentEraserSize = 20f

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
        setupDrawOptions()
        setupHighlightOptions()
        updateColorCircle()
        updateHighlightColorCircle()
        setupEraserOptions()

        val pdfUri = intent.getParcelableExtra<Uri>("pdfUri")
        if (pdfUri != null) {
            loadPdfPages(pdfUri)
        } else {
            Toast.makeText(this, "No PDF selected", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////

    private fun setupEraserOptions() {
        // Eraser Size SeekBar
        binding.eraserSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentEraserSize = progress.toFloat()
                updateCurrentEraserSettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Eraser Undo/Redo
        binding.eraserUndoLayout.setOnClickListener { undoLastEraser() }
        binding.eraserRedoLayout.setOnClickListener { redoLastEraser() }
    }

    private fun updateCurrentEraserSettings() {
        adapter.setEraserSettings(currentEraserSize)
    }

    private fun undoLastEraser() {
        adapter.undoEraser(currentPageIndex)
    }

    private fun redoLastEraser() {
        adapter.redoEraser(currentPageIndex)
    }

    private fun showEraserOptions() {
        binding.eraserOptionsPanel.visibility = View.VISIBLE
        binding.textOptionsPanel.visibility = View.GONE
        binding.drawOptionsPanel.visibility = View.GONE
        binding.highlightOptionsPanel.visibility = View.GONE
    }

    private fun hideEraserOptions() {
        binding.eraserOptionsPanel.visibility = View.GONE
    }


    ////////////////////////////////////////////////////////////////////////////////////

    // Ye do functions ADD KARO

    private fun updateHighlightColorCircle() {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(currentHighlightColor)
            setStroke(3.dpToPx(), Color.DKGRAY)
        }
        binding.highlightColorCircle.background = drawable
    }

    private fun showHighlightOptions() {
        binding.highlightOptionsPanel.visibility = View.VISIBLE
        binding.textOptionsPanel.visibility = View.GONE
        binding.drawOptionsPanel.visibility = View.GONE
    }

    private fun hideHighlightOptions() {
        binding.highlightOptionsPanel.visibility = View.GONE
    }

    private fun setupHighlightOptions() {
        // Highlight Color Circle
        binding.highlightColorCircle.setOnClickListener {
            showHighlightColorPickerDialog()
        }

        // Highlight Size SeekBar
        binding.highlightSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentHighlightSize = progress.toFloat()
                updateCurrentHighlightSettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Highlight Undo/Redo
        binding.highlightUndoLayout.setOnClickListener { undoLastHighlight() }
        binding.highlightRedoLayout.setOnClickListener { redoLastHighlight() }
        updateHighlightColorCircle()
    }

    private fun showHighlightColorPickerDialog() {
        val colors = listOf(
            Pair("Yellow", Color.YELLOW),
            Pair("Pink", Color.MAGENTA),
            Pair("Cyan", Color.CYAN),
            Pair("Green", Color.GREEN),
            Pair("Orange", Color.rgb(255, 165, 0)),
            Pair("Blue", Color.BLUE),
            Pair("Red", Color.RED),
            Pair("Gray", Color.GRAY)
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Highlight Color")

        // Main container with fixed width
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        }

        val gridLayout = GridLayout(this).apply {
            columnCount = 4
            rowCount = 2
        }

        var colorDialog: Dialog? = null

        for ((colorName, colorValue) in colors) {
            val colorContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 70.dpToPx()
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(2.dpToPx(), 2.dpToPx(), 2.dpToPx(), 2.dpToPx())
                }
            }

            val colorView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(36.dpToPx(), 36.dpToPx())

                // ✅ PROGRAMMATICALLY DRAWABLE BANAO
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(colorValue)
                drawable.setStroke(3.dpToPx(), Color.DKGRAY)

                background = drawable

                setOnClickListener {
                    currentHighlightColor = colorValue
                    updateHighlightColorCircle()
                    updateCurrentHighlightSettings()
                    colorDialog?.dismiss()
                }
            }

            val colorNameView = TextView(this).apply {
                text = colorName
                textSize = 10f
                setTextColor(Color.BLACK)
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 2.dpToPx()
                }
            }

            colorContainer.addView(colorView)
            colorContainer.addView(colorNameView)
            gridLayout.addView(colorContainer)
        }

        mainContainer.addView(gridLayout)
        builder.setView(mainContainer)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        colorDialog = builder.create()
        colorDialog.window?.setLayout(280.dpToPx(), ViewGroup.LayoutParams.WRAP_CONTENT)
        colorDialog.show()
    }

    private fun updateCurrentHighlightSettings() {
        adapter.setHighlightSettings(currentHighlightColor, currentHighlightSize)
    }

    private fun undoLastHighlight() {
        adapter.undoHighlight(currentPageIndex)
    }

    private fun redoLastHighlight() {
        adapter.redoHighlight(currentPageIndex)
    }

    ////////////////////////////////////////////////////////////////////////////////////

    private fun setupDrawOptions() {
        // YEH POORA FUNCTION ADD KARO
        // Color Circle Click Listener
        binding.colorCircle.setOnClickListener {
            showColorPickerDialog()
        }

        // Pen Size SeekBar
        binding.penSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentPenSize = progress.toFloat()
                updateCurrentPenSettings()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Undo Button with Text
        binding.undoLayout.setOnClickListener {
            undoLastDrawing()
        }

        // Redo Button with Text
        binding.redoLayout.setOnClickListener {
            redoLastDrawing()
        }
    }

    // YEH SAB FUNCTIONS ADD KARO
    private fun showColorPickerDialog() {
        val colors = listOf(
            Pair("Black", Color.BLACK),
            Pair("Red", Color.RED),
            Pair("White", Color.WHITE),
            Pair("Green", Color.GREEN),
            Pair("Blue", Color.BLUE),
            Pair("Yellow", Color.YELLOW),
            Pair("Pink", Color.MAGENTA),
            Pair("Gray", Color.GRAY)
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Pen Color")

        // Main container with fixed width
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        }

        val gridLayout = GridLayout(this).apply {
            columnCount = 4
            rowCount = 2
        }

        // ✅ VARIABLE BANAO DIALOG KE LIYE
        var colorDialog: Dialog? = null

        for ((colorName, colorValue) in colors) {
            val colorContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 70.dpToPx()  // ✅ WIDTH THODI KAM KARO
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(2.dpToPx(), 2.dpToPx(), 2.dpToPx(), 2.dpToPx()) // ✅ MARGIN BAHUT KAM KARO
                }
            }

            val colorView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(36.dpToPx(), 36.dpToPx())

                // ✅ PROGRAMMATICALLY DRAWABLE BANAO
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(colorValue) // ✅ ACTUAL COLOR SET KARO
                drawable.setStroke(3.dpToPx(), Color.DKGRAY) // ✅ BORDER ADD KARO

                background = drawable

                setOnClickListener {
                    currentPenColor = colorValue
                    updateColorCircle()
                    updateCurrentPenSettings()
                    colorDialog?.dismiss()
                }
            }

            val colorNameView = TextView(this).apply {
                text = colorName
                textSize = 10f
                setTextColor(Color.BLACK)
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 2.dpToPx() // ✅ MARGIN KAM KARO
                }
            }

            colorContainer.addView(colorView)
            colorContainer.addView(colorNameView)
            gridLayout.addView(colorContainer)
        }

        mainContainer.addView(gridLayout)
        builder.setView(mainContainer)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        colorDialog = builder.create() // ✅ DIALOG KO VARIABLE MEIN STORE KARO

        // Dialog ki width set karo (thodi kam karo)
        colorDialog.window?.setLayout(280.dpToPx(), ViewGroup.LayoutParams.WRAP_CONTENT) // ✅ 320 se 280 KARO

        colorDialog.show()
    }

    private fun updateColorCircle() {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(currentPenColor) // Selected color
        drawable.setStroke(2.dpToPx(), Color.DKGRAY) // Border

        binding.colorCircle.background = drawable
    }

    private fun updateCurrentPenSettings() {
        adapter.setPenSettings(currentPenColor, currentPenSize)
    }

    private fun undoLastDrawing() {
        adapter.undoDrawing(currentPageIndex)
    }

    private fun redoLastDrawing() {
        adapter.redoDrawing(currentPageIndex)
    }

    // Extension function
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun showDrawOptions() {
        binding.drawOptionsPanel.visibility = View.VISIBLE
    }

    private fun hideDrawOptions() {
        binding.drawOptionsPanel.visibility = View.GONE
    }

///////////////////////////////////////////////////////////////////////////////////////////////

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
            hideDrawOptions()
            hideHighlightOptions()
            clearTextSelection()
        }

        binding.btnText.setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.TEXT)
            updateToolSelection(binding.btnText)
            showTextOptions()
            hideHighlightOptions()
            hideDrawOptions()
        }

        binding.btnDraw.setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.DRAW)
            updateToolSelection(binding.btnDraw)
            showDrawOptions()
            hideTextOptions()
            hideHighlightOptions()
            clearTextSelection()
        }

        binding.btnHighlight.setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.HIGHLIGHT)
            updateToolSelection(binding.btnHighlight)
            hideTextOptions()
            hideDrawOptions()
            showHighlightOptions()
            clearTextSelection()
        }

        binding.btnEraser.setOnClickListener {
            setMode(PdfPageWithOverlayView.Mode.ERASER)
            updateToolSelection(binding.btnEraser)
            hideTextOptions()
            showEraserOptions()
            hideDrawOptions()
            hideHighlightOptions()
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
        updateToolOptionsVisibility()
    }

    private fun updateToolOptionsVisibility() {
        when (currentMode) {
            PdfPageWithOverlayView.Mode.TEXT -> {
                showTextOptions()
                hideDrawOptions()
                hideHighlightOptions()
                hideEraserOptions()
                binding.pdfRecyclerView.layoutParams = (binding.pdfRecyclerView.layoutParams as RelativeLayout.LayoutParams).apply {
                    addRule(RelativeLayout.BELOW, R.id.textOptionsPanel)
                }
            }
            PdfPageWithOverlayView.Mode.DRAW -> {
                showDrawOptions()
                hideTextOptions()
                hideHighlightOptions()
                hideEraserOptions()
                binding.pdfRecyclerView.layoutParams = (binding.pdfRecyclerView.layoutParams as RelativeLayout.LayoutParams).apply {
                    addRule(RelativeLayout.BELOW, R.id.drawOptionsPanel)
                }
            }
            PdfPageWithOverlayView.Mode.HIGHLIGHT -> {
                showHighlightOptions()
                hideTextOptions()
                hideDrawOptions()
                hideEraserOptions()
                binding.pdfRecyclerView.layoutParams = (binding.pdfRecyclerView.layoutParams as RelativeLayout.LayoutParams).apply {
                    addRule(RelativeLayout.BELOW, R.id.highlightOptionsPanel)
                }
            }
            PdfPageWithOverlayView.Mode.ERASER -> { // ✅ YEH NAYA CASE ADD KARO
                showEraserOptions()
                hideTextOptions()
                hideDrawOptions()
                hideHighlightOptions()
                binding.pdfRecyclerView.layoutParams = (binding.pdfRecyclerView.layoutParams as RelativeLayout.LayoutParams).apply {
                    addRule(RelativeLayout.BELOW, R.id.eraserOptionsPanel)
                }
            }
            else -> {
                hideTextOptions()
                hideDrawOptions()
                hideHighlightOptions()
                hideEraserOptions()
                binding.pdfRecyclerView.layoutParams = (binding.pdfRecyclerView.layoutParams as RelativeLayout.LayoutParams).apply {
                    addRule(RelativeLayout.BELOW, R.id.toolsStrip)
                }
            }
        }
        binding.pdfRecyclerView.requestLayout()
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

                //////
                updateCurrentPenSettings()
                updateCurrentHighlightSettings()
                updateCurrentEraserSettings()
                //////

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