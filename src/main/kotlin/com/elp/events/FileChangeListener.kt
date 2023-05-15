package com.elp.events

import com.elp.exampleService
import com.elp.logic.FileAnalyzer
import com.elp.logic.FileExampleInstrumentalization
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

class FileChangeListener: FileDocumentManagerListener {
    init {
        FileOpenListener.register()
    }

    override fun beforeDocumentSaving(document: Document) {
        val project = ProjectManager.getInstance().openProjects.first()
        val root  = ModuleManager.getInstance(project).modules.firstOrNull()?.rootManager?.contentRoots?.firstOrNull() ?: return
        val src = root.children.find { it.name == "src" } ?: return
        val include = root.children.find { it.name == "include" } ?: return
        val files = src.recursiveChildren + include.recursiveChildren
        val psiFiles = files.map { PsiManager.getInstance(project).findFile(it) ?: error("No psi for file ${it.path}") }

        FileAnalyzer.analyze(psiFiles)

        project.exampleService.activeExample?.let { example ->
            val file = psiFiles.find { it.virtualFile == example.activeClass?.containingFile }
            if (file != null) {
                FileExampleInstrumentalization.run(example, file)
            }
        }
    }
}

private val VirtualFile.recursiveChildren get(): List<VirtualFile> = children.flatMap { it.recursiveChildren + it }
