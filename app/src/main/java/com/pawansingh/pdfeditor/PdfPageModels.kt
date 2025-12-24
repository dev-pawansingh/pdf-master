package com.pawansingh.pdfeditor

data class PdfPageInfo(
    val width: Float,   // PDF units
    val height: Float   // PDF units
)

data class PageRenderInfo(
    val left: Float,    // screen px
    val top: Float,     // screen px
    val width: Float,   // screen px
    val height: Float   // screen px
)
