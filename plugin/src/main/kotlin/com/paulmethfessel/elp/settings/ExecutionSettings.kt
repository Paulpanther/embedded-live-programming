package com.paulmethfessel.elp.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.paulmethfessel.elp.settings.ExecutionSettings",
    storages = [Storage("MusePlugin.xml")])
class ExecutionSettings: PersistentStateComponent<ExecutionSettings> {
    var port = "/dev/ttyUSB0";
    var mock = false
    var backend: String = System.getenv("ELP_RUNNER_PATH")
    var userCodeWrapper: String = System.getenv("ELP_USER_CODE_PATH")
    var fileFilter = ""

    override fun getState() = this

    override fun loadState(state: ExecutionSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

}

val settings get() = service<ExecutionSettings>()