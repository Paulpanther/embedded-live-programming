package com.elp.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
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
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

object ReplacementBallon {

    fun create(editor: Editor, target: PsiElement) {
        val content = JPanel()
        content.layout = BorderLayout()

        val textField = JTextField("Hello World")
        textField.border = BorderFactory.createEmptyBorder()

        val closeButton = JButton("X")

        content.add(textField, BorderLayout.CENTER)
        content.add(closeButton, BorderLayout.EAST)

        val balloon = JBPopupFactory.getInstance()
            .createBalloonBuilder(content)
            .setCloseButtonEnabled(false)
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

        val inlay = editor.inlayModel.addBlockElement(target.startOffset, false, true, 0, object: EditorCustomElementRenderer {
            override fun calcWidthInPixels(inlay: Inlay<*>): Int {
                return 100
            }
        })

        closeButton.addActionListener {
            balloon.hide()
            inlay?.dispose()
        }

        val factory = JBPopupFactory.getInstance()

        balloon.show(object : PositionTracker<Balloon>(editor.contentComponent) {
            override fun recalculateLocation(balloon: Balloon): RelativePoint {
                val p = editor.offsetToVisualPosition(target.startOffset)
                editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, p)

                val pos = factory.guessBestPopupLocation(editor)
                var y = pos.screenPoint.y
                if (pos.point.y > editor.lineHeight + balloon.preferredSize.height) {
                    y -= editor.lineHeight
                }

                return RelativePoint(Point(pos.screenPoint.x, y))
            }
        }, Balloon.Position.above)
    }
}
