package com.elp

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfTypes
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.psi.OCAssignmentExpression
import com.jetbrains.cidr.lang.psi.OCDeclarationStatement
import com.jetbrains.cidr.lang.util.OCElementFactory
import kotlin.reflect.KClass

object FileAnalyzer {
    fun analyze(file: PsiFile) {
        // Find Probes
        // Store unfinished ProbePresentations in Service
        // Rewrite copy of file with probes
        // Send file to runner

        val psiProbes = findProbeElements(file)
        val probes = psiProbes.withIndex().map { (i, it) ->
            ProbePresentation(i, it.range) }
        probeService.probes = probes.toMutableList()

        invokeLater {
            runWriteAction {
                executeCommand {
                    val clone = PsiFileFactory
                        .getInstance(file.project)
                        .createFileFromText(OCLanguage.getInstance(), file.text)

                    val clonedProbeElements = psiProbes
                        .map { clone.findElementAt(it.range.startOffset)?.parentOfTypes(it.parent) ?: error("Could not find Psi Element of Probe") }

                    for ((i, element) in clonedProbeElements.withIndex()) {
                        val right = when (element) {
                            is OCDeclarationStatement -> declarationGetReplacedElement(element)
                            is OCAssignmentExpression -> assignmentGetReplacedElement(element)
                            else -> error("Invalid element type")
                        }

                        // int a = 2 + 3 -> int a = add_probe(x, 2 + 3)
                        val expr = OCElementFactory
                            .callExpression("add_probe", listOf(i.toString(), right.text), element)
                        right.replace(expr)
                    }

                    probeService.runner.executeFile(clone)
                }
            }
        }
    }

    private fun declarationGetReplacedElement(element: OCDeclarationStatement): PsiElement {
        return element.declaration.declarators.firstOrNull()?.initializer!! // Cannot be null, we tested this before
    }

    private fun assignmentGetReplacedElement(element: OCAssignmentExpression): PsiElement {
        return element.sourceExpression!! // Cannot be null, we tested this before
    }

    private fun findProbeElements(file: PsiFile): List<PsiProbeLocation> {
        val declarations = PsiTreeUtil
            .findChildrenOfType(file, OCDeclarationStatement::class.java)
            .filter { it.declaration.declarators.firstOrNull()?.initializer != null }
            .map { PsiProbeLocation(it.textRange, OCDeclarationStatement::class) }

        val assignments = PsiTreeUtil
            .findChildrenOfType(file, OCAssignmentExpression::class.java)
            .filter { it.sourceExpression != null }
            .map { PsiProbeLocation(it.textRange, OCAssignmentExpression::class) }

        return declarations + assignments
    }
}

data class PsiProbeLocation(
    val range: TextRange,
    val parent: KClass<out PsiElement>)
