package com.elp.logic

import com.elp.probeService
import com.elp.ui.ProbePresentation
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class ProbeUpdateController: Disposable {
    private val rateMS = 100L
    private var task: TimerTask? = null

    fun start(): ProbeUpdateController {
        task = Timer().scheduleAtFixedRate(0L, rateMS) {
            update()
        }
        return this
    }

    fun mark(probe: ProbePresentation) {
        probe.markedForUpdate = true
    }

    private fun update() {
        val probes = probeService.probes.values.flatten().filter { it.markedForUpdate }
        if (probes.isEmpty()) return

        probes.forEach { it.markedForUpdate = false }

        invokeLater {
            probes.forEach {
                it.applyText()
                it.editor?.repaint(it.range.startOffset, it.range.endOffset)
            }
        }
    }

    override fun dispose() {
        task?.cancel()
    }
}
