package com.elp

import com.intellij.codeInsight.hints.fireContentChanged
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.event.MouseEvent

const val ID = "EmbeddedLiveProgramming"

class HITLWidgetFactory: StatusBarWidgetFactory {
    override fun getId() = ID
    override fun getDisplayName() = "Embedded LP"
    override fun isAvailable(project: Project) = true
    override fun createWidget(project: Project) = HITLWidget()
    override fun disposeWidget(widget: StatusBarWidget) = widget.dispose()
    override fun canBeEnabledOn(statusBar: StatusBar) = true
}

class HITLWidget: StatusBarWidget, StatusBarWidget.IconPresentation {
    override fun ID() = ID
    override fun getPresentation() = this
    override fun getTooltipText() = "Start hardware integration"
    override fun getIcon() = ELPIcons.logo
    override fun dispose() = Unit

    override fun install(statusBar: StatusBar) = Unit

    override fun getClickConsumer() = Consumer<MouseEvent> {
    }
}
