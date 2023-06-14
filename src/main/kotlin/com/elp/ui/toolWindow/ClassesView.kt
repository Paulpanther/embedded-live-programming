package com.elp.ui.toolWindow

import com.elp.panel
import com.elp.plusAssign
import com.elp.services.classService
import com.intellij.openapi.project.Project
import com.intellij.ui.OnePixelSplitter
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel

class ClassesView(
    private val project: Project
) {
    private val splitter = OnePixelSplitter()
    private val classService get() = project.classService

    init {
        classService.onClassesChanged.register(::onClassesUpdate)
    }

    val component: JComponent = splitter

    private fun onClassesUpdate() {
        splitter.firstComponent = createClassListPanel()
    }

    private fun createClassListPanel() = panel {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        for (clazz in classService.classes) {
            val label = JLabel(clazz.name)
            add(label)
        }

        this += Box.createGlue()
    }
}
