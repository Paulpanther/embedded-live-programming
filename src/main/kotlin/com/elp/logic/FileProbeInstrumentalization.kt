package com.elp.logic

import com.elp.probeService
import com.elp.ui.ProbePresentation
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
import com.jetbrains.cidr.lang.psi.OCCallExpression
import com.jetbrains.cidr.lang.psi.OCDeclarationStatement
import com.jetbrains.cidr.lang.util.OCElementFactory
import kotlin.reflect.KClass

/**
 * Find Probes
 * Store unfinished ProbePresentations in Service
 * Rewrite copy of file with probes
 **/
object FileProbeInstrumentalization {
    fun run(files: List<PsiFile>, consumer: (files: Map<PsiFile, PsiFile>) -> Unit) {
        val probesPerFile = extractProbes(files)

        for ((file, probeLocations) in probesPerFile) {
            val probes = probeLocations.map { ProbePresentation(it.code, it.range) }
            probeService.probes[file.virtualFile.path] = probes.toMutableList()
        }

        invokeLater {
            runWriteAction {
                executeCommand {
                    val clones = probesPerFile
                        .map { (file, probes) -> file to generateInstrumentedFile(file, probes) }
                        .toMap()
                    consumer(clones)
                }
            }
        }
    }

    private fun extractProbes(files: List<PsiFile>): Map<PsiFile, List<PsiProbeLocation>> {
        val probePerFile = mutableMapOf<PsiFile, List<PsiProbeLocation>>()

        var i = 0
        for (file in files) {
            probePerFile[file] = findProbeElements(file).onEach { it.code = i++ }
        }

        return probePerFile
    }

    private fun generateInstrumentedFile(file: PsiFile, psiProbes: List<PsiProbeLocation>): PsiFile {
        val clone = PsiFileFactory
            .getInstance(file.project)
            .createFileFromText(file.name, OCLanguage.getInstance(), file.text)

        // Has to be done before loop, since loop changes offsets
        val clonedProbeElements = psiProbes
            .associate {
                it.code to (clone.findElementAt(it.range.startOffset)?.parentOfTypes(it.parent)
                    ?: error("Could not find Psi Element of Probe"))
            }

        for ((code, element) in clonedProbeElements) {
            val right = when (element) {
                is OCDeclarationStatement -> declarationGetReplacedElement(element)
                is OCAssignmentExpression -> assignmentGetReplacedElement(element)
                else -> error("Invalid element type")
            }

            // int a = 2 + 3 -> int a = add_probe(x, 2 + 3)
            val expr = OCElementFactory
                .callExpression("add_probe", listOf(code.toString(), right.text), element)
            right.replace(expr)
        }
        return clone
    }

    private fun declarationGetReplacedElement(element: OCDeclarationStatement): PsiElement {
        return element.declaration.declarators.firstOrNull()?.initializer!! // Cannot be null, we tested this before
    }

    private fun assignmentGetReplacedElement(element: OCAssignmentExpression): PsiElement {
        return element.sourceExpression!! // Cannot be null, we tested this before
    }

    private fun argumentsGetReplacedElements(element: OCCallExpression): List<PsiElement> {
        return element.arguments
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

//        val arguments = PsiTreeUtil
//            .findChildrenOfType(file, OCCallExpression::class.java)
//            .filter { it.arguments.isNotEmpty() }
//            .flatMap { it.arguments.map { arg -> PsiProbeLocation(arg.textRange, OCCallExpression::class) } }

        return declarations + assignments //+ arguments
    }
}

data class PsiProbeLocation(
    val range: TextRange,
    val parent: KClass<out PsiElement>,
    var code: Int = 0
)
