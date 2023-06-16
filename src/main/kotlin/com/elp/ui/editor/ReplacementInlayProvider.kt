@file:Suppress("UnstableApiUsage")

package com.elp.ui.editor

import com.elp.document
import com.elp.logic.Modification
import com.elp.logic.signature
import com.elp.services.exampleService
import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.VerticalListInlayPresentation
import com.intellij.openapi.editor.BlockInlayPriority
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition
import com.jetbrains.cidr.lang.psi.OCStruct
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
        private val document = file.document ?: error("")

        override fun collect(
            element: PsiElement,
            editor: Editor,
            sink: InlayHintsSink
        ): Boolean {
            if (example == null || file != example.clazz.file) return false

            when (element) {
                is OCFunctionDefinition -> collectFunctionReplacement(element)
                is PsiFile -> collectAddedFunctions(element)
            }
            return true
        }

        private fun collectAddedFunctions(file: PsiFile) {
            val struct = PsiTreeUtil.findChildOfType(file, OCStruct::class.java) ?: return
            val offset = struct.functionsStartOffset
            val addedFunctions = (example ?: return).modifications.filterIsInstance<Modification.AddedFunction>()
            if (addedFunctions.isEmpty()) return

            var p: InlayPresentation = VerticalListInlayPresentation(addedFunctions.map { factory.smallText("+ " + it.signature) })
            val firstMember = struct.members.firstOrNull()
            if (firstMember != null) {
                p = factory.shifted(p, firstMember)
            }

            sink.addBlockElement(
                offset,
                relatesToPrecedingText = true,
                showAbove = true,
                BlockInlayPriority.ANNOTATIONS,
                p)
        }

        private fun collectFunctionReplacement(element: OCFunctionDefinition) {
            val modification = (example ?: return).modifications
                .filterIsInstance<Modification.ReplaceFunction>()
                .find { it.signature == element.signature } ?: return

            val presentation = factory.smallText("Replaced in Example")
            val shifted = factory.shifted(presentation, element)

            sink.addBlockElement(element.startOffset,
                relatesToPrecedingText = true,
                showAbove = true,
                BlockInlayPriority.ANNOTATIONS,
                shifted)
        }

        private fun PresentationFactory.shifted(presentation: InlayPresentation, element: PsiElement): InlayPresentation {
            val offset = element.startOffset
            val width = EditorUtil.getPlainSpaceWidth(editor)
            val line = document.getLineNumber(offset)
            val startOffset = document.getLineStartOffset(line)
            val column = offset - startOffset
            return factory.inset(presentation, left = column * width)
        }
    }

    override fun createConfigurable(settings: NoSettings) =
        object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return panel { }
            }
        }
}

