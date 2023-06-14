@file:Suppress("UnstableApiUsage")

package com.elp.ui

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
import javax.swing.JComponent

class ProbeInlayProvider: InlayHintsProvider<NoSettings> {
    override val key = SettingsKey<NoSettings>("probe")
    override val name = "Probe"
    override val previewText = "Probe"

    override fun createSettings(): NoSettings {
        return NoSettings()
    }

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
                fun createInlay(element: PsiElement) {
                    val path = file.virtualFile.path
                    val probe = probeService.probes[path]?.find { it.range == element.textRange } ?: return
                    sink.addInlineElement(element.startOffset, false, probe.createPresentation(factory, editor as EditorImpl), true)
                }

                return when (element) {
                    is OCDeclarationStatement,
                    is OCAssignmentExpression -> {
                        createInlay(element)
                        true
                    }
                    else -> true
                }
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
