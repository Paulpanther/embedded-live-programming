package com.elp.ui

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.BalloonImpl
import com.intellij.ui.BalloonImpl.ActionButton
import com.intellij.ui.BalloonImpl.ShadowBorderProvider
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.popup.AbstractPopup
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.ui.speedSearch.ListWithFilter
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.PositionTracker
import java.awt.*
import javax.swing.*

object ReplacementBallon {

    fun create(editor: Editor, target: PsiElement) {
        val content = JPanel().apply {
            layout = BorderLayout()
            isOpaque = false
        }

        val textField = JTextField("Hello World").apply {
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
            .setFillColor(JBColor.PanelBackground)
            .setHideOnAction(false)
            .setHideOnKeyOutside(false)
            .setHideOnClickOutside(false)
            .createBalloon()

        (balloon as BalloonImpl).setShadowBorderProvider(NoShadowBorderProvider())

        val scopes = mutableListOf<PsiElement>()
        scopes += target
        scopes += target.parent
        scopes += target.parent.parent
        scopes += target.parent.parent.parent
        scopes += target.parent.parent.parent.parent
        val scopeArray = PsiUtilCore.toPsiElementArray(scopes)

        val popup = NavigationUtil.getPsiElementPopup(scopeArray, object: PsiElementListCellRenderer<PsiElement>() {
            override fun getElementText(element: PsiElement?): String {
                return element?.text ?: "No text"
            }

            override fun getContainerText(element: PsiElement?, name: String?): String? {
                return "Container"
            }
        }, "Hello Scope") {
            println(it)
            false
        }.apply {
            showInBestPositionFor(editor)
        }

        var highlight: RangeHighlighter? = null

        // Hacky
        val list = ((popup as AbstractPopup).component.components[1] as ListWithFilter<PsiElement>).list
        list.addListSelectionListener {
            val selected = scopes[it.firstIndex]

            highlight?.let { editor.markupModel.removeHighlighter(it) }
            highlight = editor.markupModel.addRangeHighlighter(CodeInsightColors.WARNINGS_ATTRIBUTES, selected.startOffset, selected.endOffset, HighlighterLayer.WARNING, HighlighterTargetArea.EXACT_RANGE)
        }

//        val inlay = editor.inlayModel.addBlockElement(target.startOffset, false, true, 0, EmptyInlayElementRenderer())

//        val highlight = editor.markupModel.addRangeHighlighter(CodeInsightColors.WARNINGS_ATTRIBUTES, target.startOffset, target.endOffset, HighlighterLayer.WARNING, HighlighterTargetArea.EXACT_RANGE)

//        closeButton.addActionListener {
//            editor.markupModel.removeHighlighter(highlight)
//            balloon.hide()
//            inlay?.dispose()
//        }

//        balloon.show(PsiElementPositionTracker(editor, target), Balloon.Position.above)
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

private class PsiElementPositionTracker(
    private val editor: Editor,
    private val target: PsiElement
): PositionTracker<Balloon>(editor.contentComponent) {
    private val factory = JBPopupFactory.getInstance()

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
}
