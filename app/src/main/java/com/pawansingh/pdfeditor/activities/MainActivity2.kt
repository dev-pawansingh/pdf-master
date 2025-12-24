package com.pawansingh.pdfeditor.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pawansingh.pdfeditor.R
import com.pawansingh.pdfeditor.databinding.ActivityMain2Binding

class MainActivity2 : AppCompatActivity() {

    private lateinit var binding : ActivityMain2Binding

    private enum class PdfAction {
        VIEW, EDIT
    }
    private var currentAction: PdfAction = PdfAction.VIEW
    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            when (currentAction) {
                PdfAction.VIEW -> openPdf(it)
                PdfAction.EDIT -> openPdfForEditing(it)
                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        setupClickListeners()
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
}