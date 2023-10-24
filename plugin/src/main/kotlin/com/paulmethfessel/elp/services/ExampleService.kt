package com.paulmethfessel.elp.services

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
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
import com.intellij.psi.codeStyle.CodeStyleManager
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.rd.util.getOrCreate
import com.paulmethfessel.elp.execution.CodeExecutionManager
import com.paulmethfessel.elp.execution.ImportManager
import com.paulmethfessel.elp.model.Example
import com.paulmethfessel.elp.util.NamingHelper
import com.paulmethfessel.elp.util.UpdateListeners
import com.paulmethfessel.elp.util.document

val exampleKey = Key.create<Example>("ELP_EXAMPLE")

@Service(Service.Level.PROJECT)
class ExampleService(
    val project: Project
) {
    private val exampleDirectory = createExampleModule()
    private var hasLoadedExample = false

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

    init {
        CodeExecutionManager.registerOnActiveExampleChange(this)
        project.classService.classListener.register(::loadExamples)
    }

    fun examplesForClass(clazz: Clazz): MutableList<Example> {
        return classToExamples.getOrCreate(clazz) { mutableListOf() }
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

    fun deleteExample(example: Example) {
        // TODO add delete handler that removes Tab in TabbedExampleView
        probeService.runner.stop()
        activeExample = null
        classToExamples[example.parentClazz]?.remove(example)
        runWriteAction {
            example.ownVirtualFile.delete(this)
        }
    }

    private fun loadExamples() {
        if (hasLoadedExample) return
        hasLoadedExample = true

        val exampleFiles = exampleDirectory.children.filter { it.name.endsWith(".example.h") }

        for (exampleFile in exampleFiles) {
            val clazzName = exampleFile.name.split(".").dropLast(3).joinToString(".")
            val clazz = project.classService.classes.find { it.name == clazzName } ?: continue
            val name = NamingHelper.nextName("Example", examplesForClass(clazz).map { it.name })
            classToExamples.getOrCreate(clazz) { mutableListOf() } += Example(project, clazz, exampleFile, name)
        }
        onExamplesChanged.call()
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

    private fun createExampleFile(clazz: Clazz, callback: (VirtualFile?) -> Unit) {
        val name = NamingHelper.nextName(clazz.name ?: "example", examplesForClass(clazz).map { it.name }, ".") + ".example.h"
        val dir = PsiManager.getInstance(project).findDirectory(exampleDirectory) ?: return callback(null)
        runWriteAction {
            executeCommand {
                val file = PsiFileFactory.getInstance(project).createFileFromText(name, OCLanguage.getInstance(), "class ${clazz.name} {};")
                CodeStyleManager.getInstance(project).reformat(file)
                callback(dir.add(file).containingFile.virtualFile)
            }
        }
    }
}

val Project.exampleService get() = this.service<ExampleService>()

val PsiFile.example get() = document?.getUserData(exampleKey)
val PsiFile.isExample get() = example != null
