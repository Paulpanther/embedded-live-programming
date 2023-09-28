@file:Suppress("UnstableApiUsage")

package com.paulmethfessel.elp.editor

import com.paulmethfessel.elp.services.probeService
import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.cidr.lang.psi.OCExpression
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
                    is OCExpression -> createInlay(element)
                }
            }

            private fun createInlay(element: OCExpression) {
                val probe = probeService.probes[file.name]?.find { it.element == element || it.range == element.textRange && it.element == null } ?: return
                sink.addInlineElement(element.startOffset, false, probe.createPresentation(editor as EditorImpl, element), true)
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
