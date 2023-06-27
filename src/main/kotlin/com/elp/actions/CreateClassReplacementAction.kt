package com.elp.actions

import com.elp.services.exampleService
import com.elp.services.isExample
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.psi.OCStruct

class CreateClassReplacementAction: PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "ExampleActions"
    override fun getText() = "Replace Class"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (element !is OCStruct) return false
        val file = element.containingFile
        val activeExample = project.exampleService.activeExample ?: return false
        if (file.isExample) return false
        if (activeExample.parentFile == file) return false
        return true
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val struct = element as? OCStruct ?: return
    }
}
