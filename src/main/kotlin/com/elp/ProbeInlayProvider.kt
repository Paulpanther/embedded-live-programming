@file:Suppress("UnstableApiUsage")

package com.elp

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InsetPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.TextInlayPresentation
import com.intellij.codeInsight.hints.presentation.WithAttributesPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.cidr.lang.psi.OCDeclaration
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
                if (element is OCDeclarationStatement) {
                    val probe = probeService.probes.find { it.range == element.textRange } ?: return true
                    sink.addInlineElement(element.startOffset, false, probe.createPresentation(factory), true)
                    return false
                }
                return true
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
