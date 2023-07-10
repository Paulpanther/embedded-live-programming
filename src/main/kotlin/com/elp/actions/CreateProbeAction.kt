package com.elp.actions

import com.elp.instrumentalization.InstrumentalizationManager
import com.elp.services.probeService
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.jetbrains.cidr.lang.psi.OCExpression
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition
import com.jetbrains.rd.util.getOrCreate

class CreateProbeAction: PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "ExampleActions"
    override fun getText() = "Create Probe"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val expr = element.parentOfType<OCExpression>() ?: return false
        return expr.parentOfType<OCFunctionDefinition>() != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val expr = element.parentOfType<OCExpression>() ?: return
        probeService.requestedUserProbes.getOrCreate(element.containingFile.virtualFile) { mutableListOf() } += expr.textRange
        InstrumentalizationManager.run(project)
    }
}