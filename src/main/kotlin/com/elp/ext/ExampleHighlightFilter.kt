package com.elp.ext

import com.intellij.codeInsight.daemon.ProblemHighlightFilter
import com.intellij.psi.PsiFile

class ExampleHighlightFilter: ProblemHighlightFilter() {
    override fun shouldHighlight(psiFile: PsiFile): Boolean {
        return false
    }
}
