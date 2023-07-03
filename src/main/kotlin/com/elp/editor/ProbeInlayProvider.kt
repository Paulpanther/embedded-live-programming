@file:Suppress("UnstableApiUsage")

package com.elp.editor

import com.elp.services.probeService
import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.cidr.lang.psi.OCAssignmentExpression
import com.jetbrains.cidr.lang.psi.OCDeclarationStatement
import com.jetbrains.cidr.lang.psi.OCExpression
import com.jetbrains.cidr.lang.psi.OCReturnStatement
import javax.swing.JComponent

class ProbeInlayProvider: InlayHintsProvider<NoSettings> {
    override val key = SettingsKey<NoSettings>("probe")
    override val name = "Probe"
    override val previewText = "Probe"

    override fun createSettings() = NoSettings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return object: FactoryInlayHintsCollector(editor) {
            override fun collect(
                element: PsiElement,
                editor: Editor,
                sink: InlayHintsSink
            ): Boolean {
                matchExpression(element)
                return true
            }

            private fun matchExpression(element: PsiElement) {
                when (element) {
                    is OCDeclarationStatement -> createInlay(element.declaration.declarators.firstOrNull()?.initializer ?: return)
                    is OCAssignmentExpression -> createInlay(element.sourceExpression ?: return)
                    is OCReturnStatement -> createInlay(element.expression ?: return)
                }
            }

            private fun createInlay(element: OCExpression) {
                val probe = probeService.probes[file.name]?.find { it.range == element.textRange } ?: return
                sink.addInlineElement(element.startOffset, false, probe.createPresentation(factory, editor as EditorImpl), true)
            }

        }
    }

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object: ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return panel {  }
            }
        }
    }
}
