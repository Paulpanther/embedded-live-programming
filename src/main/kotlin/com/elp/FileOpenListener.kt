package com.elp

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentListener
import com.intellij.psi.PsiFile

class FileOpenListener: PsiDocumentListener {
    companion object {
        fun register() {
            (openProject ?: return).messageBus.connect().subscribe(PsiDocumentListener.TOPIC, FileOpenListener())
        }
    }

    override fun documentCreated(
        document: Document,
        psiFile: PsiFile?,
        project: Project
    ) {}

    override fun fileCreated(file: PsiFile, document: Document) {
//        FileAnalyzer.analyze(file)
    }
}
