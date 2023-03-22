@file:Suppress("UnstableApiUsage")

package com.elp

import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.codeInsight.hints.fireContentChanged
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.InsetPresentation
import com.intellij.codeInsight.hints.presentation.TextInlayPresentation
import com.intellij.codeInsight.hints.presentation.WithAttributesPresentation
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.util.TextRange

const val minDelay = 100L

class ProbePresentation(
    val code: Int,
    val range: TextRange
) {
    private var lastUpdate = System.currentTimeMillis()
    private var inUpdate = false

    private var text = "not run"
    var presentation: TextInlayPresentation? = null
        private set

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
        presentation?.apply {
            this.text = text
            if (!inUpdate && lastUpdate - System.currentTimeMillis() >= minDelay) {
                inUpdate = true

                invokeLater {
                    fireContentChanged()
                    lastUpdate = System.currentTimeMillis()
                    inUpdate = false
                }
            }
        }
    }
}
