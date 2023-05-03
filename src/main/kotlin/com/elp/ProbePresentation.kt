@file:Suppress("UnstableApiUsage")

package com.elp

import com.intellij.codeInsight.hints.presentation.*
import com.intellij.openapi.util.TextRange

const val minDelay = 100L

class ProbePresentation(
    val code: Int,
    val range: TextRange
) {

    private var text = "not run"
    var presentation: TextInlayPresentation? = null
        private set
    var markedForUpdate = false

    fun createPresentation(factory: PresentationFactory): InlayPresentation {
        val wrappedPresentation = factory.smallText(text)
        val p = (
                (wrappedPresentation as WithAttributesPresentation)
                    .presentation as InsetPresentation).presentation as TextInlayPresentation
//        val p = SparklineProbe(0, 256)
        presentation = p
        return factory.inset(wrappedPresentation, top = 5, left = 3)
    }

    fun updateText(text: String) {
        this.text = text
        probeService.probeUpdater.mark(this)
    }

    fun applyText() {
//        presentation?.update(text.toInt())
        presentation?.let { it.text = text }
    }
}
