package com.pawansingh.pdfeditor.activities

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pawansingh.pdfeditor.R
import com.pawansingh.pdfeditor.databinding.ActivityImageToPdfBinding

class ImageToPdf : AppCompatActivity() {

    private lateinit var binding : ActivityImageToPdfBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityImageToPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupToolbar()
        loadImage()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadImage() {
        val imageUri = intent.getStringExtra("image_uri")?.let { Uri.parse(it) }

        imageUri?.let { uri ->
            binding.imageView.setImageURI(uri)
            Toast.makeText(this, "Image loaded! PDF conversion coming soon.", Toast.LENGTH_SHORT).show()
        } ?: Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
    }
}