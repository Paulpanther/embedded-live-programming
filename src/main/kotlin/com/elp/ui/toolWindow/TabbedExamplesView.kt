package com.elp.ui.toolWindow

import com.elp.actions.showCreateExampleDialog
import com.elp.model.Example
import com.elp.services.classService
import com.elp.services.exampleService
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.impl.JBEditorTabs
import java.awt.BorderLayout
import javax.swing.JPanel

class TabbedExamplesView(
    private val project: Project,
    parentDisposable: Disposable
) {
    private val tabs = JBEditorTabs(project, null, parentDisposable)
    private val wrapper = JPanel(BorderLayout())
    private val examples get() = project.exampleService.examples

    init {
        project.exampleService.onActiveExampleChanged.register(::showActiveExample)
        project.exampleService.onExamplesChanged.register(::updateTabs)

        val group = DefaultActionGroup(object: AnAction("Add Example", "Create a new example for the selected class", AllIcons.General.Add ) {
            override fun actionPerformed(e: AnActionEvent) {
                val clazz = project.classService.currentClass
                runWriteAction {
                    showCreateExampleDialog(project, clazz)
                }
            }
        })

        val toolbar = ActionManager.getInstance().createActionToolbar("TabbedExampleView", group, false)
        val toolbarComponent = JPanel()
        toolbar.targetComponent = toolbarComponent
        toolbarComponent.add(toolbar.component)
        
        tabs.setSelectionChangeHandler { info, _, doChangeSelection ->
            (info.`object` as? Example)?.activate()
            doChangeSelection.run()
        }

        tabs.presentation.apply {
            setEmptyText("There are no examples here yet")
            setTabLabelActionsAutoHide(false)
        }
        for (example in examples) {
            addTabFor(example)
        }

        wrapper.add(toolbarComponent, BorderLayout.WEST)
        wrapper.add(tabs.component, BorderLayout.CENTER)
    }

    val component = wrapper

    fun makeActive(useActive: Boolean) {
        if (useActive) {
            showActiveExample()
        } else {
            val selected = tabs.selectedInfo?.`object` as? Example ?: return
            selected.activate()
        }
    }

    private fun updateTabs() {
        val oldExamples = tabs.tabs.mapNotNull { it.`object` as? Example }.toSet()
        if (oldExamples == examples) return

        tabs.removeAllTabs()
        for (example in examples) {
            addTabFor(example)
        }

        showActiveExample()
    }

    private fun showActiveExample() {
        val active = project.exampleService.activeExample ?: return
        val tab = tabs.tabs.find { it.`object` as? Example == active } ?: return
        tabs.select(tab, true)
        tabs.requestFocusInWindow()
//        active.file.navigate(true)
    }

    private fun addTabFor(example: Example, focus: Boolean = false) {
        val info = TabInfo(JBScrollPane(example.editor)).apply {
            setObject(example)
            val structName = example.parentStruct.name
            if (structName != null) {
                append("$structName: ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
            append(example.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
        tabs.addTab(info)
        if (focus) tabs.select(info, true)
    }

    private fun createAndAddExample() {
    }
}
