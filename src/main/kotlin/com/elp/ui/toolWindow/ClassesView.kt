package com.elp.ui.toolWindow

import com.elp.panel
import com.elp.plusAssign
import com.elp.services.Clazz
import com.elp.services.classService
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionListModel
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
import com.jetbrains.rd.util.getOrCreate
import java.awt.Component
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.ListSelectionModel

class ClassesView(
    private val project: Project,
    private val parentDisposable: Disposable
) {
    private val splitter = OnePixelSplitter()
    private val classService get() = project.classService
    private val classesModel = CollectionListModel<Clazz>()
    private val exampleViews = mutableMapOf<Clazz, TabbedExamplesView>()

    init {
        classService.onClassesChanged.register(::onClassesUpdate)
        splitter.firstComponent = JBList(classesModel).apply {
            installCellRenderer { JLabel(it.name) }
            selectionMode = ListSelectionModel.SINGLE_SELECTION

            addListSelectionListener {
                if (it.valueIsAdjusting) return@addListSelectionListener
                val clazz = selectedValue ?: return@addListSelectionListener
                val view = exampleViews.getOrCreate(clazz) { TabbedExamplesView(project, clazz, parentDisposable) }
                view.makeActive()
                splitter.secondComponent = view.component
            }
        }
    }

    val component: JComponent = splitter

    private fun onClassesUpdate() {
        classesModel.replaceAll(classService.classes)
    }
}
