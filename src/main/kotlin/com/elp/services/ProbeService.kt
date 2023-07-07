package com.elp.services

import com.elp.instrumentalization.ProbeUpdateController
import com.elp.instrumentalization.Runner
import com.elp.ui.LoadingNotification
import com.elp.ui.ProbePresentation
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.EditorNotificationPanel

@Service
class ProbeService: Disposable {
    var probes = mutableMapOf<String, List<ProbePresentation>>()

    val probeUpdater = ProbeUpdateController().start()
    val runner = Runner(mock = false).start()
    private var loading: LoadingNotification? = null

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
