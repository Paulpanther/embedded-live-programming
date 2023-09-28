@file:Suppress("UnstableApiUsage")

package com.paulmethfessel.elp.editor

import com.paulmethfessel.elp.util.document
import com.paulmethfessel.elp.execution.*
import com.paulmethfessel.elp.services.exampleService
import com.paulmethfessel.elp.services.isExample
import com.paulmethfessel.elp.util.navigable
import com.paulmethfessel.elp.util.struct
import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.VerticalListInlayPresentation
import com.intellij.openapi.editor.BlockInlayPriority
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.cidr.lang.psi.OCStruct
import javax.swing.JComponent

/**
 * shows inlays for replaced fields and methods in class
 */
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
    ) = object : FactoryInlayHintsCollector(editor) {
        private val project = file.project
        private val example = project.exampleService.activeExample
        private val document = file.document ?: error("")

        override fun collect(
            element: PsiElement,
            editor: Editor,
            sink: InlayHintsSink
        ): Boolean {
            if (element.containingFile.isExample) return false
            if (example == null || element !is PsiFile) return false
            val struct = element.struct ?: return false
            val modifications = example.modifications.filter { it.originalStruct.name == struct.name }

            collectReplacedClasses(struct)
            collectReplacements(struct, modifications)
            collectAdditions(struct, modifications)
            return true
        }

        private fun collectReplacedClasses(struct: OCStruct) {
            val inExample = example?.ownReplacedStructs?.any { it.name == struct.name } == true
            if (!inExample) return

            sink.addBlockElement(
                struct.startOffset,
                relatesToPrecedingText = false,
                showAbove = true,
                BlockInlayPriority.ANNOTATIONS,
                navigable("Replaced in Example", struct.navigable))
        }

        private fun collectAdditions(struct: OCStruct, modifications: List<Modification>) {
            val offset = struct.functionsStartOffset
            val added = modifications.filterAdditions()
            if (added.isEmpty()) return

            var p: InlayPresentation = VerticalListInlayPresentation(added.map {
                navigable("+ ${it.added}", it.added.element.navigable)
            })
            val firstMember = struct.members.firstOrNull()
            if (firstMember != null) {
                p = shifted(p, firstMember)
            }

            sink.addBlockElement(
                offset,
                relatesToPrecedingText = true,
                showAbove = true,
                BlockInlayPriority.ANNOTATIONS,
                p
            )
        }

        private fun collectReplacements(struct: OCStruct, modifications: List<Modification>) {
            val members = struct.memberFunctions + struct.memberFields
            val replacements = modifications.filterReplacements()
            for (member in members) {
                val modification = replacements
                    .find { it.original == member } ?: continue

                val descriptor = modification.added.navigable ?: continue
                if (modification.added is Member.Field) {
                    val value = modification.added.value ?: continue
                    val shifted = navigable(text("= $value"), descriptor)

                    sink.addInlineElement(
                        member.element.endOffset,
                        relatesToPrecedingText = true,
                        shifted,
                        placeAtTheEndOfLine = false)
                } else {
                    val shifted = shifted(navigable("Replaced in Example", descriptor), member.element)

                    sink.addBlockElement(
                        member.element.startOffset,
                        relatesToPrecedingText = true,
                        showAbove = true,
                        BlockInlayPriority.ANNOTATIONS,
                        shifted)
                }
            }
        }

        private fun navigable(text: String, descriptor: OpenFileDescriptor) =
            navigable(factory.smallText(text), descriptor)

        private fun navigable(text: InlayPresentation, descriptor: OpenFileDescriptor): InlayPresentation {
            return factory.onClick(text, MouseButton.Left) { _, _ ->
                example?.navigateTo(descriptor)
            }
        }

        private fun shifted(presentation: InlayPresentation, element: PsiElement): InlayPresentation {
            val offset = element.startOffset
            val width = EditorUtil.getPlainSpaceWidth(editor)
            val line = document.getLineNumber(offset)
            val startOffset = document.getLineStartOffset(line)
            val column = offset - startOffset
            return factory.inset(presentation, left = column * width)
        }

        private fun text(text: String): InlayPresentation {
            return factory.inset(factory.smallText(text), top = 5, left = 3)
        }
    }

    override fun createConfigurable(settings: NoSettings) =
        object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return panel { }
            }
        }
}

