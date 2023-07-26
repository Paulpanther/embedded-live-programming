package com.elp.editor

import com.elp.actions.ReplacementCreator
import com.elp.actions.activeExampleOrCreate
import com.elp.actions.parentMember
import com.elp.ui.ELPIcons
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.jetbrains.cidr.lang.psi.OCDeclarator
import com.jetbrains.cidr.lang.psi.OCStruct

/**
 * adds line markers to create field and method replacements
 */
class ReplacementLineMarkerProvider: LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.elementType?.debugName != "IDENTIFIER") return null
        if (element.parent !is OCDeclarator) return null
        val member = element.parentMember ?: return null
        val struct = element.parentOfType<OCStruct>() ?: return null


        return LineMarkerInfo(
            element,
            element.textRange,
            ELPIcons.createReplacement,
            { "Create Replacement for member ${member.name}" },
            { _, _ ->
                activeExampleOrCreate(struct.project) { example ->
                    val created = ReplacementCreator.create(example, struct, member)
                    if (!created) {
                        // TODO move to ref
                    }
                }
            },
            GutterIconRenderer.Alignment.LEFT,
            { "Create Replacement" }
        )
    }
}
