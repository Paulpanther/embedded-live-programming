package com.elp.ui

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.popup.AbstractPopup
import com.intellij.ui.speedSearch.ListWithFilter
import com.jetbrains.cidr.lang.psi.OCExpression
import com.jetbrains.cidr.lang.psi.OCStatement
import kotlin.reflect.KClass

class ExpressionSelector(
    private val editor: Editor,
    private val title: String,
    private val onSelect: (selection: PsiElement) -> Unit
) {

    fun show(target: PsiElement) {
        val scopes = findScopes(target)
        val scopeArray = PsiUtilCore.toPsiElementArray(scopes)
        val highlight = SelectionHighlighter(editor)

        val popup = NavigationUtil.getPsiElementPopup(scopeArray, object: PsiElementListCellRenderer<PsiElement>() {
            override fun getElementText(element: PsiElement?) = element?.text ?: "No text"
            override fun getContainerText(element: PsiElement?, name: String?) = null
        }, title) {
            onSelect(it)
            highlight.remove()
            false
        }.apply {
            addListener(object: JBPopupListener {
                override fun onClosed(event: LightweightWindowEvent) {
                    highlight.remove()
                }
            })

            showInBestPositionFor(editor)
        }

        // Hacky
        ((popup as AbstractPopup).component.components[1] as ListWithFilter<PsiElement>).list.apply {
            addListSelectionListener {
                selectedValue?.let { highlight.show(it) }
            }

            selectedValue?.let { highlight.show(it) }
        }

    }

    private fun findScopes(target: PsiElement): List<PsiElement> {
        return PsiTreeUtil.collectParents(target, OCExpression::class.java, true) {
            it.parent == null
        }
    }
}


private class SelectionHighlighter(
    private val editor: Editor
) {
    private var highlight: RangeHighlighter? = null

    fun show(element: PsiElement) {
        remove()
        highlight = editor.markupModel.addRangeHighlighter(
            EditorColors.LIVE_TEMPLATE_ATTRIBUTES,
            element.startOffset,
            element.endOffset,
            HighlighterLayer.WARNING,
            HighlighterTargetArea.EXACT_RANGE)
    }

    fun remove() {
        highlight?.let { editor.markupModel.removeHighlighter(it) }
    }
}
