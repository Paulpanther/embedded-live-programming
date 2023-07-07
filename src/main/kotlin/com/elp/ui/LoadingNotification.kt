package com.elp.ui

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.ui.EditorNotificationPanel

class LoadingNotification private constructor(
    project: Project,
    private val editor: FileEditor,
) {
    private val manager = FileEditorManager.getInstance(project)
    private val panel = EditorNotificationPanel(editor).apply {
        text("Reloading Code")
        manager.addTopComponent(editor, this)
    }

    companion object {
        fun create(file: PsiFile): LoadingNotification? {
            val manager = FileEditorManager.getInstance(file.project)
            val editor = manager.getSelectedEditor(file.virtualFile) ?: return null
            return LoadingNotification(file.project, editor)
        }
    }

    fun hide() {
        invokeLater {
            manager.removeTopComponent(editor, panel)
        }
    }
}
