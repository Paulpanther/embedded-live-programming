@file:Suppress("UnstableApiUsage")

package com.elp

import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.codeInsight.hints.fireContentChanged
import com.intellij.codeInsight.hints.presentation.InsetPresentation
import com.intellij.codeInsight.hints.presentation.TextInlayPresentation
import com.intellij.codeInsight.hints.presentation.WithAttributesPresentation

class ProbePresentation {
    private var presentation: TextInlayPresentation? = null

    fun createPresentation(factory: InlayPresentationFactory) {
        val wrappedPresentation = factory.smallText("Hey")
        presentation = (
                (wrappedPresentation as WithAttributesPresentation)
                    .presentation as InsetPresentation).presentation as TextInlayPresentation
    }

    fun updateText(text: String) {
        presentation?.apply {
            this.text = text
            fireContentChanged()
        }
    }
}
