package com.elp.ui.toolWindow

import com.elp.services.Clazz
import com.elp.services.classService
import com.elp.services.exampleService
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionListModel
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
import com.jetbrains.rd.util.getOrCreate
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
    private val list: JBList<Clazz>
    private var exampleChangeRequest = false

    init {
        classService.onClassesChanged.register(::onClassesUpdate)
        project.exampleService.onActiveExampleChanged.register(::onExampleChanged)

        list = JBList(classesModel).apply {
            installCellRenderer { JLabel(it.name) }
            selectionMode = ListSelectionModel.SINGLE_SELECTION

            addListSelectionListener {
                if (it.valueIsAdjusting) return@addListSelectionListener
                val clazz = selectedValue ?: return@addListSelectionListener
                val view = exampleViews.getOrCreate(clazz) { TabbedExamplesView(project, clazz, parentDisposable) }
                view.makeActive(exampleChangeRequest)
                exampleChangeRequest = false
                splitter.secondComponent = view.component
            }
        }
        splitter.firstComponent = list
    }

    val component: JComponent = splitter

    private fun onClassesUpdate() {
        classesModel.replaceAll(classService.classes)
    }

    private fun onExampleChanged() {
        val activeClass = project.exampleService.activeExample?.clazz
        val prevClass = list.selectedValue
        if (activeClass != prevClass) {
            val i = classesModel.items.indexOf(activeClass)
            if (i == -1) return
            exampleChangeRequest = true
            list.selectedIndex = i
        }
    }
}
