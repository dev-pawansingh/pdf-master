package com.pawansingh.pdfeditor.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PDFFile(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val pageCount: Int = 0
) {
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    fun getFormattedDate(): String {
        val date = Date(lastModified)
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return format.format(date)
    }
}
