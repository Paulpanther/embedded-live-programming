package com.elp.actions

import com.elp.util.error
import com.elp.util.panel
import com.elp.services.classService
import com.elp.services.isExample
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.components.JBList
import com.intellij.ui.components.dialog
import com.intellij.util.application
import java.awt.BorderLayout
import javax.swing.JTextField

class CreateOrOpenExampleAction: IntentionAction {
    override fun startInWriteAction() = false
    override fun getFamilyName() = "ExampleActions"
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = file?.isExample == false
    override fun getText() = "Create or open Example"

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file == null || file.isExample) return
        val clazz = project.classService.findClass(file.virtualFile) ?: return project.error("File contains no class")

        val examples = clazz.examples
        val list = JBList(examples)
        val field = JTextField()
        ListSpeedSearch(list)

        val panel = panel {
            layout = BorderLayout()

            add(field, BorderLayout.NORTH)
            add(list, BorderLayout.CENTER)
        }
        application.invokeLater {
            val dialog = dialog(
                "Create or open Example",
                panel,
                focusedComponent = list)
            if (!dialog.showAndGet()) return@invokeLater

            if (field.text != "") {
                clazz.addExample(field.text.trim())
            } else {
                val selected = list.selectedValue ?: return@invokeLater
                selected.activate()
            }
        }
    }
}
