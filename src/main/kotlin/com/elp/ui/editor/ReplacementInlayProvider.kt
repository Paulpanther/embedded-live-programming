@file:Suppress("UnstableApiUsage")

package com.elp.ui.editor

import com.elp.document
import com.elp.logic.*
import com.elp.services.exampleService
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
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition
import com.jetbrains.cidr.lang.psi.OCStatement
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
    ) = object : FactoryInlayHintsCollector(editor) {
        private val project = file.project
        private val example = project.exampleService.activeExample
        private val document = file.document ?: error("")

        override fun collect(
            element: PsiElement,
            editor: Editor,
            sink: InlayHintsSink
        ): Boolean {
            if (example == null || file != example.clazz.file || element !is PsiFile) return false
            val struct = element.struct ?: return false

            collectFunctionReplacement(struct)
            collectMemberReplacement(struct)
            collectAddedFunctionsAndMembers(struct)
            return true
        }

        private fun collectAddedFunctionsAndMembers(struct: OCStruct) {
            val offset = struct.functionsStartOffset
            val added = (example ?: return).modifications
                .mapNotNull {
                    when (it) {
                        is Modification.AddedFunction -> it.signature
                        is Modification.AddedMember -> it.signature
                        else -> null
                    }
                }
            if (added.isEmpty()) return

            var p: InlayPresentation = VerticalListInlayPresentation(added.map {
                navigable("+ $it", it)
                factory.smallText("+ $it")
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

        private fun collectFunctionReplacement(struct: OCStruct) {
            val functions = struct.functions
            for (function in functions) {
                val modification = (example ?: return).modifications
                    .filterIsInstance<Modification.ReplaceFunction>()
                    .find { it.signature == function.signature } ?: return

                val descriptor = modification.original.nameIdentifier?.navigable ?: return
                val shifted = shifted(navigable("Replaced in Example", descriptor), function)

                sink.addBlockElement(
                    function.startOffset,
                    relatesToPrecedingText = true,
                    showAbove = true,
                    BlockInlayPriority.ANNOTATIONS,
                    shifted
                )
            }
        }

        private fun collectMemberReplacement(struct: OCStruct) {
            val members = struct.fields
            for (member in members) {
                val modification = (example ?: return).modifications
                    .filterIsInstance<Modification.ReplaceMember>()
                    .find { it.signature == member.signature } ?: return

                val value = modification.value
                if (value == null) {
                    val descriptor = modification.original.declarators.firstOrNull()?.navigable ?: return
                    val shifted = shifted(navigable("Replaced in Example", descriptor), member)

                    sink.addBlockElement(
                        member.startOffset,
                        relatesToPrecedingText = true,
                        showAbove = true,
                        BlockInlayPriority.ANNOTATIONS,
                        shifted
                    )
                } else {
                    val presentation = text("= $value")
                    sink.addInlineElement(member.endOffset, true, presentation, false)
                }
            }
        }

        private fun navigable(text: String, descriptor: OpenFileDescriptor): InlayPresentation {
            return factory.onClick(factory.smallText(text), MouseButton.Left) { _, _ ->
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

