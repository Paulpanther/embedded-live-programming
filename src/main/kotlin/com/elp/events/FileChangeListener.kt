package com.elp.events

import com.elp.instrumentalization.ImportManager
import com.elp.instrumentalization.InstrumentalizationManager
import com.elp.services.exampleService
import com.elp.services.probeService
import com.elp.util.childrenOfType
import com.elp.util.error
import com.elp.util.getPsiFile
import com.elp.util.openProject
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiErrorElement
import com.intellij.ui.EditorNotificationPanel

class FileChangeListener : FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        val project = openProject ?: return
        val file = document.getPsiFile(project) ?: return

        InstrumentalizationManager.run(project)

//        probeService.showLoading(file)

        val example = project.exampleService.activeExample ?: return
        if (file !in example.referencedFiles) return
        ImportManager.update(example)
    }
}

