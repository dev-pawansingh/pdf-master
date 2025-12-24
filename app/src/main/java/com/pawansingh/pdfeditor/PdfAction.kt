package com.pawansingh.pdfeditor

sealed class PdfAction {
    data class Draw(val page: Int) : PdfAction()
    data class Highlight(val page: Int) : PdfAction()
    data class Erase(val page: Int) : PdfAction()
    data class Text(val page: Int) : PdfAction()
}
