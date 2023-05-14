package com.elp

import com.elp.ui.Replacement
import com.elp.util.UpdateListeners
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol


@Service
class ExampleService(
    private val project: Project
) {
    val examples = mutableListOf(Example(project))
    var activeExample: Example = examples[0]
        set(value) {
            if (field == value) return

            activeExample.hide()
            value.show()
            field = value
        }

    fun addNewExample(): Example {
        val newExample = Example(project)
        examples += newExample
        activeExample = newExample
        return newExample
    }
}

val Project.exampleService get() = this.service<ExampleService>()

class Example(
    private val project: Project,
) {
    private val replacements = mutableListOf<Replacement>()
    val file: PsiFile = PsiFileFactory.getInstance(project).createFileFromText(OCLanguage.getInstance(), "void setup() {\n\t\n}\n\nvoid loop() {\n\t\n}")
    val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: error("")

    var activeClass: OCClassSymbol? = null
        set(value) {
            field = value
            onActiveClassChange.call()
        }

    private val onReplacementChange = UpdateListeners()
    private val onActiveClassChange = UpdateListeners()

    fun show() {
        replacements.forEach { it.show() }
    }

    fun hide() {
        replacements.forEach { it.hide() }
    }

    fun addReplacement(replacement: Replacement) {
        replacements += replacement
        onReplacementChange.call()
    }

    fun removeReplacement(replacement: Replacement) {
        if (replacement !in replacements) return

        replacements -= replacement
        replacement.dispose()
        onReplacementChange.call()
    }

    fun dispose() {
        replacements.forEach { it.dispose() }
    }
}
