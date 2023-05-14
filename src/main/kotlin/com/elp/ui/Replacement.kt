package com.elp.ui

import com.elp.exampleService
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.BalloonImpl
import com.intellij.ui.BalloonImpl.ShadowBorderProvider
import com.intellij.ui.EditorTextField
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.PositionTracker
import com.jetbrains.cidr.lang.OCFileType
import com.jetbrains.cidr.lang.util.OCElementFactory
import java.awt.*
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class Replacement(
    private val editor: Editor,
    private val target: PsiElement
) {
    private val project = target.project
    private var inlay: Inlay<EmptyInlayElementRenderer>
    private var balloon: BalloonImpl
    private var highlight: RangeHighlighter
    private var editorField: ReplacementEditor? = null
    private var visible = true
    private var disposed = false

    companion object {
        fun selectTargetThenShow(editor: Editor, initialTarget: PsiElement) {
            ExpressionSelector(editor, "Select expression to replace") { selected ->
                Replacement(editor, selected)
            }.show(initialTarget)
        }
    }

    init {
        inlay = createAndShowInlay()
        highlight = createAndShowHighlight()
        balloon = createBalloon()

        project.exampleService.activeExample.addReplacement(this)

        balloon.show(InlayPositionTracker(editor, inlay), Balloon.Position.above)
    }

    private fun onCloseAction() {
        project.exampleService.activeExample.removeReplacement(this)
    }

    fun dispose() {
        if (disposed) return
        disposed = true

        hide(check = false)
        balloon.dispose()
        project.exampleService.activeExample.removeReplacement(this)
    }

    fun hide(check: Boolean = true) {
        if (check && !visible) return
        visible = false

        balloon.dispose()
        highlight.dispose()
        inlay.dispose()
    }

    fun show() {
        if (visible) return
        visible = true

        inlay = createAndShowInlay()
        highlight = createAndShowHighlight()
        balloon = createBalloon()
    }

    private fun createAndShowHighlight(): RangeHighlighter {
        return editor.markupModel.addRangeHighlighter(
            EditorColors.DELETED_TEXT_ATTRIBUTES,
            target.startOffset,
            target.endOffset,
            HighlighterLayer.WARNING,
            HighlighterTargetArea.EXACT_RANGE
        )
    }

    private fun createAndShowInlay(): Inlay<EmptyInlayElementRenderer> {
        return editor.inlayModel.addBlockElement(
            target.startOffset,
            false,
            true,
            0,
            EmptyInlayElementRenderer())!!
    }

    private fun createBalloon() = (JBPopupFactory.getInstance()
        .createBalloonBuilder(createBalloonContent())
        .setCloseButtonEnabled(false)
//            .setFillColor(EditorColors.NOTIFICATION_BACKGROUND.defaultColor)
        .setHideOnAction(false)
        .setHideOnKeyOutside(false)
        .setHideOnClickOutside(false)
        .createBalloon() as BalloonImpl)
        .apply {
            setShadowBorderProvider(NoShadowBorderProvider())
        }

    private fun createBalloonContent(): JPanel {
        val content = JPanel().apply {
            layout = BorderLayout()
        }

        val editorField = editorField ?: ReplacementEditor(project, target).also {
            editorField = it
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

            addActionListener { onCloseAction() }
        }

        content.add(editorField.field, BorderLayout.CENTER)
        content.add(closeButton, BorderLayout.LINE_END)

        return content
    }
}

private class ReplacementEditor(
    private val project: Project,
    private val target: PsiElement
) {
    val field: EditorTextField

    init {
        val code = OCElementFactory.expressionCodeFragmentCpp("123456789", project, target, true, true)
        val doc = PsiDocumentManager.getInstance(project).getDocument(code)!!

        field = EditorTextField(doc, project, OCFileType.INSTANCE).apply {
            border = BorderFactory.createEmptyBorder()
        }

//        doc.addDocumentListener(object : DocumentListener {
//            override fun documentChanged(event: DocumentEvent) {
//                val fieldEditor = textField.editor ?: return
//                val highlighter = TextEditorBackgroundHighlighter(project, fieldEditor)
//                val passes = highlighter.createPassesForEditor()
//
//                val indicator = DaemonProgressIndicator()
//                ProgressManager.getInstance().runProcess({
//                    application.runReadAction {
//                        for (pass in passes) {
//                            pass.collectInformation(indicator)
//                            application.runReadAction {
//                                pass.applyInformationToEditor()
//                            }
//                        }
//                    }
//                }, indicator)
//            }
//        })
    }
}

private class NoShadowBorderProvider : ShadowBorderProvider {
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

private class EmptyInlayElementRenderer : EditorCustomElementRenderer {
    override fun calcWidthInPixels(inlay: Inlay<*>) = 100 // Abitrary number
}

private class InlayPositionTracker(
    private val editor: Editor,
    private val inlay: Inlay<*>,
) : PositionTracker<Balloon>(editor.contentComponent) {
    private val factory = JBPopupFactory.getInstance()

    override fun recalculateLocation(balloon: Balloon): RelativePoint {
        var p = editor.visualPositionToXY(inlay.visualPosition)
        p = Point(p.x + balloon.preferredSize.width / 2, p.y)

        val balloonComponent = (balloon as BalloonImpl).component
        if (!editor.scrollingModel.visibleArea.contains(p)) {
            balloonComponent?.apply { isVisible = false }
        } else {
            balloonComponent?.apply { if (!isVisible) isVisible = true }
        }

        return RelativePoint(editor.contentComponent, p)
    }
}
