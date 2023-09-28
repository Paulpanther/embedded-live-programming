package com.paulmethfessel.elp.actions

import com.paulmethfessel.elp.services.example
import com.paulmethfessel.elp.services.isExample
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class JumpToSourceAction: IntentionAction {
    override fun startInWriteAction() = false
    override fun getText() = "Jump to source"
    override fun getFamilyName() = "ExampleActions"
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = file?.isExample == true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val example = file?.example ?: return
        example.parentClazz.file.navigate(true)
    }
}
