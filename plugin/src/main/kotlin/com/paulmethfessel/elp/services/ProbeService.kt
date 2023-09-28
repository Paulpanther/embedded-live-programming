package com.paulmethfessel.elp.services

import com.paulmethfessel.elp.execution.ProbeUpdateController
import com.paulmethfessel.elp.execution.Runner
import com.paulmethfessel.elp.ui.LoadingNotification
import com.paulmethfessel.elp.ui.ProbePresentation
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

@Service
class ProbeService: Disposable {
    // probes requested by CreateProbeAction. Will be deleted once realized
    var requestedUserProbes = mutableMapOf<String, MutableList<TextRange>>()
    // Copy of user-probes in probes. Stored because probes array is reset each refresh
    var userProbes = mutableMapOf<String, MutableList<ProbePresentation>>()
    var probes = mutableMapOf<String, MutableList<ProbePresentation>>()

    val probeUpdater = ProbeUpdateController().start()
    val runner = Runner(mock = false).start()
    private var loading: LoadingNotification? = null

    var lastExecutedHash = 0

    init {
        Disposer.register(this, runner)
        Disposer.register(this, probeUpdater)
    }

    fun showLoading(file: PsiFile) {
        stopLoading()
        loading = LoadingNotification.create(file)
    }

    fun stopLoading() {
        loading?.hide()
    }

    override fun dispose() = Unit
}

val probeService get() = service<ProbeService>()
