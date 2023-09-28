package com.paulmethfessel.elp.execution

import com.paulmethfessel.elp.actions.isValidProbeType
import com.paulmethfessel.elp.services.probeService
import com.paulmethfessel.elp.ui.ProbePresentation
import com.paulmethfessel.elp.util.*
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.cidr.lang.psi.OCExpression
import com.jetbrains.cidr.lang.util.OCElementFactory

/**
 * Find Probes
 * Store unfinished ProbePresentations in Service
 * Rewrite file with probes
 **/
object FileProbeInjection {
    fun run(clonedFiles: List<ClonedFile>) {
        probeService.probes.clear()
        val code = Ref(0)
        val probesPerFile = findAndRegisterProbes(clonedFiles, code)
        val userProbesPerFile = findAndRegisterUserProbes(clonedFiles, code)

        for (file in clonedFiles) {
            val probes = (probesPerFile[file] ?: listOf()) + (userProbesPerFile[file] ?: listOf())
            val presentations = probes.map { ProbePresentation(it.code, it.element.textRange, it.userProbe) }

            // Append previous user probes that were collected in past
            probeService.probes.appendValue(file.name, probeService.userProbes[file.name] ?: listOf())

            // Store user probes found now
            probeService.userProbes.appendValue(file.name, presentations.filter { it.isUserProbe })

            // Add all new probes
            probeService.probes.appendValue(file.name, presentations)
        }
        probeService.requestedUserProbes.clear()

        for (file in clonedFiles) {
            val probes = (probesPerFile[file] ?: listOf()) + (userProbesPerFile[file] ?: listOf())
            instrumentFile(probes)
        }
    }

    private fun findAndRegisterProbes(files: List<ClonedFile>, code: Ref<Int>): Map<ClonedFile, List<ProbeLocation>> {
        val expressionPerFile = files.associateWith { findProbes(it) }
        val probesPerFile = expressionPerFile
            .mapValues { (_, expressions) -> expressions.map {
                ProbeLocation(it, code.getAndPostInc()) } }
        return probesPerFile
    }

    private fun findAndRegisterUserProbes(files: List<PsiFile>, code: Ref<Int>): Map<PsiFile, List<ProbeLocation>> {
        val expressionPerFile = files.associateWith { findUserProbes(it) }
        val probesPerFile = expressionPerFile
            .mapValues { (_, expressions) -> expressions.map {
                ProbeLocation(it, code.getAndPostInc(), true) } }
        return probesPerFile
    }

    private fun instrumentFile(probes: List<ProbeLocation>) {
        for (probe in probes) {
            // int a = 2 + 3 -> int a = add_probe(x, 2 + 3)
            val expr = OCElementFactory
                .callExpression("add_probe", listOf(probe.code.toString(), probe.element.text), probe.element)
            probe.element.replace(expr)
        }
    }

    private fun findProbes(file: ClonedFile): List<OCExpression> {
        val doc = file.document ?: return listOf()
        return file
            .childrenOfType<OCExpression>()
            .filter { (file.originalElement(it) as? OCExpression)?.isValidProbeType == true }
            .groupBy { doc.getLineNumber(it.startOffset) }
            .mapNotNull { (_, expressions) -> expressions.maxByOrNull { it.textRange.length } }

//        val declarations = file
//            .childrenOfType<OCDeclarationStatement>()
//            .mapNotNull { it.declaration.declarators.firstOrNull()?.initializer }
//
//        val assignments = file
//            .childrenOfType<OCAssignmentExpression>()
//            .mapNotNull { it.sourceExpression }
//
//        val returns = file
//            .childrenOfType<OCReturnStatement>()
//            .mapNotNull { it.expression }
//
//        return declarations + assignments + returns
    }

    private fun findUserProbes(file: PsiFile): List<OCExpression> {
        val ranges = probeService.requestedUserProbes[file.name] ?: return listOf()
        return ranges.mapNotNull { file.childAtRangeOfType(it) }
    }
}

private data class ProbeLocation(
    val element: OCExpression,
    val code: Int,
    val userProbe: Boolean = false)
