package com.elp.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.ui.JBUI
import com.jetbrains.cidr.lang.psi.OCClassDeclaration
import com.jetbrains.cidr.lang.psi.visitors.OCVisitor
import com.jetbrains.cidr.lang.ui.OCClassChooserDialog
import java.awt.FlowLayout
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class ExampleToolWindowSettingsView(
    private val project: Project
): JPanel() {
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        add(JPanel().apply {
            layout = FlowLayout()

            val openClass = getOpenClass() ?: "non selected"

            JLabel("Class: ").also { add(it) }
            val classLabel = JLabel(openClass).also { add(it) }
            JButton(AllIcons.Actions.Edit).apply {
                border = BorderFactory.createEmptyBorder()
                isOpaque = false

                addActionListener {
                    val classChooser = OCClassChooserDialog("Choose class", project, null, null) { true }
                    classChooser.showDialog()
                    val selectedClass = classChooser.selected.name

                    classLabel.text = selectedClass
                }
            }.also { add(it) }
        })
    }

    private fun getOpenClass(): String? {
        val doc = FileEditorManager.getInstance(project).selectedTextEditor?.document ?: return null
        val file = PsiDocumentManager.getInstance(project).getPsiFile(doc) ?: return null

        var name: String? = null
        file.accept(object: OCVisitor() {
            override fun visitClassDeclaration(dcl: OCClassDeclaration<*>?) {
                name = dcl?.canonicalName
            }
        })

        return name
    }
}
