package com.elp.events

import com.elp.execution.ImportManager
import com.elp.execution.CodeExecutionManager
import com.elp.services.exampleService
import com.elp.util.getPsiFile
import com.elp.util.openProject
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener

/**
 * Runs the CodeExecutionManager on safe
 */
class FileChangeListener : FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        val project = openProject ?: return
        val file = document.getPsiFile(project) ?: return

        CodeExecutionManager.run(project)

//        probeService.showLoading(file)

        val example = project.exampleService.activeExample ?: return
        if (file !in example.referencedFiles) return
        ImportManager.update(example)
    }
}

