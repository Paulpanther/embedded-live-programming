package com.elp.actions

import com.elp.execution.CodeExecutionManager
import com.elp.services.probeService
import com.elp.util.appendValue
import com.elp.util.error
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.jetbrains.cidr.lang.psi.OCExpression
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition

private val supportedTypes = listOf("int", "float", "double", "bool", "std::string")

/**
 * takes the current expression and stores it as a possible probe location.
 * Then the CodeRewritingManager is executed and will try to commit those probes.
 */
class CreateProbeAction: PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "ExampleActions"
    override fun getText() = "Create Probe"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val expr = element.parentOfType<OCExpression>() ?: return false
        return expr.parentOfType<OCFunctionDefinition>() != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val expr = element.parentOfType<OCExpression>(true) ?: return
        if (!expr.isValidProbeType) {
            project.error("Return type of selected expression is not supported")
            return
        }
        probeService.requestedUserProbes.appendValue(element.containingFile.name, expr.textRange)
        CodeExecutionManager.run(project)
    }
}

val OCExpression.isValidProbeType get(): Boolean {
    val inFunction = parentOfType<OCFunctionDefinition>() != null
    return inFunction && (resolvedType.name in supportedTypes || resolvedType.isCString) && text.isNotBlank()
}
