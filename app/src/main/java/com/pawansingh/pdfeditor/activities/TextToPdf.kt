package com.pawansingh.pdfeditor.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pawansingh.pdfeditor.R
import com.pawansingh.pdfeditor.databinding.ActivityTextToPdfBinding
import java.io.ByteArrayOutputStream
import java.io.File
//
class TextToPdf : AppCompatActivity() {

    private lateinit var binding: ActivityTextToPdfBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTextToPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
//        setupToolbar()
//        setupClickListeners()
    }
//
//    private fun setupToolbar() {
//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        binding.toolbar.setNavigationOnClickListener {
//            finish()
//        }
//    }
//
//    private fun setupConvertButton() {
//        binding.btnConvert.setOnClickListener {
//            val text = binding.etText.text.toString()
//            if (text.isNotEmpty()) {
//                Toast.makeText(this, "PDF creation coming soon!", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//    private fun setupClickListeners() {
//        binding.btnConvert.setOnClickListener {
//            val text = binding.etText.text.toString().trim()
//            if (text.isEmpty()) {
//                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//            generatePdfFromText(text)
//        }
//    }
//
//    private fun generatePdfFromText(text: String) {
//        // Show loading state
//        binding.btnConvert.isEnabled = false
//        binding.btnConvert.text = "Creating PDF..."
//
//        Thread {
//            try {
//                val document = PDDocument()
//                val page = PDPage()
//                document.addPage(page)
//
//                val contentStream = PDPageContentStream(document, page)
//                contentStream.beginText()
//
//                // Set font and position
//                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16f)
//                contentStream.newLineAtOffset(50f, 700f)
//                contentStream.showText("Text to PDF")
//
//                // Add some space
//                contentStream.newLineAtOffset(0f, -30f)
//                contentStream.setFont(PDType1Font.HELVETICA, 12f)
//
//                // Simple text wrapping
//                val lines = wrapText(text, 80) // 80 characters per line
//                var yPosition = 670f
//
//                for (line in lines) {
//                    if (yPosition < 50f) {
//                        // Add new page if we run out of space
//                        contentStream.endText()
//                        contentStream.close()
//
//                        val newPage = PDPage()
//                        document.addPage(newPage)
//                        val newContentStream = PDPageContentStream(document, newPage)
//                        newContentStream.beginText()
//                        newContentStream.setFont(PDType1Font.HELVETICA, 12f)
//                        newContentStream.newLineAtOffset(50f, 700f)
//                        yPosition = 700f
//                    }
//
//                    contentStream.showText(line)
//                    contentStream.newLineAtOffset(0f, -15f)
//                    yPosition -= 15f
//                }
//
//                contentStream.endText()
//                contentStream.close()
//
//                // Save PDF to app storage
//                val outputStream = ByteArrayOutputStream()
//                document.save(outputStream)
//                document.close()
//
//                val fileName = "text_pdf_${System.currentTimeMillis()}.pdf"
//                val pdfUri = savePdfToAppStorage(outputStream.toByteArray(), fileName)
//
//                runOnUiThread {
//                    binding.btnConvert.isEnabled = true
//                    binding.btnConvert.text = "Create PDF"
//
//                    Toast.makeText(this, "PDF Created Successfully!", Toast.LENGTH_SHORT).show()
//
//                    // Open the PDF with system viewer
//                    openPdfWithSystemViewer(pdfUri)
//                }
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//                runOnUiThread {
//                    binding.btnConvert.isEnabled = true
//                    binding.btnConvert.text = "Create PDF"
//                    Toast.makeText(this, "Error creating PDF: ${e.message}", Toast.LENGTH_LONG).show()
//                }
//            }
//        }.start()
//    }
//
//    private fun wrapText(text: String, maxLineLength: Int): List<String> {
//        val words = text.split(" ")
//        val lines = mutableListOf<String>()
//        var currentLine = ""
//
//        for (word in words) {
//            if (currentLine.length + word.length + 1 <= maxLineLength) {
//                currentLine = if (currentLine.isEmpty()) word else "$currentLine $word"
//            } else {
//                if (currentLine.isNotEmpty()) {
//                    lines.add(currentLine)
//                }
//                currentLine = if (word.length > maxLineLength) {
//                    // Handle very long words by breaking them
//                    word.chunked(maxLineLength).first()
//                } else {
//                    word
//                }
//            }
//        }
//
//        if (currentLine.isNotEmpty()) {
//            lines.add(currentLine)
//        }
//
//        return lines
//    }
//
//    private fun savePdfToAppStorage(pdfBytes: ByteArray, fileName: String): Uri {
//        // Create pdfs directory if it doesn't exist
//        val pdfsDir = File(filesDir, "pdfs")
//        if (!pdfsDir.exists()) {
//            pdfsDir.mkdirs()
//        }
//
//        val file = File(pdfsDir, fileName)
//        file.writeBytes(pdfBytes)
//
//        return androidx.core.content.FileProvider.getUriForFile(
//            this,
//            "${packageName}.provider",
//            file
//        )
//    }
//
//    private fun openPdfWithSystemViewer(uri: Uri) {
//        val intent = Intent(Intent.ACTION_VIEW).apply {
//            setDataAndType(uri, "application/pdf")
//            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//        }
//
//        try {
//            startActivity(intent)
//        } catch (e: Exception) {
//            Toast.makeText(this, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
//        }
//    }
}