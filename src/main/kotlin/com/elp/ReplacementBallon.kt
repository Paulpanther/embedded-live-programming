package com.elp

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.BalloonImpl
import com.intellij.ui.BalloonImpl.ShadowBorderProvider
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.PositionTracker
import java.awt.*
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

object ReplacementBallon {

    fun create(editor: Editor, target: PsiElement) {
        val content = JPanel()
        content.layout = BorderLayout()
        val textField = JTextField("Hello World")
        textField.border = BorderFactory.createEmptyBorder()
        content.add(textField, BorderLayout.CENTER)

        val balloon = JBPopupFactory.getInstance()
            .createBalloonBuilder(content)
            .setCloseButtonEnabled(true)
            .setFillColor(JBColor.PanelBackground)
            .setHideOnAction(false)
            .setHideOnKeyOutside(false)
            .setHideOnClickOutside(false)
            .createBalloon()

        (balloon as BalloonImpl).setShadowBorderProvider(object : ShadowBorderProvider {
            override fun getInsets() = JBUI.emptyInsets()
            override fun paintShadow(component: JComponent, g: Graphics) {}
            override fun paintBorder(bounds: Rectangle, g: Graphics2D) {}
            override fun paintPointingShape(
                bounds: Rectangle,
                pointTarget: Point,
                position: Balloon.Position,
                g: Graphics2D
            ) {
            }
        })

        val factory = JBPopupFactory.getInstance()

        balloon.show(object : PositionTracker<Balloon>(editor.contentComponent) {
            override fun recalculateLocation(balloon: Balloon): RelativePoint {
                val p = editor.offsetToVisualPosition(target.startOffset)
                editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, p)

                return factory.guessBestPopupLocation(editor)
            }
        }, Balloon.Position.above)
    }
}
