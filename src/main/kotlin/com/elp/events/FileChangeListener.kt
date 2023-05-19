package com.elp.events

import com.elp.exampleService
import com.elp.logic.FileProbeInstrumentalization
import com.elp.logic.FileExampleInstrumentalization
import com.elp.logic.error
import com.elp.probeService
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.util.OCElementFactory

class FileChangeListener : FileDocumentManagerListener {
    init {
        FileOpenListener.register()
    }

    override fun beforeDocumentSaving(document: Document) {
        val project = ProjectManager.getInstance().openProjects.first()
        val root =
            ModuleManager.getInstance(project).modules.firstOrNull()?.rootManager?.contentRoots?.firstOrNull() ?: return
        val src = root.children.find { it.name == "src" } ?: return
        val include = root.children.find { it.name == "include" } ?: return
        val files = src.recursiveChildren + include.recursiveChildren.filter { it.name != "code.h" }  // TODO remove the filter
        val psiFiles = files.map { PsiManager.getInstance(project).findFile(it) ?: error("No psi for file ${it.path}") }

        FileProbeInstrumentalization.run(psiFiles) { oldToNewFiles ->
            buildRunnableProject(project, oldToNewFiles) {
                probeService.runner.executeFiles(it)
            }
        }
    }

    private fun buildRunnableProject(
        project: Project,
        oldToNewFiles: Map<PsiFile, PsiFile>,
        finishedFilesConsumer: (files: List<PsiFile>) -> Unit
    ) {
        val oldFiles = oldToNewFiles.keys.toList()
        val example = project.exampleService.activeExample
            ?: return project.error("Create an example to run the project")

        val file = oldFiles.find { it.virtualFile == example.activeClass?.containingFile }
            ?: return project.error("Active Example has no valid class assigned to it")
        val newFile = oldToNewFiles[file]
            ?: return project.error("Could not generate instrumentalized classes")

        FileExampleInstrumentalization.run(example, newFile) { exampleFile, exampleClassName ->
            // Replace file in list
            val finishedFiles = oldToNewFiles.map { (oldFile, newFile) ->
                if (oldFile == file) exampleFile else newFile
            }
            finishedFilesConsumer(finishedFiles + createRunnerFile(project, exampleClassName, exampleFile.name))
        }
    }

    private fun createRunnerFile(project: Project, exampleClassName: String, exampleFileName: String) =
        PsiFileFactory.getInstance(project).createFileFromText(OCLanguage.getInstance(), """
            #include "$exampleFileName"
            #include "code.h"
            
            $exampleClassName* ____m = nullptr;
            
            void setup() {
                ____m = $exampleClassName::setup();
            }
            
            void loop() {
                ____m->loop();
            }
        """.trimIndent())
            .apply { name = "________main.cpp" }
}

private val VirtualFile.recursiveChildren get(): List<VirtualFile> = children.flatMap { it.recursiveChildren + it }
