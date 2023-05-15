package com.elp.ui

import com.elp.Example
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.cidr.lang.psi.OCClassDeclaration
import com.jetbrains.cidr.lang.psi.visitors.OCVisitor
import com.jetbrains.cidr.lang.symbols.cpp.OCStructSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.ui.OCClassChooserDialog
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

class ExampleToolWindowSettingsView(
    private val project: Project,
    private val example: Example
): JPanel() {
    init {
        layout = BorderLayout()

        add(classSelectionPanel(), BorderLayout.NORTH)
        add(replacementPanel(), BorderLayout.CENTER)
    }

    private fun classSelectionPanel() = JPanel().apply {
        layout = FlowLayout()

        val openClass = getOpenClass() ?: "non selected"

        // Label "Class: "
        JLabel("Class: ").also { add(it) }

        // Label that shows current selected class
        val classLabel = JLabel(openClass).also { add(it) }

        // Button to allow changing the selected class
        JButton(AllIcons.Actions.Edit).apply {
            border = BorderFactory.createEmptyBorder()
            isOpaque = false

            addActionListener {
                val classChooser = OCClassChooserDialog("Choose class", project, null, null) { true }
                classChooser.showDialog()
                val selectedClass = classChooser.selected

                classLabel.text = selectedClass.name
                example.activeClass = selectedClass.symbol as OCStructSymbol
            }
        }.also { add(it) }
    }

    private fun replacementPanel() = JPanel(BorderLayout()).apply {
        val model = CollectionListModel(example.replacements)

        fun updateReplacements() {
            model.replaceAll(example.replacements)
            model.sort { o1, o2 ->
                val file = o1.targetFile.compareTo(o2.targetFile)

                if (file != 0) file
                else o1.targetLine.compareTo(o2.targetLine)
            }
        }
        updateReplacements()

        val list = JBList(model).apply {
            emptyText.text = "No Replacements"
            cellRenderer = ListCellRenderer { _, value, _, _, _ -> JLabel(value?.toString() ?: "undefined") }

            example.onReplacementsChange.register {
                updateReplacements()
            }
        }.also { add(JBScrollPane(it), BorderLayout.CENTER) }

        ToolbarDecorator
            .createDecorator(list)
            .setRemoveAction { println("Remove") }
            .createPanel()
            .also { add(it, BorderLayout.NORTH) }
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
