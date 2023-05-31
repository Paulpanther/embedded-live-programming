package com.elp.ui

import com.elp.dumbActionButton
import com.elp.getAllOpenOpenFiles
import com.elp.panel
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.cidr.lang.psi.OCImplementation
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class ExampleToolWindowClassesView(
    private val project: Project,
    private val onClassSelected: (clazz: PsiFile) -> Unit,
) : JPanel() {
    private val model = project.getAllOpenOpenFiles() ?: listOf()

    init {
        layout = BorderLayout()
        add(JBScrollPane(panel {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            for (file in model) {
                add(renderClass(file))
            }

            add(Box.createVerticalGlue())
        }), BorderLayout.CENTER)
    }

    private fun renderClass(file: PsiFile) = panel {
        layout = BorderLayout()
        add(JLabel(file.clazz?.name ?: file.name), BorderLayout.CENTER)

        dumbActionButton("Select as Example", "Select this class as Entrypoint", AllIcons.Actions.Execute) {
            onClassSelected(file)
        }.also { add(it, BorderLayout.LINE_END) }

//        dumbActionButton("Select as Example", "Mock or disable this Class", AllIcons.RunConfigurations.ToolbarSkipped) {
//        }.also { add(it, BorderLayout.LINE_END) }
    }
}

private val PsiFile.clazz get() = PsiTreeUtil.findChildOfType(this, OCImplementation::class.java)
