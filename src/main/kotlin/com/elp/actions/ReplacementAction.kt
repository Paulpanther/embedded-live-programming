package com.elp.actions

import com.elp.ui.ReplacementBallon
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class ReplacementAction: PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String {
        return "Family Name"
    }

    override fun getText(): String {
        return "Replace Expression"
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return true
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        ReplacementBallon.create(editor ?: return, element)
    }
}
