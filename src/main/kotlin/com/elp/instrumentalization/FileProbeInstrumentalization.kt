package com.elp.instrumentalization

import com.elp.services.probeService
import com.elp.ui.ProbePresentation
import com.elp.util.childAtRangeOfType
import com.elp.util.childrenOfType
import com.intellij.psi.PsiFile
import com.jetbrains.cidr.lang.psi.OCAssignmentExpression
import com.jetbrains.cidr.lang.psi.OCDeclarationStatement
import com.jetbrains.cidr.lang.psi.OCExpression
import com.jetbrains.cidr.lang.psi.OCReturnStatement
import com.jetbrains.cidr.lang.util.OCElementFactory

/**
 * Find Probes
 * Store unfinished ProbePresentations in Service
 * Rewrite file with probes
 **/
object FileProbeInstrumentalization {
    fun run(files: List<PsiFile>) {
        val expressionPerFile = files.associateWith { findProbes(it) }
        val userExpressionsPerFile = files.associateWith { findUserProbes(it) }

        var code = 0
        val probesPerFile = expressionPerFile
            .mapValues { (_, expressions) -> expressions.map { ProbeLocation(it, code++) } }
        val userProbesPerFile = userExpressionsPerFile
            .mapValues { (_, expressions) -> expressions.map { ProbeLocation(it, code++, true) }.toMutableList() }

        for ((file, probes) in probesPerFile) {
            val presentations = probes.map { ProbePresentation(it.code, it.element.textRange, it.userProbe) }
            probeService.probes[file.name] = presentations.toMutableList()
        }
        probeService.foundUserProbes = userExpressionsPerFile
        probeService.requestedUserProbes.clear()

        for ((file, probes) in probesPerFile + userProbesPerFile) {
            instrumentFile(file, probes)
        }
    }

    private fun instrumentFile(file: PsiFile, probes: List<ProbeLocation>): PsiFile {
        for (probe in probes) {
            // int a = 2 + 3 -> int a = add_probe(x, 2 + 3)
            val expr = OCElementFactory
                .callExpression("add_probe", listOf(probe.code.toString(), probe.element.text), probe.element)
            probe.element.replace(expr)
        }
        return file
    }

    private fun findProbes(file: PsiFile): List<OCExpression> {
        val declarations = file
            .childrenOfType<OCDeclarationStatement>()
            .mapNotNull { it.declaration.declarators.firstOrNull()?.initializer }

        val assignments = file
            .childrenOfType<OCAssignmentExpression>()
            .mapNotNull { it.sourceExpression }

        val returns = file
            .childrenOfType<OCReturnStatement>()
            .mapNotNull { it.expression }

        return declarations + assignments + returns
    }

    private fun findUserProbes(file: PsiFile): List<OCExpression> {
        val ranges = probeService.requestedUserProbes[file.virtualFile] ?: return listOf()
        return ranges.mapNotNull { file.childAtRangeOfType<OCExpression>(it) }
    }
}

private data class ProbeLocation(
    val element: OCExpression,
    val code: Int,
    val userProbe: Boolean = false)
