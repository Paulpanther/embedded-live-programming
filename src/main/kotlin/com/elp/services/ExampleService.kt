package com.elp.services

import com.elp.document
import com.elp.error
import com.elp.getPsiFile
import com.elp.logic.Modification
import com.elp.ui.Replacement
import com.elp.util.ExampleNotification
import com.elp.util.NamingHelper
import com.elp.util.UpdateListeners
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.EditorTextField
import com.jetbrains.cidr.lang.OCFileType
import com.jetbrains.rd.util.getOrCreate

val exampleKey = Key.create<Example>("ELP_EXAMPLE")

@Service
class ExampleService(
    private val project: Project
) {
    val exampleDirectory = createExampleModule()

    val onExamplesChanged = UpdateListeners()
    private val examples = mutableMapOf<Clazz, MutableList<Example>>()

    val onActiveExampleChanged = UpdateListeners()
    var activeExample: Example? = null
        set(value) {
            if (field == value) return

            activeExample?.hide()
            value?.show()
            field = value
            onActiveExampleChanged.call()
        }

    fun examplesForClass(clazz: Clazz): MutableList<Example> {
        return examples.getOrCreate(clazz) { mutableListOf() }
    }

    fun getActiveExampleOrShowError(error: String, consumer: (example: Example) -> Unit) {
        val example = activeExample
        if (example == null) {
            ExampleNotification.notifyError(project, error)
        } else {
            consumer(example)
        }
    }

    fun addExampleToClass(clazz: Clazz, name: String, callback: (Example) -> Unit) {
        createExampleFile(clazz) { file ->
            file ?: error("Could not create example file")
            val example = Example(project, clazz, file, name)
            examplesForClass(clazz) += example
            onExamplesChanged.call()
            activeExample = example
            callback(example)
        }
    }

    private fun createExampleModule(): VirtualFile {
        val root = ModuleManager.getInstance(project)
            .modules.firstOrNull()?.rootManager?.contentRoots?.firstOrNull() ?: error("Could not find content root")
        val existingExampleDir = root.findChild("examples")
        if (existingExampleDir != null && !existingExampleDir.isDirectory) {
            error("Examples directory cannot be created.")
        }

        return (existingExampleDir ?: root.createChildDirectory(this, "examples")).also {
            runWriteAction {
                it.children.forEach { child -> child.delete(this) }
            }
        }
    }

    private fun createExampleFile(clazz: Clazz, callback: (VirtualFile?) -> Unit) {
        runWriteAction {
            val name = NamingHelper.nextName(clazz.name ?: "example", examplesForClass(clazz).map { it.name }) + ".example.h"
            val file = exampleDirectory.createChildData(this, name)
            val doc = file.document ?: error("Could not get document for newly created example")

            executeCommand {
                doc.insertString(0, "class ${clazz.name} {\n\t\n};")
                callback(file)
            }
        }
    }
}

val Project.exampleService get() = this.service<ExampleService>()

class Example(
    private val project: Project,
    val clazz: Clazz,
    val virtualFile: VirtualFile,
    var name: String,
) {
    val replacements = mutableListOf<Replacement>()
    val onReplacementsChange = UpdateListeners()
    val document get() = virtualFile.document ?: error("Could not get document of example")
    val file get() = document.getPsiFile(project) ?: error("Could not get psi file of example")
    var modifications = listOf<Modification>()
    val editor = EditorTextField(document, project, OCFileType.INSTANCE, false, false)

    init {
        document.putUserData(exampleKey, this)
    }

    fun makeActive() {
        project.exampleService.activeExample = this
    }

    fun makeDeactive() {
        project.exampleService.activeExample = null
    }

    fun navigateTo(descriptor: OpenFileDescriptor) {
        editor.component.requestFocusInWindow()
        descriptor.navigateIn(editor.editor ?: return project.error("Could not navigate to element"))
    }

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

    override fun toString() = name
}

val PsiFile.example get() = document?.getUserData(exampleKey)
val PsiFile.isExample get() = example != null
