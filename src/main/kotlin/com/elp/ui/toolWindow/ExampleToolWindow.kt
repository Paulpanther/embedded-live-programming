package com.elp.ui.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class ExampleToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.contentManager.apply {
            val content = factory.createContent(ClassesView(project).component, null, false)
            content.isCloseable = false
            addContent(content)
        }
    }
}
