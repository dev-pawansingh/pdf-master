package com.pawansingh.pdfeditor.activities

import android.graphics.Paint
import android.graphics.Path

sealed class Annotation {
    abstract val page: Int
}

data class DrawAnnotation(
    override val page: Int,
    val path: Path,
    val paint: Paint
) : Annotation()

data class HighlightAnnotation(
    override val page: Int,
    val path: Path,
    val paint: Paint
) : Annotation()

data class EraseAction(
    override val page: Int,
    val erasedAnnotation: Annotation,
    val index: Int
) : Annotation()

