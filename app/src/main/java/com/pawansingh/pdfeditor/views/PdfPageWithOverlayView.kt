package com.pawansingh.pdfeditor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.sqrt

class PdfPageWithOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Mode { HAND, TEXT, DRAW, HIGHLIGHT, ERASER, MOVE_TEXT }

    private var currentMode = Mode.HAND
    private val paths = mutableListOf<DrawingPath>()
    private val textAnnotations = mutableListOf<TextAnnotation>()
    private var currentPath = Path()
    private var currentPaint = Paint()
    private var selectedTextAnnotation: TextAnnotation? = null
    private var isMovingText = false
    private var startX = 0f
    private var startY = 0f

    // Text styling
    private var currentTextColor = Color.BLACK
    private var currentTextSize = 20f
    private var isBold = false
    private var isItalic = false
    private var currentFontFamily = Typeface.DEFAULT

    // Selection
    private val selectionPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
    }

    private var isResizingText = false

    // Callbacks
    var onAddTextRequest: ((x: Float, y: Float) -> Unit)? = null
    var onTextSelected: ((TextAnnotation?) -> Unit)? = null
    var onTouchHandled: ((Boolean) -> Unit)? = null

    init {
        setupPaint()
    }

    private fun setupPaint() {
        currentPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    fun setMode(mode: Mode) {
        currentMode = mode
        if (mode != Mode.MOVE_TEXT) {
            selectedTextAnnotation = null
            onTextSelected?.invoke(null)
        }
        isMovingText = false

        when (mode) {
            Mode.DRAW -> {
                currentPaint.style = Paint.Style.STROKE
                currentPaint.color = Color.RED
                currentPaint.strokeWidth = 5f
            }
            Mode.HIGHLIGHT -> {
                currentPaint.style = Paint.Style.FILL_AND_STROKE
                currentPaint.color = Color.YELLOW and 0x80FFFFFF.toInt()
                currentPaint.strokeWidth = 20f
            }
            Mode.ERASER -> {
                currentPaint.style = Paint.Style.STROKE
                currentPaint.color = Color.TRANSPARENT
                currentPaint.strokeWidth = 20f
                currentPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            else -> {}
        }
        invalidate()
    }

    // Text styling methods
    fun setTextColor(color: Int) {
        currentTextColor = color
        selectedTextAnnotation?.let { textAnn ->
            val newPaint = Paint(textAnn.paint).apply { this.color = color }
            updateTextAnnotation(textAnn, textAnn.copy(paint = newPaint))
        }
    }

    fun setTextSize(size: Float) {
        currentTextSize = size
        selectedTextAnnotation?.let { textAnn ->
            val newPaint = Paint(textAnn.paint).apply { textSize = size }
            updateTextAnnotation(textAnn, textAnn.copy(paint = newPaint))
        }
    }

    fun setTextBold(bold: Boolean) {
        isBold = bold
        updateSelectedTextStyle()
    }

    fun setTextItalic(italic: Boolean) {
        isItalic = italic
        updateSelectedTextStyle()
    }

    fun setFontFamily(fontFamily: Typeface) {
        currentFontFamily = fontFamily
        updateSelectedTextStyle()
    }

    private fun updateSelectedTextStyle() {
        selectedTextAnnotation?.let { textAnn ->
            val style = when {
                isBold && isItalic -> Typeface.BOLD_ITALIC
                isBold -> Typeface.BOLD
                isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            val newPaint = Paint(textAnn.paint).apply {
                typeface = Typeface.create(currentFontFamily, style)
            }
            updateTextAnnotation(textAnn, textAnn.copy(paint = newPaint))
        }
    }

    fun addTextAnnotation(text: String, x: Float, y: Float) {
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val textPaint = Paint().apply {
            color = currentTextColor
            textSize = currentTextSize
            isAntiAlias = true
            typeface = Typeface.create(currentFontFamily, style)
        }
        val newAnnotation = TextAnnotation(text, x, y, textPaint)
        textAnnotations.add(newAnnotation)
        selectedTextAnnotation = newAnnotation
        onTextSelected?.invoke(newAnnotation)
        invalidate()

        setMode(Mode.HAND)
    }

    fun updateTextAnnotation(oldText: TextAnnotation, newText: TextAnnotation) {
        val index = textAnnotations.indexOf(oldText)
        if (index != -1) {
            textAnnotations[index] = newText
            if (selectedTextAnnotation == oldText) {
                selectedTextAnnotation = newText
            }
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paths.forEach { canvas.drawPath(it.path, it.paint) }
        if (!currentPath.isEmpty) canvas.drawPath(currentPath, currentPaint)

        textAnnotations.forEach { textAnn ->
            canvas.drawText(textAnn.text, textAnn.x, textAnn.y, textAnn.paint)
            if (textAnn == selectedTextAnnotation) {
                drawSelectionBorder(canvas, textAnn)
                drawDeleteButton(canvas, textAnn)
                drawResizeButton(canvas, textAnn)
            }
        }
    }

    private fun drawSelectionBorder(canvas: Canvas, textAnn: TextAnnotation) {
        val textWidth = textAnn.paint.measureText(textAnn.text)
        val metrics = textAnn.paint.fontMetrics
        val borderRect = RectF(
            textAnn.x - 15f,
            textAnn.y + metrics.top - 10f,
            textAnn.x + textWidth + 15f,
            textAnn.y + metrics.bottom + 10f
        )
        canvas.drawRect(borderRect, selectionPaint)
    }

    private fun drawDeleteButton(canvas: Canvas, textAnn: TextAnnotation) {
        val textWidth = textAnn.paint.measureText(textAnn.text)
        val metrics = textAnn.paint.fontMetrics

        // Draw delete button (red circle with X) at top-right corner
        val deleteButtonX = textAnn.x + textWidth + 25f
        val deleteButtonY = textAnn.y + metrics.top - 20f
        val buttonRadius = 25f

        // Draw red circle
        val deleteButtonPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(deleteButtonX, deleteButtonY, buttonRadius, deleteButtonPaint)

        // Draw white X
        val buttonTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("×", deleteButtonX, deleteButtonY + 6f, buttonTextPaint)
    }

    private fun drawResizeButton(canvas: Canvas, textAnn: TextAnnotation) {
        val textWidth = textAnn.paint.measureText(textAnn.text)
        val metrics = textAnn.paint.fontMetrics

        // Draw resize button (green circle) at bottom-right corner
        val resizeButtonX = textAnn.x + textWidth + 25f
        val resizeButtonY = textAnn.y + 25f
        val buttonRadius = 25f

        // Draw green circle
        val resizeButtonPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(resizeButtonX, resizeButtonY, buttonRadius, resizeButtonPaint)

        // Draw resize icon
        val buttonTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("±", resizeButtonX, resizeButtonY + 6f, buttonTextPaint)
    }

    // Add button touch handling
    private fun handleUITouch(x: Float, y: Float): Boolean {
        selectedTextAnnotation?.let { textAnn ->
            // Check delete button
            if (isPointOnDeleteButton(x, y, textAnn)) {
                deleteSelectedText()
                return true
            }

            // Check resize button
            if (isPointOnResizeButton(x, y, textAnn)) {
                isResizingText = true
                return true
            }
        }
        return false
    }

    private fun isPointOnDeleteButton(x: Float, y: Float, textAnn: TextAnnotation): Boolean {
        val textWidth = textAnn.paint.measureText(textAnn.text)
        val metrics = textAnn.paint.fontMetrics

        val deleteButtonX = textAnn.x + textWidth + 25f
        val deleteButtonY = textAnn.y + metrics.top - 20f
        val buttonRadius = 15f

        val distance = sqrt(
            (x - deleteButtonX) * (x - deleteButtonX) +
                    (y - deleteButtonY) * (y - deleteButtonY)
        )
        return distance <= buttonRadius
    }

    private fun isPointOnResizeButton(x: Float, y: Float, textAnn: TextAnnotation): Boolean {
        val textWidth = textAnn.paint.measureText(textAnn.text)
        val metrics = textAnn.paint.fontMetrics

        val resizeButtonX = textAnn.x + textWidth + 25f
        val resizeButtonY = textAnn.y + 25f
        val buttonRadius = 15f

        val distance = sqrt(
            (x - resizeButtonX) * (x - resizeButtonX) +
                    (y - resizeButtonY) * (y - resizeButtonY)
        )
        return distance <= buttonRadius
    }

    private fun handleTextResize(x: Float, y: Float) {
        selectedTextAnnotation?.let { textAnn ->
            val originalSize = textAnn.paint.textSize

            // Calculate distance change from resize handle start
            val deltaX = x - startX
            val deltaY = y - startY
            val delta = sqrt(deltaX * deltaX + deltaY * deltaY)

            // Determine direction: outward increases, inward decreases
            val scaleFactor = if ((deltaX + deltaY) > 0) 1f + delta/100f else 1f - delta/100f

            val newSize = (originalSize * scaleFactor).coerceIn(8f, 72f)
            if (abs(newSize - originalSize) >= 1f) {
                val newPaint = Paint(textAnn.paint).apply { textSize = newSize }
                updateTextAnnotation(textAnn, textAnn.copy(paint = newPaint))

                // Update start coordinates for smooth resizing
                startX = x
                startY = y
            }
        }
    }

    fun deleteSelectedText() {
        selectedTextAnnotation?.let { textAnn ->
            textAnnotations.remove(textAnn)
            selectedTextAnnotation = null
            onTextSelected?.invoke(null)
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = x
                startY = y

                // Check if we're interacting with delete or resize buttons
                val touchedUI = selectedTextAnnotation?.let { textAnn ->
                    when {
                        isPointOnDeleteButton(x, y, textAnn) -> {
                            deleteSelectedText()
                            true
                        }
                        isPointOnResizeButton(x, y, textAnn) -> {
                            isResizingText = true
                            true
                        }
                        else -> false
                    }
                } ?: false

                if (touchedUI) {
                    onTouchHandled?.invoke(true)
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                }

                // Check if clicking on text (for moving)
                val clickedText = findTextAtPosition(x, y)
                if (clickedText != null) {
                    selectedTextAnnotation = clickedText
                    onTextSelected?.invoke(clickedText)
                    invalidate()
                    if (!isResizingText) isMovingText = true
                    onTouchHandled?.invoke(true)
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                }

                // Current mode behavior
                when (currentMode) {
                    Mode.DRAW, Mode.HIGHLIGHT, Mode.ERASER -> {
                        currentPath.moveTo(x, y)
                        onTouchHandled?.invoke(true)
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                    Mode.TEXT -> {
                        onAddTextRequest?.invoke(x, y)
                        onTouchHandled?.invoke(true)
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                    else -> return false
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when {
                    isMovingText && selectedTextAnnotation != null -> {
                        val oldText = selectedTextAnnotation!!
                        val newText = oldText.copy(x = x, y = y)
                        updateTextAnnotation(oldText, newText)
                        selectedTextAnnotation = newText
                        onTouchHandled?.invoke(true)
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }

                    isResizingText -> {
                        handleTextResize(x, y)
                        onTouchHandled?.invoke(true)
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }

                    currentMode == Mode.DRAW || currentMode == Mode.HIGHLIGHT || currentMode == Mode.ERASER -> {
                        currentPath.lineTo(x, y)
                        invalidate()
                        onTouchHandled?.invoke(true)
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isMovingText = false
                isResizingText = false
                onTouchHandled?.invoke(false)
                parent.requestDisallowInterceptTouchEvent(false)

                if (currentMode == Mode.DRAW || currentMode == Mode.HIGHLIGHT || currentMode == Mode.ERASER) {
                    paths.add(DrawingPath(Path(currentPath), Paint(currentPaint)))
                    currentPath.reset()
                    invalidate()
                }
            }
        }

        return currentMode in listOf(Mode.DRAW, Mode.HIGHLIGHT, Mode.ERASER, Mode.TEXT)
    }

    private fun findTextAtPosition(x: Float, y: Float): TextAnnotation? {
        return textAnnotations.find { textAnn ->
            val textWidth = textAnn.paint.measureText(textAnn.text)
            val metrics = textAnn.paint.fontMetrics
            val textRect = RectF(
                textAnn.x - 20f,
                textAnn.y + metrics.top - 15f,
                textAnn.x + textWidth + 20f,
                textAnn.y + metrics.bottom + 15f
            )
            textRect.contains(x, y)
        }
    }

    fun clearSelection() {
        selectedTextAnnotation = null
        onTextSelected?.invoke(null)
        invalidate()
    }

    fun getTextRect(textAnn: TextAnnotation): RectF {
        val textWidth = textAnn.paint.measureText(textAnn.text)
        val metrics = textAnn.paint.fontMetrics
        return RectF(
            textAnn.x - 20f,
            textAnn.y + metrics.top - 15f,
            textAnn.x + textWidth + 20f,
            textAnn.y + metrics.bottom + 15f
        )
    }

    // Data classes
    data class DrawingPath(val path: Path, val paint: Paint)
    data class TextAnnotation(val text: String, val x: Float, val y: Float, val paint: Paint)
}