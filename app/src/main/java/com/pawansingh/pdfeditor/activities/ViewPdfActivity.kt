package com.pawansingh.pdfeditor.activities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pawansingh.pdfeditor.R
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

class ViewPdfActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pageIndicator: TextView
    private val pages = mutableListOf<Bitmap>()
    private var scaleFactor = 1f
    private lateinit var scaleDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_pdf)

        recyclerView = findViewById(R.id.pdfRecyclerView)
        pageIndicator = findViewById(R.id.pageNumberOverlay)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(PageMarginDecoration(12))

        val pdfUri = intent.getParcelableExtra<Uri>("pdfUri")
        if (pdfUri != null) openPdf(pdfUri) else {
            Toast.makeText(this, "Failed to load PDF", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Initialize zoom gesture
        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val prevScale = scaleFactor
                scaleFactor *= detector.scaleFactor
                scaleFactor = max(1f, min(scaleFactor, 3f))

                // Apply smooth zoom to RecyclerView
                recyclerView.scaleX = scaleFactor
                recyclerView.scaleY = scaleFactor

                // Keep zoom centered
                recyclerView.pivotX = detector.focusX
                recyclerView.pivotY = detector.focusY
                return true
            }
        })

        recyclerView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            false
        }

        // Dynamic page indicator update
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lm = recyclerView.layoutManager as LinearLayoutManager

                var maxVisibleHeight = 0
                var mostVisiblePage = 0

                for (i in lm.findFirstVisibleItemPosition()..lm.findLastVisibleItemPosition()) {
                    val child = lm.findViewByPosition(i) ?: continue
                    val visibleHeight = child.height - max(0, -child.top) - max(0, child.bottom - recyclerView.height)
                    if (visibleHeight > maxVisibleHeight) {
                        maxVisibleHeight = visibleHeight
                        mostVisiblePage = i
                    }
                }

                pageIndicator.text = "${mostVisiblePage + 1} / ${pages.size}"
            }
        })

    }

    private fun openPdf(uri: Uri) {
        try {
            val file = copyUriToFile(uri)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pages.add(bmp)
                page.close()
            }

            recyclerView.adapter = PdfPageAdapter(pages)
            pageIndicator.text = "1 / ${pages.size}"

            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error opening PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyUriToFile(uri: Uri): File {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val tempFile = File(cacheDir, "temp.pdf")
        val outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return tempFile
    }
}

class PdfPageAdapter(private val pages: List<Bitmap>) :
    RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {

    inner class PdfPageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_START
        }
        return PdfPageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.imageView.setImageBitmap(pages[position])
    }

    override fun getItemCount(): Int = pages.size
}

/* ------------ Page separator with visible color ------------ */
class PageMarginDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    private val paint = Paint().apply { color = Color.parseColor("#000000") } // light gray separator

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount - 1) {
            val child = parent.getChildAt(i)
            val top = child.bottom.toFloat()
            c.drawRect(
                child.left.toFloat(),
                top,
                child.right.toFloat(),
                top + space.toFloat(),
                paint
            )
        }
    }
}