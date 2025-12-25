package com.pawansingh.pdfeditor.activities

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.pawansingh.pdfeditor.PdfAction
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
    private var currentFontName = "Default"
    private var currentTextSize: Float = 20F
    private val textSizes = arrayOf("12", "14", "16", "18", "20", "24", "28", "32", "36", "40")

    private var currentTextColor = Color.BLACK

    private val globalUndoStack = mutableListOf<PdfAction>()
    private val globalRedoStack = mutableListOf<PdfAction>()

    private var currentPenColor = Color.RED
    private var currentPenSize = 5f

    private var currentHighlightColor = Color.YELLOW
    private var currentHighlightSize = 15f

    private var currentEraserSize = 20f

    private var currentPageIndex = 0
    private var currentTouchX = 0f
    private var currentTouchY = 0f

    private var pendingFileName: String = "Edited_PDF"

    private val createPdfLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            if (uri != null) {
                savePdfToUri(uri)
            }
        }

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
        updateTextColorCircle()
        setupEraserOptions()

        val pdfUri = intent.getParcelableExtra<Uri>("pdfUri")
        if (pdfUri != null) {
            loadPdfPages(pdfUri)
        } else {
            Toast.makeText(this, "No PDF selected", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupEraserOptions() {
        binding.eraserSizeSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentEraserSize = progress.toFloat()
                updateCurrentEraserSettings()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateCurrentEraserSettings() {
        adapter.setEraserSettings(currentEraserSize)
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
        binding.highlightColorCircle.setOnClickListener {
            showCommonColorPicker("Select Highlight Color") { color ->
                currentHighlightColor = color
                updateHighlightColorCircle()
                updateCurrentHighlightSettings()
            }
        }

        binding.highlightSizeSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentHighlightSize = progress.toFloat()
                updateCurrentHighlightSettings()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        updateHighlightColorCircle()
    }

    private fun updateCurrentHighlightSettings() {
        adapter.setHighlightSettings(currentHighlightColor, currentHighlightSize)
    }

    private fun setupDrawOptions() {
        binding.colorCircle.setOnClickListener {
            showCommonColorPicker("Select Pen Color"){ color ->
                currentPenColor = color
                updateColorCircle()
                updateCurrentPenSettings()
            }
        }

        binding.penSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentPenSize = progress.toFloat()
                updateCurrentPenSettings()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun showDrawOptions() {
        binding.drawOptionsPanel.visibility = View.VISIBLE
    }

    private fun hideDrawOptions() {
        binding.drawOptionsPanel.visibility = View.GONE
    }

    private fun initializeViews() {
        binding.pdfRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.pdfRecyclerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                selectedTextAnnotation?.let { textAnn ->
                    val overlay = adapter.getOverlayView(currentPageIndex)
                    overlay?.let { ov ->
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
            it.pressAnim()
            setMode(PdfPageWithOverlayView.Mode.HAND)
            updateToolSelection(binding.btnHand)
            hideTextOptions()
            hideDrawOptions()
            hideHighlightOptions()
            clearTextSelection()
        }

        binding.btnText.setOnClickListener {
            it.pressAnim()
            setMode(PdfPageWithOverlayView.Mode.TEXT)
            updateToolSelection(binding.btnText)
            showTextOptions()
            hideHighlightOptions()
            hideDrawOptions()
        }

        binding.btnDraw.setOnClickListener {
            it.pressAnim()
            setMode(PdfPageWithOverlayView.Mode.DRAW)
            updateToolSelection(binding.btnDraw)
            showDrawOptions()
            hideTextOptions()
            hideHighlightOptions()
            clearTextSelection()
        }

        binding.btnHighlight.setOnClickListener {
            it.pressAnim()
            setMode(PdfPageWithOverlayView.Mode.HIGHLIGHT)
            updateToolSelection(binding.btnHighlight)
            hideTextOptions()
            hideDrawOptions()
            showHighlightOptions()
            clearTextSelection()
        }

        binding.btnEraser.setOnClickListener {
            it.pressAnim()
            setMode(PdfPageWithOverlayView.Mode.ERASER)
            updateToolSelection(binding.btnEraser)
            hideTextOptions()
            showEraserOptions()
            hideDrawOptions()
            hideHighlightOptions()
            clearTextSelection()
        }

        binding.btnUndo.setOnClickListener {
            it.pressAnim()
            if (globalUndoStack.isEmpty()) return@setOnClickListener
            val action = globalUndoStack.removeAt(globalUndoStack.size - 1)
            globalRedoStack.add(action)
            when (action) {
                is PdfAction.Draw -> adapter.undoDrawing(action.page)
                is PdfAction.Highlight -> adapter.undoHighlight(action.page)
                is PdfAction.Erase -> adapter.undoEraser(action.page)
                is PdfAction.Text -> {}
            }
        }

        binding.btnRedo.setOnClickListener {
            it.pressAnim()
            if (globalRedoStack.isEmpty()) return@setOnClickListener
            val action = globalRedoStack.removeAt(globalRedoStack.size - 1)
            globalUndoStack.add(action)
            when (action) {
                is PdfAction.Draw -> adapter.redoDrawing(action.page)
                is PdfAction.Highlight -> adapter.redoHighlight(action.page)
                is PdfAction.Erase -> adapter.redoEraser(action.page)
                is PdfAction.Text -> {}
            }
        }

        binding.btnSave.setOnClickListener {
            it.pressAnim()
            showSaveDialog()
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
        val fonts = arrayOf("Default", "Serif", "Sans Serif", "Monospace", "Roboto", "Noto Serif")
        val fontAdapter =
            object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fonts) {
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
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentFontName = fonts[position]
                val fontFamily = when (position) {
                    1 -> Typeface.SERIF
                    2 -> Typeface.SANS_SERIF
                    3 -> Typeface.MONOSPACE
                    4 -> ResourcesCompat.getFont(this@EditPdfActivity, R.font.roboto)
                    5 -> ResourcesCompat.getFont(this@EditPdfActivity, R.font.noto_serif)
                    else -> Typeface.DEFAULT
                }
                selectedTextAnnotation?.let {
                    adapter.getOverlayView(currentPageIndex)?.setFontFamily(fontFamily)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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

        binding.textSizeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    currentTextSize = textSizes[position].toFloat()
                    val size = textSizes[position].toFloat()
                    selectedTextAnnotation?.let {
                        adapter.getOverlayView(currentPageIndex)?.setTextSize(size)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        binding.textColorCircle.setOnClickListener {
            showTextColorPicker()
        }

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
        binding.btnBold.setBackgroundColor(
            if (isBoldActive) ContextCompat.getColor(
                this,
                R.color.primary
            ) else Color.TRANSPARENT
        )
        binding.btnItalic.setBackgroundColor(
            if (isItalicActive) ContextCompat.getColor(
                this,
                R.color.primary
            ) else Color.TRANSPARENT
        )
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
            }

            PdfPageWithOverlayView.Mode.DRAW -> {
                showDrawOptions()
                hideTextOptions()
                hideHighlightOptions()
                hideEraserOptions()
            }

            PdfPageWithOverlayView.Mode.HIGHLIGHT -> {
                showHighlightOptions()
                hideTextOptions()
                hideDrawOptions()
                hideEraserOptions()
            }

            PdfPageWithOverlayView.Mode.ERASER -> {
                showEraserOptions()
                hideTextOptions()
                hideDrawOptions()
                hideHighlightOptions()
            }

            else -> {
                hideTextOptions()
                hideDrawOptions()
                hideHighlightOptions()
                hideEraserOptions()
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
                    val displayMetrics = resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels
                    val scale = screenWidth.toFloat() / page.width.toFloat()
                    val bitmap = Bitmap.createBitmap(
                        (page.width * scale).toInt(),
                        (page.height * scale).toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    val matrix = Matrix().apply { setScale(scale, scale) }
                    page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfBitmaps.add(bitmap)
                    page.close()
                }
                renderer.close()
                pfd.close()

                adapter = PdfPageAdapter(
                    pdfBitmaps,
                    onAddTextRequest = { pageIndex, x, y -> onAddTextRequest(pageIndex, x, y) },
                    onTextAdded = { pageIndex, text, x, y -> },
                    onTextSelected = { textAnnotation -> onTextSelected(textAnnotation) },
                    onTextSizeChanged = { _, newSize ->
                        currentTextSize = newSize
                        val index = textSizes.indexOf(newSize.toInt().toString())
                        if (index != -1) {
                            binding.textSizeSpinner.setSelection(index)
                        }
                    }
                )
                adapter.onDrawFinished = { page ->
                    pushGlobalAction(PdfAction.Draw(page))
                }

                adapter.onHighlightFinished = { page ->
                    pushGlobalAction(PdfAction.Highlight(page))
                }

                adapter.onEraserFinished = { page ->
                    pushGlobalAction(PdfAction.Erase(page))
                }

                binding.pdfRecyclerView.adapter = adapter
                updateCurrentPenSettings()
                updateCurrentHighlightSettings()
                updateCurrentEraserSettings()

                Toast.makeText(this, "PDF loaded: ${pdfBitmaps.size} pages", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun pushGlobalAction(action: PdfAction) {
        globalUndoStack.add(action)
        globalRedoStack.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfBitmaps.forEach { if (!it.isRecycled) it.recycle() }
        pdfBitmaps.clear()
    }

    private fun showSaveDialog() {
        val editText = EditText(this).apply {
            hint = "File name"
            setText("Edited_PDF")
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Save PDF")
            .setMessage("Enter file name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                pendingFileName = editText.text.toString()
                    .ifBlank { "Edited_PDF" }

                createPdfLauncher.launch("$pendingFileName.pdf")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun savePdfToUri(uri: Uri) {
        val pdfDocument = PdfDocument()

        try {
            pdfBitmaps.forEachIndexed { index, originalBitmap ->
                val overlay = adapter.getOverlayView(index)
                val finalBitmap =
                    overlay?.exportOverlayBitmap(originalBitmap) ?: originalBitmap

                val pageInfo = PdfDocument.PageInfo.Builder(
                    finalBitmap.width,
                    finalBitmap.height,
                    index + 1
                ).create()

                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(finalBitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
            }

            contentResolver.openOutputStream(uri)?.use { output ->
                pdfDocument.writeTo(output)
            }

            Toast.makeText(this, "PDF saved successfully", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun View.pressAnim() {
        animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
            .withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()
    }

    private fun updateTextColorCircle() {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(currentTextColor)
            setStroke(2.dpToPx(), Color.DKGRAY)
        }
        binding.textColorCircle.background = drawable
    }

    private fun showTextColorPicker() {
        showCommonColorPicker(
            title = "Select Text Color",
            onColorSelected = { color ->
                currentTextColor = color
                updateTextColorCircle()

                selectedTextAnnotation?.let {
                    adapter.getOverlayView(currentPageIndex)
                        ?.setTextColor(color)
                }
            }
        )
    }

    private fun showCommonColorPicker(title: String, onColorSelected: (Int) -> Unit) {
        val colors = listOf(
            Pair("Black", Color.BLACK),
            Pair("Red", Color.RED),
            Pair("Blue", Color.BLUE),
            Pair("Green", Color.GREEN),
            Pair("Yellow", Color.YELLOW),
            Pair("Pink", Color.MAGENTA),
            Pair("Cyan", Color.CYAN),
            Pair("Gray", Color.GRAY)
        )
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        }
        val grid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 2
        }
        var dialog: AlertDialog? = null
        colors.forEach { (colorName, color) ->
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
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(3.dpToPx(), Color.DKGRAY)
                }
                setOnClickListener {
                    onColorSelected(color)
                    dialog?.dismiss()
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
                ).apply { topMargin = 2.dpToPx() }
            }
            colorContainer.addView(colorView)
            colorContainer.addView(colorNameView)
            grid.addView(colorContainer)
        }
        mainContainer.addView(grid)
        builder.setView(mainContainer)
        builder.setNegativeButton("Cancel", null)
        dialog = builder.create()
        dialog.window?.setLayout(280.dpToPx(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

}