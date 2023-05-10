package com.elp.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.BalloonImpl
import com.intellij.ui.BalloonImpl.ShadowBorderProvider
import com.intellij.ui.EditorTextField
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.PositionTracker
import com.jetbrains.cidr.lang.OCFileType
import java.awt.*
import javax.swing.*

object ReplacementBallon {

    fun create(editor: Editor, target: PsiElement) {
        val content = JPanel().apply {
            layout = BorderLayout()
        }

        val textField = EditorTextField("Hello World", target.project, OCFileType.INSTANCE).apply {
            border = BorderFactory.createEmptyBorder()
        }

        val closeButton = JButton(AllIcons.Ide.Notification.Close).apply {
            size = Dimension(icon.iconWidth, icon.iconHeight)
            preferredSize = Dimension(icon.iconWidth, icon.iconHeight)
            border = null
            isBorderPainted = false
            margin = JBUI.emptyInsets()
            isContentAreaFilled = false
            isFocusPainted = false
            isOpaque = false
        }

        content.add(textField, BorderLayout.CENTER)
        content.add(closeButton, BorderLayout.LINE_END)

        val balloon = JBPopupFactory.getInstance()
            .createBalloonBuilder(content)
            .setCloseButtonEnabled(false)
//            .setFillColor(EditorColors.NOTIFICATION_BACKGROUND.defaultColor)
            .setHideOnAction(false)
            .setHideOnKeyOutside(false)
            .setHideOnClickOutside(false)
            .createBalloon()

        (balloon as BalloonImpl).setShadowBorderProvider(NoShadowBorderProvider())

        ExpressionSelector(editor, "Select expression to replace") { selected ->
            val inlay = editor.inlayModel.addBlockElement(selected.startOffset, false, true, 0, EmptyInlayElementRenderer()) ?: return@ExpressionSelector
            val highlight = editor.markupModel.addRangeHighlighter(EditorColors.DELETED_TEXT_ATTRIBUTES, selected.startOffset, selected.endOffset, HighlighterLayer.WARNING, HighlighterTargetArea.EXACT_RANGE)

            closeButton.addActionListener {
                editor.markupModel.removeHighlighter(highlight)
                balloon.hide()
                inlay.dispose()
            }

            balloon.show(InlayPositionTracker(editor, inlay), Balloon.Position.above)
        }.show(target)
    }
}

private class NoShadowBorderProvider: ShadowBorderProvider {
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
}

private class EmptyInlayElementRenderer: EditorCustomElementRenderer {
    override fun calcWidthInPixels(inlay: Inlay<*>) = 100 // Abitrary number
}

private class InlayPositionTracker(
    private val editor: Editor,
    private val inlay: Inlay<*>,
): PositionTracker<Balloon>(editor.contentComponent) {
    private val factory = JBPopupFactory.getInstance()
    private var last: RelativePoint? = null

    override fun recalculateLocation(balloon: Balloon): RelativePoint {
        val p = inlay.visualPosition
        editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, p)

        if (!factory.isBestPopupLocationVisible(editor)) {
            (balloon as BalloonImpl).component?.apply { isVisible = false }
            last?.let { return it }
        }

        (balloon as BalloonImpl).component?.apply { if (!isVisible) isVisible = true }

        val pos = factory.guessBestPopupLocation(editor)
        var y = pos.screenPoint.y
        if (pos.point.y > editor.lineHeight + balloon.preferredSize.height) {
            y -= editor.lineHeight
        }

        return RelativePoint(Point(pos.screenPoint.x, y)).also { last = it }
    }
}
