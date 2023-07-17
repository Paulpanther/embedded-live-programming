package com.elp.ui.toolWindow

import com.elp.actions.showCreateExampleDialog
import com.elp.model.Example
import com.elp.services.Clazz
import com.elp.services.classService
import com.elp.services.exampleService
import com.elp.util.NamingHelper
import com.elp.util.actionGroup
import com.elp.util.panel
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.impl.JBEditorTabs
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTextField

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

        val group = actionGroup {
            action("Add Example", "Create a new example for the selected class", AllIcons.General.Add) {
                val clazz = project.classService.currentClass
                runWriteAction {
                    showCreateExampleDialog(project, clazz)
                }
            }
            action("Rename Example", "Rename the current example", AllIcons.Actions.Edit) {
                val example = project.exampleService.activeExample ?: return@action
                showRenameExampleDialog(example)
            }
        }

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
            setTabName(this, example)
        }
        tabs.addTab(info)
        if (focus) tabs.select(info, true)
    }

    private fun setTabName(info: TabInfo, example: Example) {
        info.text = ""
        val structName = example.parentStruct.name
        if (structName != null) {
            info.append("$structName: ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
        info.append(example.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    private fun showRenameExampleDialog(example: Example) {
        val field = JTextField(example.name)

        val dialog = object: DialogWrapper(project) {
            init {
                title = "Rename Example"
                init()
            }

            override fun createCenterPanel() = panel {
                layout = BorderLayout()
                add(field, BorderLayout.NORTH)
            }

            override fun getPreferredFocusedComponent() = field

            override fun doValidate(): ValidationInfo? {
                if (field.text.isBlank()) return ValidationInfo("Please enter a valid name", field)
                return null
            }
        }

        invokeLater {
            if (dialog.showAndGet()) {
                example.name = field.text.trim()
                val info = tabs.findInfo(example) ?: return@invokeLater
                setTabName(info, example)
            }
        }
    }
}
