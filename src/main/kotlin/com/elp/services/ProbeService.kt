package com.elp.services

import com.elp.instrumentalization.ProbeUpdateController
import com.elp.instrumentalization.Runner
import com.elp.ui.ProbePresentation
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer

@Service
class ProbeService: Disposable {
    var probes = mutableMapOf<String, List<ProbePresentation>>()

    val probeUpdater = ProbeUpdateController().start()
    val runner = Runner(mock = true).start()

    init {
        Disposer.register(this, runner)
        Disposer.register(this, probeUpdater)
    }

    override fun dispose() = Unit
}

val probeService get() = service<ProbeService>()
