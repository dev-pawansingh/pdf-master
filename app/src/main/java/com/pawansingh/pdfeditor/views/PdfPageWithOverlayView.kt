package com.pawansingh.pdfeditor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class PdfPageWithOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Mode {
        HAND, // For scrolling/selection
        TEXT, // For adding text
        DRAW, // For drawing
        HIGHLIGHT, // For highlighting
        ERASER, // For erasing
        MOVE_TEXT // For moving existing text
    }

    private var currentMode = Mode.HAND
    private val paths = mutableListOf<DrawingPath>()
    private val textAnnotations = mutableListOf<TextAnnotation>()
    private var currentPath = Path()
    private var currentPaint = Paint()
    private var selectedTextAnnotation: TextAnnotation? = null
    private var isMovingText = false
    private var startX = 0f
    private var startY = 0f

    // Callback for when user wants to add text
    var onAddTextRequest: ((Float, Float) -> Unit)? = null

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
        selectedTextAnnotation = null
        isMovingText = false

        when (mode) {
            Mode.DRAW -> {
                currentPaint.style = Paint.Style.STROKE
                currentPaint.color = Color.RED
                currentPaint.strokeWidth = 5f
                currentPaint.xfermode = null
            }
            Mode.HIGHLIGHT -> {
                currentPaint.style = Paint.Style.FILL_AND_STROKE
                currentPaint.color = Color.YELLOW and 0x80FFFFFF.toInt()
                currentPaint.strokeWidth = 20f
                currentPaint.xfermode = null
            }
            Mode.ERASER -> {
                currentPaint.style = Paint.Style.STROKE
                currentPaint.color = Color.TRANSPARENT
                currentPaint.strokeWidth = 20f
                currentPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            Mode.HAND, Mode.TEXT, Mode.MOVE_TEXT -> {
                // These modes don't need paint setup
            }
        }
        invalidate()
    }

    fun addTextAnnotation(text: String, x: Float, y: Float) {
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            isAntiAlias = true
        }
        textAnnotations.add(TextAnnotation(text, x, y, textPaint))
        invalidate()
    }

    fun updateTextAnnotation(oldText: TextAnnotation, newText: TextAnnotation) {
        val index = textAnnotations.indexOf(oldText)
        if (index != -1) {
            textAnnotations[index] = newText
            invalidate()
        }
    }

    fun removeTextAnnotation(text: TextAnnotation) {
        textAnnotations.remove(text)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw all paths
        paths.forEach { drawingPath ->
            canvas.drawPath(drawingPath.path, drawingPath.paint)
        }

        // Draw current path
        if (!currentPath.isEmpty) {
            canvas.drawPath(currentPath, currentPaint)
        }

        // Draw text annotations
        textAnnotations.forEach { textAnn ->
            // Draw selection rectangle if selected
            if (textAnn == selectedTextAnnotation) {
                val selectionPaint = Paint().apply {
                    color = Color.BLUE and 0x40FFFFFF.toInt()
                    style = Paint.Style.FILL
                }
                val bounds = Rect()
                textAnn.paint.getTextBounds(textAnn.text, 0, textAnn.text.length, bounds)
                canvas.drawRect(
                    textAnn.x - 10,
                    textAnn.y - bounds.height() - 5,
                    textAnn.x + bounds.width() + 10,
                    textAnn.y + 5,
                    selectionPaint
                )
            }
            canvas.drawText(textAnn.text, textAnn.x, textAnn.y, textAnn.paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = x
                startY = y

                when (currentMode) {
                    Mode.DRAW, Mode.HIGHLIGHT, Mode.ERASER -> {
                        currentPath.moveTo(x, y)
                        return true
                    }
                    Mode.TEXT -> {
                        // Request text input at this position
                        onAddTextRequest?.invoke(x, y)
                        return true
                    }
                    Mode.MOVE_TEXT -> {
                        // Check if touching any text annotation
                        selectedTextAnnotation = findTextAtPosition(x, y)
                        isMovingText = selectedTextAnnotation != null
                        return true
                    }
                    Mode.HAND -> {
                        // Allow parent to handle scrolling
                        return false
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                when (currentMode) {
                    Mode.DRAW, Mode.HIGHLIGHT, Mode.ERASER -> {
                        currentPath.lineTo(x, y)
                        invalidate()
                        return true
                    }
                    Mode.MOVE_TEXT -> {
                        if (isMovingText && selectedTextAnnotation != null) {
                            val oldText = selectedTextAnnotation!!
                            val newText = oldText.copy(x = x, y = y)
                            updateTextAnnotation(oldText, newText)
                            selectedTextAnnotation = newText
                            return true
                        }
                    }
                    Mode.HAND -> {
                        return false
                    }
                    else -> {}
                }
            }
            MotionEvent.ACTION_UP -> {
                when (currentMode) {
                    Mode.DRAW, Mode.HIGHLIGHT, Mode.ERASER -> {
                        paths.add(DrawingPath(Path(currentPath), Paint(currentPaint)))
                        currentPath.reset()
                        invalidate()
                        return true
                    }
                    Mode.MOVE_TEXT -> {
                        isMovingText = false
                        return true
                    }
                    Mode.HAND -> {
                        // Check if it was a tap (not scroll)
                        val dx = abs(x - startX)
                        val dy = abs(y - startY)
                        if (dx < 10 && dy < 10) {
                            // It was a tap, not scroll - select text if any
                            selectedTextAnnotation = findTextAtPosition(x, y)
                            invalidate()
                        }
                        return false
                    }
                    else -> {}
                }
            }
        }
        return false
    }

    private fun findTextAtPosition(x: Float, y: Float): TextAnnotation? {
        return textAnnotations.find { textAnn ->
            val bounds = Rect()
            textAnn.paint.getTextBounds(textAnn.text, 0, textAnn.text.length, bounds)
            val textRect = RectF(
                textAnn.x - 10,
                textAnn.y - bounds.height() - 5,
                textAnn.x + bounds.width() + 10,
                textAnn.y + 5
            )
            textRect.contains(x, y)
        }
    }

    fun getTextAnnotations(): List<TextAnnotation> = textAnnotations

    fun clearAll() {
        paths.clear()
        textAnnotations.clear()
        currentPath.reset()
        selectedTextAnnotation = null
        invalidate()
    }

    // Data classes
    data class DrawingPath(val path: Path, val paint: Paint)
    data class TextAnnotation(val text: String, val x: Float, val y: Float, val paint: Paint)
}