package com.pawansingh.pdfeditor.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pawansingh.pdfeditor.R
import com.pawansingh.pdfeditor.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentAction: PdfAction = PdfAction.VIEW

    private enum class PdfAction {
        VIEW, EDIT
    }

    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            when (currentAction) {
                PdfAction.VIEW -> openPdf(it)
                PdfAction.EDIT -> openPdfForEditing(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        setupClickListeners()
        setupBottomNavigation()
    }

    private fun setupClickListeners() {
        binding.openPdfCard.setOnClickListener {
            currentAction = PdfAction.VIEW
            pickPdf()
        }

        binding.editPdfCard.setOnClickListener {
            currentAction = PdfAction.EDIT
            pickPdf()
        }

        binding.imageToPdfCard.setOnClickListener {
            startActivity(Intent(this, ImageToPdf::class.java))
        }

        binding.textToPdfCard.setOnClickListener {
            startActivity(Intent(this, TextToPdf::class.java))
        }

        binding.wordToPdfCard.setOnClickListener {
            Toast.makeText(this, "Word to PDF - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        binding.pptToPdfCard.setOnClickListener {
            Toast.makeText(this, "PPT to PDF - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        binding.excelToPdfCard.setOnClickListener {
            Toast.makeText(this, "Excel to PDF - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        binding.htmlToPdfCard.setOnClickListener {
            Toast.makeText(this, "HTML to PDF - Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_files -> {
                    Toast.makeText(this, "Files - Coming Soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_tools -> {
                    Toast.makeText(this, "Tools - Coming Soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun pickPdf() {
        pdfPickerLauncher.launch("application/pdf")
    }

    private fun openPdf(uri: Uri) {
        val intent = Intent(this, PdfXActivity::class.java).apply { putExtra("pdfUri", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun openPdfForEditing(uri: Uri) {
        val intent = Intent(this, EditPdfActivity::class.java)
        intent.putExtra("pdfUri", uri)
        startActivity(intent)
    }

    fun savePdfToAppStorage(pdfBytes: ByteArray, fileName: String): Uri {
        val file = File(filesDir, "pdfs/$fileName")
        file.parentFile?.mkdirs()
        file.writeBytes(pdfBytes)

        return androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )
    }
}