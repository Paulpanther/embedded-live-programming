package com.elp.ui

import com.elp.Example
import com.elp.exampleService
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.jetbrains.cidr.lang.OCFileType

class ExampleToolWindowFactory : ToolWindowFactory {
    private lateinit var toolWindow: ToolWindow
    private lateinit var project: Project

    private val tabs = mutableListOf<ExampleTab>()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        this.toolWindow = toolWindow
        this.project = project

        // When a different tab is selected also change the active example
        toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                if (event.operation == ContentManagerEvent.ContentOperation.add) {
                    val example = exampleForContent(event.content) ?: return
                    project.exampleService.activeExample = example
                } else if (event.operation == ContentManagerEvent.ContentOperation.remove) {
                    project.exampleService.activeExample = null
                }
            }
        })

        // Create add example button in toolbar
        (toolWindow as? ToolWindowEx)?.setTabActions(object :
            AnAction("Add Example", "Adds a new editor with and example attached", AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {
                val example = project.exampleService.addNewExample()
                createContent(example)
            }
        })
    }

    private fun createContent(example: Example) {
        val file = example.activeClass?.getContainingPsiFile(project) ?: return
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

        val editor = EditorFactory.getInstance().createEditor(document, project, EditorKind.MAIN_EDITOR) ?: return
        (editor as EditorEx).highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, OCFileType.INSTANCE)
//        example.document.addDocumentListener(object : DocumentListener {
//            override fun documentChanged(event: DocumentEvent) {
//        val highlighter = TextEditorBackgroundHighlighter(project, editor)
//        val passes = highlighter.createPassesForEditor()
//
//        val indicator = DaemonProgressIndicator()
//        ProgressManager.getInstance().runProcess({
//            passes.forEach { it.collectInformation(indicator) }
//        }, indicator)
//
//        application.invokeLater {
//            passes.forEach { it.applyInformationToEditor() }
//        }
//            }
//        })

        toolWindow.contentManager.apply {
            val splitter = OnePixelSplitter()
            splitter.firstComponent = editor.component
            splitter.secondComponent = ExampleToolWindowSettingsView(project, example)

            val content = factory.createContent(splitter, "Example", false)
            tabs += ExampleTab(example, content)
            addContent(content)
        }
    }

    private fun contentForExample(example: Example) = tabs.find { it.example == example }?.content
    private fun exampleForContent(content: Content) = tabs.find { it.content == content }?.example
}

private class ExampleTab(
    val example: Example,
    val content: Content
)
