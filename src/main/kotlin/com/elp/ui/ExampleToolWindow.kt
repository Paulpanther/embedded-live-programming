package com.elp.ui

import com.elp.ExampleClass
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.OnePixelSplitter
import com.jetbrains.rd.util.getOrCreate

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
            val editorPerClass = mutableMapOf<ExampleClass, ExamplesEditorView>()

            fun setClazz(clazz: ExampleClass) {
                leftSplitter.secondComponent = editorPerClass
                    .getOrCreate(clazz) { ExamplesEditorView(project, clazz, this) }
            }

            leftSplitter.firstComponent = ExampleToolWindowClassesView(project, ::setClazz)
            leftSplitter.secondComponent = ExamplesEditorView(project, null, this)

            val content = factory.createContent(leftSplitter, "Examples", false)
            addContent(content)
        }
    }
}
