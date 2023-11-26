@file:Suppress("UnstableApiUsage")

package com.paulmethfessel.elp.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import java.io.File

class ExecutionSettingsView: Configurable {
    private val panel by lazy {
        panel {
            row {
                textField()
                    .label("USB port")
                    .bindText(settings::port)
                    .validationOnInput { if (it.text.isBlank()) error("Missing port") else null }
            }
            group("Developer Settings") {
                row {
                    checkBox("Mock probes")
                        .comment("Requires restart of IDE to come into effect")
                        .bindSelected(settings::mock)
                }
                row {
                    textFieldWithBrowseButton("Backend Path")
                        .label("Backend path")
                        .bindText(settings::backend)
                        .validationOnInput {
                            val file = File(it.text)
                            if (!settings.mock && !file.exists()) error("File does not exist")
                            else null
                        }
                }
                row {
                    textFieldWithBrowseButton("User Code Path")
                        .label("User code path")
                        .bindText(settings::userCodeWrapper)
                        .validationOnInput {
                            val file = File(it.text)
                            if (!settings.mock && !file.exists()) error("File does not exist")
                            else null
                        }
                }
            }
        }
    }

    override fun createComponent() = panel

    override fun isModified() = panel.isModified()

    override fun apply() {
        panel.apply()
    }

    override fun getDisplayName() = "MµSE"
}