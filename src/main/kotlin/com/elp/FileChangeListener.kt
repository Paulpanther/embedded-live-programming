package com.elp

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiDocumentManager

class FileChangeListener: FileDocumentManagerListener {
    init {
        FileOpenListener.register()
    }

    override fun beforeDocumentSaving(document: Document) {
        val project = ProjectManager.getInstance().openProjects.first()
        val file = PsiDocumentManager.getInstance(project).getPsiFile(document)
        file?.let { FileAnalyzer.analyze(it) }
    }
}
