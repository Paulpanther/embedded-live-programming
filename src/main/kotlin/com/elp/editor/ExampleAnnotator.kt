package com.elp.editor

import com.elp.instrumentalization.loop
import com.elp.model.Example
import com.elp.services.example
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.psi.OCStruct

class ExampleAnnotator: Annotator {
    private lateinit var example: Example
    private lateinit var holder: AnnotationHolder

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        example = element.containingFile.example ?: return
        this.holder = holder
        if (element is OCStruct) annotateStruct(element)
    }

    private fun annotateStruct(struct: OCStruct) {
        val loop = struct.loop

        if (loop == null && example.parentStruct.loop == null) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Missing loop function")
                .range(struct.nameIdentifier?.textRange ?: struct.textRange)
                .create()
        }
    }
}
