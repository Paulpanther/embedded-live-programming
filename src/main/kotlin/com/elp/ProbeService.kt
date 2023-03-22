package com.elp

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer

@Service
class ProbeService: Disposable {
    var probes = mutableListOf<ProbePresentation>()
    val runner = Runner().start().also {
        Disposer.register(this, it)
    }

    override fun dispose() = Unit
}

val probeService get() = service<ProbeService>()
