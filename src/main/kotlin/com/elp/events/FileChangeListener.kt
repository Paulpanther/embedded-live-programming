package com.elp.events

import com.elp.instrumentalization.InstrumentalizationManager
import com.elp.util.openProject
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener

class FileChangeListener : FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        val project = openProject ?: return
        InstrumentalizationManager.run(project)
    }
}

