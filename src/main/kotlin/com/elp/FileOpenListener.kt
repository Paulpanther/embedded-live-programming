package com.elp

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentListener
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class FileOpenListener: PsiDocumentListener {
    companion object {
        fun register() {
            val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return
            project.messageBus.connect().subscribe(PsiDocumentListener.TOPIC, FileOpenListener())
        }
    }

    override fun documentCreated(
        document: Document,
        psiFile: PsiFile?,
        project: Project
    ) {}

    override fun fileCreated(file: PsiFile, document: Document) {
        FileAnalyzer.analyze(file)
    }
}
