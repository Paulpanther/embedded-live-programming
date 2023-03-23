@file:Suppress("UnstableApiUsage")

package com.elp

import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.InsetPresentation
import com.intellij.codeInsight.hints.presentation.TextInlayPresentation
import com.intellij.codeInsight.hints.presentation.WithAttributesPresentation
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

    fun createPresentation(factory: InlayPresentationFactory): InlayPresentation {
        val wrappedPresentation = factory.smallText(text)
        val p = (
                (wrappedPresentation as WithAttributesPresentation)
                    .presentation as InsetPresentation).presentation as TextInlayPresentation
        presentation = p
        return wrappedPresentation
    }

    fun updateText(text: String) {
        this.text = text
        probeService.probeUpdater.mark(this)
    }

    fun applyText() {
        presentation?.let { it.text = text }
    }
}
