package com.elp

import com.elp.ui.Replacement
import com.elp.util.ExampleNotification
import com.elp.util.UpdateListeners
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.symbols.cpp.OCStructSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol


@Service
class ExampleService(
    private val project: Project
) {
    val examples = mutableListOf(Example(project))
    var activeExample: Example? = null
        set(value) {
            if (field == value) return

            activeExample?.hide()
            value?.show()
            field = value
        }

    fun addNewExample(): Example {
        val newExample = Example(project)
        examples += newExample
        activeExample = newExample
        return newExample
    }

    fun getActiveExampleOrShowError(error: String, consumer: (example: Example) -> Unit) {
        val example = activeExample
        if (example == null) {
            ExampleNotification.notifyError(project, error)
        } else {
            consumer(example)
        }
    }
}

val Project.exampleService get() = this.service<ExampleService>()

class Example(
    private val project: Project,
) {
    val replacements = mutableListOf<Replacement>()
    val file: PsiFile = PsiFileFactory.getInstance(project).createFileFromText(OCLanguage.getInstance(), "void setup() {\n\t\n}\n\nvoid loop() {\n\t\n}")
    val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: error("")

    var activeClass: OCStructSymbol? = null
        set(value) {
            field = value
            onActiveClassChange.call()
        }

    val onReplacementsChange = UpdateListeners()
    val onActiveClassChange = UpdateListeners()

    fun show() {
        replacements.forEach { it.show() }
    }

    fun hide() {
        replacements.forEach { it.hide() }
    }

    fun addReplacement(replacement: Replacement) {
        replacements += replacement
        onReplacementsChange.call()
    }

    fun removeReplacement(replacement: Replacement) {
        if (replacement !in replacements) return

        replacements -= replacement
        replacement.dispose()
        onReplacementsChange.call()
    }

    fun dispose() {
        replacements.forEach { it.dispose() }
    }
}
