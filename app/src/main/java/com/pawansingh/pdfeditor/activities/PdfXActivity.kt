package com.pawansingh.pdfeditor.activities

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.alamin5g.pdf.PDFView
import com.pawansingh.pdfeditor.R
import com.pawansingh.pdfeditor.databinding.ActivityPdfxBinding

class PdfXActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfxBinding
    private var totalPages = 0
    private var lastFraction = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPdfxBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val pdfUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("pdfUri", Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("pdfUri")
        }
        if (pdfUri != null) {
            showPdf(pdfUri)
        } else {
            finish()
        }
    }

    private fun showPdf(pdfUri: Uri) {
        binding.pdfView.fromUri(pdfUri)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true) // zoom
            .enableAnnotationRendering(true)
            .defaultPage(0)
            .spacing(10)
            .useBestQuality(true)
            .enableAntialiasing(true)
            .pageFitPolicy(PDFView.FitPolicy.BOTH)
            .onError { throwable ->
                throwable.printStackTrace()
            }
            .onLoad { pageCount ->
                totalPages = pageCount
                setupThumbSize()
                binding.pageBubble.visibility = View.VISIBLE
                updateScrollIndicator(0)
            }
            .onPageChange { page, _ ->
                updateScrollIndicator(page)
            }
            .load()
    }
    private fun updateScrollIndicator(currentPage: Int) {
        if (totalPages <= 1) return
        val container = binding.scrollContainer
        val bubble = binding.pageBubble
        val thumb = binding.scrollThumb
        val safePage = currentPage.coerceIn(0, totalPages - 1)
        bubble.text = "${safePage + 1} / $totalPages"
        container.post {
            val containerHeight = container.height
            val bubbleHeight = bubble.height
            val thumbHeight = thumb.height
            val maxYForBubble = containerHeight - bubbleHeight
            val maxYForThumb = containerHeight - thumbHeight
            if (maxYForBubble <= 0 || maxYForThumb <= 0) return@post
            val safePage = currentPage.coerceIn(0, totalPages - 1)
            val isLastPage = safePage >= totalPages -2
            val fraction = if (isLastPage) 1f
                else safePage.toFloat() / (totalPages - 1)
            val bubbleTargetY = maxYForBubble * fraction
            val thumbTargetY = maxYForThumb * fraction
            bubble.animate()
                .translationY(bubbleTargetY)
                .setDuration(180)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()

            thumb.animate()
                .translationY(thumbTargetY)
                .setDuration(180)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()

            lastFraction = fraction
        }
    }
    private fun setupThumbSize() {
        binding.scrollContainer.post {
            val containerHeight = binding.scrollContainer.height

            if (containerHeight <= 0 || totalPages <= 0) return@post

            val maxVisiblePages = 10   // tweak this if you want
            val effectivePages = minOf(totalPages, maxVisiblePages)

            val thumbHeight = containerHeight / effectivePages

            val minThumb = dpToPx(24)
            val maxThumb = containerHeight   // for 1-page PDFs

            val finalHeight = thumbHeight.coerceIn(minThumb, maxThumb)

            binding.scrollThumb.layoutParams.height = finalHeight
            binding.scrollThumb.requestLayout()
        }
    }
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}