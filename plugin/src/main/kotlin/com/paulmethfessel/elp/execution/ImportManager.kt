package com.paulmethfessel.elp.execution

import com.paulmethfessel.elp.model.Example
import com.paulmethfessel.elp.util.childrenOfType
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.jetbrains.cidr.lang.psi.OCIncludeDirective

/**
 * Example files need to have same imports as original files, so this copies them
 */
object ImportManager {
    fun update(example: Example) {
        val parentImports = example.referencedFiles
            .flatMap { it.childrenOfType<OCIncludeDirective>() }
            .map { it.referenceText }
            .distinct()
        val ownImports = example.ownFile
            .childrenOfType<OCIncludeDirective>()
            .map { it.referenceText }
        val missingImports = parentImports - ownImports.toSet()

        runWriteAction {
            executeCommand {
                val doc = example.ownDocument
                for (import in missingImports) {
                    doc.insertString(0, "#include \"$import\"\n")
                }
            }
        }
    }
}
