@file:Suppress("UnstableApiUsage")

package com.elp.ui

import com.elp.model.Probe
import com.elp.services.probeService
import com.intellij.codeInsight.hints.presentation.*
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.TextRange
import com.jetbrains.cidr.lang.psi.OCExpression

class ProbePresentation(
    val code: Int,
    val range: TextRange,
    val isUserProbe: Boolean = false
) {
    private var probe: Probe? = null
    private var presentation: SparklineProbeWrapper? = null
    var editor: EditorImpl? = null
    var markedForUpdate = false
    var element: OCExpression? = null

    fun createPresentation(editor: EditorImpl, element: OCExpression): InlayPresentation {
        this.element = element
        this.editor = editor
        return SparklineProbeWrapper(editor).also { presentation = it }
    }

    fun update(probe: Probe) {
        this.probe = probe
        probeService.probeUpdater.mark(this)
    }

    fun applyText() {
        val probe = probe ?: return
        presentation?.update(probe)
    }
}
