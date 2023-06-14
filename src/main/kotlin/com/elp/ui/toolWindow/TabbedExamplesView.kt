package com.elp.ui.toolWindow

import com.elp.services.Clazz
import com.elp.services.Example
import com.elp.services.exampleService
import com.elp.util.NamingHelper
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.EditorTextField
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.impl.JBEditorTabs
import com.jetbrains.cidr.lang.OCFileType
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class TabbedExamplesView(
    private val project: Project,
    private val clazz: Clazz,
    parentDisposable: Disposable
) {
    private val tabs = JBEditorTabs(project, null, parentDisposable)
    private val wrapper = JPanel(BorderLayout())

    init {
        val group = DefaultActionGroup(object: AnAction("Add Example", "Create a new example for the selected class", AllIcons.General.Add ) {
            override fun actionPerformed(e: AnActionEvent) {
                createAndAddExample()
            }
        })
        val toolbar = ActionManager.getInstance().createActionToolbar("TabbedExampleView", group, false)
        val toolbarComponent = JPanel()
        toolbar.targetComponent = toolbarComponent
        toolbarComponent.add(toolbar.component)
        
        tabs.setSelectionChangeHandler { info, _, doChangeSelection ->
            (info.`object` as? Example)?.makeActive()
            doChangeSelection.run()
        }

        tabs.presentation.apply {
            setEmptyText("There are no examples here yet")
            setTabLabelActionsAutoHide(false)
        }
        for (example in clazz.examples) {
            addTabFor(example)
        }

        wrapper.add(toolbarComponent, BorderLayout.WEST)
        wrapper.add(tabs.component, BorderLayout.CENTER)
    }

    val component = wrapper

    fun makeActive() {
        val selected = tabs.selectedInfo?.`object` as? Example ?: return
        selected.makeActive()
    }

    private fun addTabFor(example: Example, focus: Boolean = false) {
        val document = PsiDocumentManager.getInstance(project).getDocument(example.file) ?: error("Could not get document for new example")
        val editor = EditorTextField(document, project, OCFileType.INSTANCE, false, false)
        val info = TabInfo(editor).apply {
            setObject(example)
            text = example.name
        }
        tabs.addTab(info)
        if (focus) tabs.select(info, true)
    }

    private fun createAndAddExample() {
        val field = JTextField(NamingHelper.nextName("Example ", clazz.examples.map { it.name }))
        val dialog = object: DialogWrapper(project) {
            init {
                title = "Set Example name"
                init()
            }

            override fun createCenterPanel() = field

            override fun doValidate(): ValidationInfo? {
                val nameUsed = clazz.examples.any { it.name == field.text }
                if (nameUsed) return ValidationInfo("Name already in use", field)
                return null
            }
        }
        if (dialog.showAndGet()) {
            clazz.addExample(field.text) {
                addTabFor(it, true)
            }
        }
    }
}
