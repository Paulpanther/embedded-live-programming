package com.elp.ui

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorTextField
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.JBUI
import com.jetbrains.cidr.lang.OCFileType
import com.jetbrains.cidr.lang.OCLanguage
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JPanel

class ExampleToolWindowFactory : ToolWindowFactory {
    private lateinit var toolWindow: ToolWindow
    private lateinit var project: Project

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        this.toolWindow = toolWindow
        this.project = project

        createContent()
    }

    private fun createContent() {

        toolWindow.contentManager.apply {
            val leftSplitter = OnePixelSplitter()
            val rightSplitter = OnePixelSplitter()

            fun setFile(psiFile: PsiFile) {
                rightSplitter.firstComponent = createEditor(psiFile)
            }

            leftSplitter.firstComponent = ExampleToolWindowClassesView(project, ::setFile)
            leftSplitter.secondComponent = rightSplitter

            val content = factory.createContent(leftSplitter, "Example", false)
            addContent(content)
        }
    }

    private fun createEditor(original: PsiFile): JComponent {
        val file: PsiFile = PsiFileFactory.getInstance(project).createFileFromText(
            "main.example.cpp",
            OCLanguage.getInstance(),
            "class Main {\n\tvoid setup() {\n\t\t\n\t}\n\n\tvoid loop() {\n\t\t\n\t}\n};",
            true, false, true,
            original.virtualFile)
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: error("")
        FileDocumentManager.getInstance().getDocument(file.virtualFile)

        val editor = EditorTextField(document, project, OCFileType.INSTANCE, false, false)
        return editor.component

//        val editor = EditorFactory.getInstance().createEditor(document, project, EditorKind.MAIN_EDITOR) ?: error("")
//        (editor as EditorEx).highlighter =
//            EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file.virtualFile)
//
//        return JBUI.Panels.simplePanel(editor.component).also { panel ->
//            DataManager.registerDataProvider(panel) { dataId ->
//                FileEditorManager
//                    .getInstance(project)
//                    .getData(dataId, editor, editor.caretModel.currentCaret)
//                    ?.let { return@registerDataProvider it }
//
//                if (CommonDataKeys.EDITOR.`is`(dataId)) return@registerDataProvider editor
//                if (CommonDataKeys.VIRTUAL_FILE.`is`(dataId) && file.virtualFile.isValid)
//                    return@registerDataProvider file.virtualFile
//                null
//            }
//        }
    }
}
