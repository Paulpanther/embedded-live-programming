package com.elp.actions

import com.elp.services.exampleService
import com.elp.services.isExample
import com.elp.util.document
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.cidr.lang.psi.OCStruct

class CreateClassReplacementAction: PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "ExampleActions"
    override fun getText() = "Replace Class"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val struct = element.parent as? OCStruct ?: return false
        val file = element.containingFile
        val activeExample = project.exampleService.activeExample ?: return false
        if (file.isExample) return false
        if (activeExample.parentFile == file) return false
        if (activeExample.ownStructs.any { it.name == struct.name }) return false
        return true
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val struct = (element.parent as? OCStruct)?.name ?: return
        val example = project.exampleService.activeExample ?: return

        val file = example.ownFile
        val offset = file.endOffset
        val doc = file.document ?: return

        runWriteAction {
            executeCommand {
                doc.insertString(offset, "\nclass $struct {\n\t\n};")
                example.editor.requestFocusInWindow()
                example.editor.editor?.caretModel?.moveToOffset(offset + struct.length + 10)
            }
        }
    }
}
