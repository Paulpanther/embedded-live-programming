package com.elp.ui

import com.elp.dumbActionButton
import com.elp.getAllOpenFiles
import com.elp.panel
import com.elp.services.Clazz
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.cidr.lang.psi.OCImplementation
import java.awt.BorderLayout
import javax.swing.*

class ExampleToolWindowClassesView(
    private val project: Project,
    private val onClassSelected: (clazz: Clazz) -> Unit,
) : JPanel() {
    private val model = project.getAllOpenFiles() ?: listOf()

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
        val clazz = file.clazz

        layout = BorderLayout()
        add(JLabel(clazz?.name ?: file.name), BorderLayout.CENTER)

        dumbActionButton("Select as Example", "Select this class as Entrypoint", AllIcons.Actions.Execute) {
            println(file)
            if (clazz != null) {
//                onClassSelected(clazz)
            }
        }.also { add(it, BorderLayout.LINE_END) }

//        dumbActionButton("Select as Example", "Mock or disable this Class", AllIcons.RunConfigurations.ToolbarSkipped) {
//        }.also { add(it, BorderLayout.LINE_END) }
    }
}

private val PsiFile.clazz get() = PsiTreeUtil.findChildOfType(this, OCImplementation::class.java)
