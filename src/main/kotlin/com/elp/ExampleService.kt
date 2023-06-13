package com.elp

import com.elp.ui.Replacement
import com.elp.util.ExampleNotification
import com.elp.util.UpdateListeners
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.psi.OCImplementation
import com.jetbrains.cidr.lang.psi.impl.OCStructImpl
import com.jetbrains.cidr.lang.symbols.cpp.OCStructSymbol

typealias ExampleClass = OCImplementation

@Service
class ExampleService(
    private val project: Project
) {
    val exampleDirectory = createExampleModule()
    val examples = mutableListOf<Example>()
    var activeExample: Example? = null
        set(value) {
            if (field == value) return

            activeExample?.hide()
            value?.show()
            field = value
        }

    fun examplesOfClass(clazz: ExampleClass): List<Example> {
        return examples.filter { it.clazz == clazz }
    }

    fun addNewExample(file: PsiFile): Example {
        val clazz = PsiTreeUtil.findChildOfType(file, ExampleClass::class.java) ?: error("Could not find class in file")
        val exampleFile = createExampleFile(clazz) ?: error("Could not create file")
        val newExample = Example(project, clazz, exampleFile, "Example")
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

    private fun createExampleModule(): VirtualFile {
        val root = ModuleManager.getInstance(project)
            .modules.firstOrNull()?.rootManager?.contentRoots?.firstOrNull() ?: error("Could not find content root")
        val existingExampleDir = root.findChild("examples")
        if (existingExampleDir != null && !existingExampleDir.isDirectory) {
            error("Examples directory cannot be created.")
        }

        return existingExampleDir ?: root.createChildDirectory(this, "examples")
    }

    private fun createExampleFile(clazz: ExampleClass): PsiFile? {
        val name = "${clazz.name}.example.h"
        val file = exampleDirectory.createChildData(this, name)
        file.getOutputStream(this).bufferedWriter().write("class Main {};")
        return file.getPsiFile(project)
    }
}

val Project.exampleService get() = this.service<ExampleService>()

class Example(
    private val project: Project,
    val clazz: ExampleClass,
    val file: PsiFile,
    var name: String,
) {
    val replacements = mutableListOf<Replacement>()

    val onReplacementsChange = UpdateListeners()

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
