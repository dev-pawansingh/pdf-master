package com.pawansingh.pdfeditor.activities

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.pawansingh.pdfeditor.PageRenderInfo
import com.pawansingh.pdfeditor.PdfPageInfo
import com.pawansingh.pdfeditor.ToolMode

class AnnotationOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var pdfXOffset = 0f
    private var pdfYOffset = 0f
    private var currentPage = 0
    private var pdfScale = 1f

    var currentTool: ToolMode = ToolMode.HAND
    private var eraserRadius = 30f

    private val annotations = mutableListOf<Annotation>()
    private var currentPath: Path? = null
    private var currentPaint: Paint? = null

    private val undoStack = mutableListOf<Annotation>()
    private val redoStack = mutableListOf<Annotation>()

    lateinit var pageInfoProvider: (Int) -> Pair<PdfPageInfo, PageRenderInfo>

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setCurrentPage(page: Int) {
        currentPage = page
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val (pdf, render) = pageInfoProvider(currentPage)
        Log.d(
            "RenderInfo",
            "page=$currentPage left=${render.left} top=${render.top} w=${render.width} h=${render.height}"
        )
        if (render.width <= 0f || render.height <= 0f) {
            return
        }
        canvas.save()
        canvas.translate(render.left, render.top)
        canvas.scale(
            render.width / pdf.width,
            render.height / pdf.height
        )
        annotations.forEach {
            if (it.page == currentPage) {
                when (it) {
                    is DrawAnnotation -> canvas.drawPath(it.path, it.paint)
                    is HighlightAnnotation -> canvas.drawPath(it.path, it.paint)
                    else -> {}
                }
            }
        }
        currentPath?.let {
            currentPaint?.let { paint ->
                canvas.drawPath(it, paint)
            }
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (currentTool == ToolMode.HAND || currentTool == ToolMode.TEXT) {
            return false
        }
        val pdfX = (event.x + pdfXOffset) / pdfScale
        val pdfY = (event.y + pdfYOffset) / pdfScale

        when (currentTool) {
            ToolMode.DRAW -> handleDraw(event)
            ToolMode.HIGHLIGHT -> handleHighlight(event.action, pdfX, pdfY)
            ToolMode.ERASER -> handleErase(pdfX, pdfY)
            else -> return false
        }
        invalidate()
        return true
    }

    private fun handleDraw(event: MotionEvent) {
        val p = screenToPdf(currentPage, event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val p = screenToPdf(currentPage, event.x, event.y)
                currentPath = Path().apply {
                    moveTo(p.x, p.y)
                }
                currentPaint = Paint().apply {
                    color = Color.RED
                    strokeWidth = 6f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
            }

            MotionEvent.ACTION_MOVE -> {
                currentPath?.lineTo(p.x, p.y)
            }

            MotionEvent.ACTION_UP -> {
                currentPath?.let { path ->
                    currentPaint?.let { paint ->
                        val ann = DrawAnnotation(
                            page = currentPage,
                            path = Path(path),          // clone path
                            paint = Paint(paint)        // clone paint
                        )
                        annotations.add(ann)
                        undoStack.add(ann)
                        redoStack.clear()
                    }
                }
                currentPath = null
                currentPaint = null
            }
        }
    }

    private fun handleHighlight(action: Int, x: Float, y: Float) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path().apply {
                    moveTo(x, y)
                }
                currentPaint = Paint().apply {
                    color = Color.YELLOW
                    strokeWidth = 20f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    alpha = 80
                }
            }

            MotionEvent.ACTION_MOVE -> {
                currentPath?.lineTo(x, y)
            }

            MotionEvent.ACTION_UP -> {
                currentPath?.let {
                    val ann = HighlightAnnotation(
                        page = currentPage,
                        path = it,
                        paint = Paint(currentPaint)
                    )
                    annotations.add(ann)
                    undoStack.add(ann)
                    redoStack.clear()
                }
                currentPath = null
                currentPaint = null
            }
        }
    }

    private fun handleErase(x: Float, y: Float) {
        val radius = eraserRadius / pdfScale

        val iterator = annotations.iterator()
        while (iterator.hasNext()) {
            val ann = iterator.next()
            if (ann.page != currentPage) continue

            val path = when (ann) {
                is DrawAnnotation -> ann.path
                is HighlightAnnotation -> ann.path
                else -> null
            } ?: continue

            val bounds = RectF()
            path.computeBounds(bounds, true)
            bounds.inset(-radius, -radius)

            if (!bounds.contains(x, y)) continue

            val index = annotations.indexOf(ann)
            iterator.remove()
            undoStack.add(EraseAction(currentPage, ann, index))
            redoStack.clear()
            break
        }
    }

    fun updatePdfTransform(xOffset: Float, yOffset: Float, scale: Float) {
        pdfXOffset = xOffset
        pdfYOffset = yOffset
        pdfScale = scale
        invalidate()
    }

    private fun screenToPdf(page: Int, sx: Float, sy: Float): PointF {
        val (pdf, render) = pageInfoProvider(page)
        val x = (sx - render.left) / render.width * pdf.width
        val y = (sy - render.top) / render.height * pdf.height
        return PointF(x, y)
    }
}
