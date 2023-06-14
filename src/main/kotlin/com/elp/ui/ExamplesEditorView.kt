package com.elp.ui

import com.elp.ExampleClass
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.impl.JBEditorTabs

class ExamplesEditorView(
    project: Project,
    private val clazz: ExampleClass?,
    parent: Disposable
): JBEditorTabs(project, null, parent) {
    init {
        addTab(TabInfo(EditorTextField("Hello World")).also { it.text = "Example 1" })
        addTab(TabInfo(EditorTextField("Hello World 2")).also { it.text = "Example 2" })
    }
//    private val tabs = createTabs()
//
//    private fun createTabs(): List<ExampleTab> {
//        return project.exampleService
//            .examplesOfClass(clazz)
//            .map { ExampleTab(it) }
//    }
}
