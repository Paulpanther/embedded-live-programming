package com.elp.services

import com.elp.instrumentalization.ImportManager
import com.elp.model.Example
import com.elp.util.document
import com.elp.util.ExampleNotification
import com.elp.util.NamingHelper
import com.elp.util.UpdateListeners
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.rd.util.getOrCreate

val exampleKey = Key.create<Example>("ELP_EXAMPLE")

@Service
class ExampleService(
    private val project: Project
) {
    val exampleDirectory = createExampleModule()

    val onExamplesChanged = UpdateListeners()
    private val classToExamples = mutableMapOf<Clazz, MutableList<Example>>()
    val examples get() = classToExamples.values.flatten()

    val onActiveExampleChanged = UpdateListeners()
    var activeExample: Example? = null
        set(value) {
            if (field == value) return

            field = value
            onActiveExampleChanged.call()
        }

    fun examplesForClass(clazz: Clazz): MutableList<Example> {
        return classToExamples.getOrCreate(clazz) { mutableListOf() }
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
            ImportManager.update(example)
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
            val dir = PsiManager.getInstance(project).findDirectory(exampleDirectory) ?: return@runWriteAction
            val file = PsiFileFactory.getInstance(project).createFileFromText(name, OCLanguage.getInstance(), "class ${clazz.name} {};")
            callback(dir.add(file).containingFile.virtualFile)
        }
    }
}

val Project.exampleService get() = this.service<ExampleService>()

val PsiFile.example get() = document?.getUserData(exampleKey)
val PsiFile.isExample get() = example != null
