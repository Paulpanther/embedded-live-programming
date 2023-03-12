package com.elp

import com.intellij.codeInsight.hints.presentation.TextInlayPresentation
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
class ProbeService {
    var p: TextInlayPresentation? = null
}

val probeService get() = service<ProbeService>()
