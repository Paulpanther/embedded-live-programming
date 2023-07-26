package com.elp.editor

import com.elp.services.probeService
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.psi.PsiElement

/**
 * highlights possible probe expressions.
 * TODO sometimes these don't show correctly. Why?
 */
class ProbeAnnotator: Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val probes = probeService.probes[element.containingFile.name] ?: return
        val probe = probes.find { it.range == element.textRange } ?: return

        // TODO change to custom TextAttributes
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.textRange)
            .textAttributes(CodeInsightColors.RUNTIME_ERROR)
            .create()
    }
}
