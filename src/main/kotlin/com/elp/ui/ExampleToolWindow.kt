package com.elp.ui

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.OnePixelSplitter
import com.jetbrains.cidr.lang.OCFileType
import com.jetbrains.cidr.lang.OCLanguage

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
                val file: PsiFile = PsiFileFactory.getInstance(project).createFileFromText(
                    OCLanguage.getInstance(),
                    "class Main {\n\tvoid setup() {\n\t\t\n\t}\n\n\tvoid loop() {\n\t\t\n\t}\n};")
                val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: error("")

                val editor =
                    EditorFactory.getInstance().createEditor(document, project, EditorKind.MAIN_EDITOR) ?: return
                (editor as EditorEx).highlighter =
                    EditorHighlighterFactory.getInstance().createEditorHighlighter(project, OCFileType.INSTANCE)

                val analyzer = DaemonCodeAnalyzer.getInstance(project)
                println("Highlighting Available: " + analyzer.isHighlightingAvailable(file))
                println("AutoHints Available: " + analyzer.isAutohintsAvailable(file))

//                document.addDocumentListener(object : DocumentListener {
//                    override fun documentChanged(event: DocumentEvent) {
//                        val highlighter = TextEditorBackgroundHighlighter(project, editor)
//                        val passes = highlighter.createPassesForEditor()
//
//                        application.runReadAction {
//                            val indicator = DaemonProgressIndicator()
//                            ProgressManager.getInstance().runProcess({
//                                passes.forEach { it.collectInformation(indicator) }
//                            }, indicator)
//
//                            application.invokeLater {
//                                passes.forEach { it.applyInformationToEditor() }
//                            }
//                        }
//                    }
//                })

                rightSplitter.firstComponent = editor.component
            }

            leftSplitter.firstComponent = ExampleToolWindowClassesView(project, ::setFile)
            leftSplitter.secondComponent = rightSplitter

            val content = factory.createContent(leftSplitter, "Example", false)
            addContent(content)
        }
    }
}
