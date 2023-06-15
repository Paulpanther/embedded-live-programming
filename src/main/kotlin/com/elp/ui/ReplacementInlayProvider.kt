@file:Suppress("UnstableApiUsage")

package com.elp.ui

import com.elp.document
import com.elp.logic.Modification
import com.elp.logic.signature
import com.elp.services.Example
import com.elp.services.exampleService
import com.elp.services.probeService
import com.intellij.codeInsight.hints.*
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.editor.BlockInlayPriority
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.cidr.lang.psi.OCAssignmentExpression
import com.jetbrains.cidr.lang.psi.OCDeclarationStatement
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition
import javax.swing.JComponent

class ReplacementInlayProvider : InlayHintsProvider<NoSettings> {
    override val key = SettingsKey<NoSettings>("replacement")
    override val name = "Replacement"
    override val previewText = "Replacement"

    override fun createSettings() = NoSettings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ) = object: FactoryInlayHintsCollector(editor) {
        private val project = file.project
        private val example = project.exampleService.activeExample
        private val document = file.document

        override fun collect(
            element: PsiElement,
            editor: Editor,
            sink: InlayHintsSink
        ): Boolean {
            if (example == null) return false

            when (element) {
                is OCFunctionDefinition -> {
                    collectFunctionReplacement(element)
                }
            }
            return true
        }

        private fun collectFunctionReplacement(element: OCFunctionDefinition) {
            val modification = (example ?: return).modifications
                .filterIsInstance<Modification.ReplaceFunction>()
                .find { it.signature == element.signature } ?: return

            document ?: return
            val offset = element.startOffset
            val width = EditorUtil.getPlainSpaceWidth(editor)
            val line = document.getLineNumber(offset)
            val startOffset = document.getLineStartOffset(line)
            val column = offset - startOffset
            val presentation = factory.smallText("Replaced in Example")
            val shifted = factory.inset(presentation, left = column * width)

            sink.addBlockElement(element.startOffset,
                relatesToPrecedingText = true,
                showAbove = true,
                BlockInlayPriority.ANNOTATIONS,
                shifted)
        }
    }

    override fun createConfigurable(settings: NoSettings) =
        object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return panel { }
            }
        }
}
