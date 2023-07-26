package com.elp.actions

import com.elp.model.Example
import com.elp.services.Clazz
import com.elp.util.error
import com.elp.util.panel
import com.elp.services.classService
import com.elp.services.exampleService
import com.elp.services.isExample
import com.elp.util.NamingHelper
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.PsiFile
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.components.JBList
import com.intellij.ui.components.dialog
import java.awt.BorderLayout
import javax.swing.JTextField

/**
 * press alt + enter on a class to open this.
 * Creates an example or opens the selected one for this class
 */
class CreateOrOpenExampleAction: IntentionAction {
    override fun startInWriteAction() = false
    override fun getFamilyName() = "ExampleActions"
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = file?.isExample == false
    override fun getText() = "Create or open Example"

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file == null || file.isExample) return
        val clazz = project.classService.findClass(file.virtualFile) ?: return project.error("File contains no class")
        clazz.showCreateOrOpenExampleDialog()
    }
}

fun Clazz.showCreateOrOpenExampleDialog(callback: (Example) -> Unit = {}) {
    val list = JBList(examples)
    val field = JTextField(NamingHelper.nextName("Example", examples.map { it.name } ))
    ListSpeedSearch(list)

    val panel = panel {
        layout = BorderLayout()

        add(field, BorderLayout.NORTH)
        add(list, BorderLayout.CENTER)
    }
    val dialog = dialog(
        "Create or open Example",
        panel,
        focusedComponent = list,
        ok = {
            if (field.text.isNotBlank()) {
                val nameUsed = examples.any { it.name == field.text }
                if (nameUsed) listOf(ValidationInfo("Example named already used", field))
                null
            } else {
                list.selectedValue ?: ValidationInfo("No class selected", list)
                null
            }
        }
    )
    if (!dialog.showAndGet()) return

    if (field.text != "") {
        this.addExample(field.text.trim(), callback)
    } else {
        val selected = list.selectedValue ?: return
        selected.activate()
    }
}

fun activeExampleOrCreate(project: Project, callback: (Example) -> Unit) {
    val example = project.exampleService.activeExample
    if (example != null) return callback(example)

    val clazz = project.classService.currentClass
    showCreateExampleDialog(project, clazz, callback)
}

fun showCreateExampleDialog(project: Project, initialClass: Clazz?, callback: (Example) -> Unit = {}) {
    val field = JTextField(NamingHelper.nextName("Example", project.exampleService.examples.map { it.name } ))
    val list = JBList(project.classService.classes)
    if (initialClass != null) {
        list.setSelectedValue(initialClass, true)
    }

    val dialog = object: DialogWrapper(project) {
        init {
            title = "Create Example"
            init()
        }

        override fun createCenterPanel() = panel {
            layout = BorderLayout()
            add(field, BorderLayout.NORTH)
            add(list, BorderLayout.CENTER)
        }

        override fun getPreferredFocusedComponent() = field

        override fun doValidate(): ValidationInfo? {
            val clazz = list.selectedValue ?: return ValidationInfo("No class selected", list)
            val nameUsed = clazz.examples.any { it.name == field.text }
            if (nameUsed) return ValidationInfo("Name already in use", field)
            return null
        }
    }

    invokeLater {
        if (dialog.showAndGet()) {
            val clazz = list.selectedValue!!
            clazz.addExample(field.text, callback)
        }
    }
}
