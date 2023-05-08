package com.elp

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager

class FileChangeListener: FileDocumentManagerListener {
    init {
        FileOpenListener.register()
    }

    override fun beforeDocumentSaving(document: Document) {
        val project = ProjectManager.getInstance().openProjects.first()
        val root  = ModuleManager.getInstance(project).modules.firstOrNull()?.rootManager?.contentRoots?.firstOrNull() ?: return
        val src = root.children.find { it.name == "src" } ?: return
        val files = src.recursiveChildren
        val psiFiles = files.map { PsiManager.getInstance(project).findFile(it) ?: error("No psi for file ${it.path}") }

        FileAnalyzer.analyze(psiFiles)
    }
}

private val VirtualFile.recursiveChildren get(): List<VirtualFile> = children.flatMap { it.recursiveChildren + it }
