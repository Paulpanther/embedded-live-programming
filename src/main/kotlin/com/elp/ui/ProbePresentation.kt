@file:Suppress("UnstableApiUsage")

package com.elp.ui

import com.elp.services.probeService
import com.intellij.codeInsight.hints.presentation.*
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.TextRange

class ProbePresentation(
    val code: Int,
    val range: TextRange
) {

    private var text = "not run"
    private var presentation: SparklineProbe? = null
        private set
    var editor: EditorImpl? = null
    var markedForUpdate = false

    fun createPresentation(factory: PresentationFactory, editor: EditorImpl): InlayPresentation {
        this.editor = editor
//        val wrappedPresentation = factory.smallText(text)
//        val p = (
//                (wrappedPresentation as WithAttributesPresentation)
//                    .presentation as InsetPresentation).presentation as TextInlayPresentation

        val p = SparklineProbe(editor)
        presentation = p
//        return factory.inset(p, top = 5, left = 3) // for text
        return p
    }

    fun updateText(text: String) {
        this.text = text
        probeService.probeUpdater.mark(this)
    }

    fun applyText() {
        presentation?.update(text.toInt())
//        presentation?.let { it.text = text }
    }
}
