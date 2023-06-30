package com.elp.events

import com.elp.instrumentalization.ImportManager
import com.elp.instrumentalization.InstrumentalizationManager
import com.elp.services.exampleService
import com.elp.util.getPsiFile
import com.elp.util.openProject
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener

class FileChangeListener : FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        val project = openProject ?: return
        InstrumentalizationManager.run(project)

        val file = document.getPsiFile(project) ?: return
        val example = project.exampleService.activeExample ?: return
        if (file !in example.referencedFiles) return
        ImportManager.update(example)
    }
}

