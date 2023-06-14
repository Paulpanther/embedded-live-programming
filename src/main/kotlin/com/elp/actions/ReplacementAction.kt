package com.elp.actions

import com.elp.services.exampleService
import com.elp.ui.Replacement
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
        return project.exampleService.activeExample != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        Replacement.selectTargetThenShow(editor ?: return, element)
    }
}
