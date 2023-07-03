package com.elp.instrumentalization

import com.elp.model.Example
import com.elp.services.ExampleService
import com.elp.services.classService
import com.elp.services.exampleService
import com.elp.services.probeService
import com.elp.util.clone
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hints.InlayHintsPassFactory
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.cidr.lang.OCLanguage

object InstrumentalizationManager {
    fun registerOnActiveExampleChange(exampleService: ExampleService) {
        exampleService.onActiveExampleChanged.register { run(exampleService.project) }
    }

    fun run(project: Project) {
        val files = project.classService.classes.map { it.file.clone() }
        val example = project.exampleService.activeExample ?: return
        val exampleFile = example.ownFile.clone()

        invokeLater {
            runWriteAction {
                executeCommand {
                    FileProbeInstrumentalization.run(files + exampleFile)
                    FileExampleInstrumentalization.run(example, files, exampleFile)

                    probeService.runner.executeFiles(files + createRunnerFile(project, example))

                    @Suppress("UnstableApiUsage")
                    InlayHintsPassFactory.forceHintsUpdateOnNextPass()
                    for (file in example.referencedFiles) {
                        DaemonCodeAnalyzer.getInstance(project).restart(file)
                    }
                }
            }
        }
    }

    private fun createRunnerFile(project: Project, example: Example): PsiFile {
        val structName = example.ownMainStruct.name ?: "undefined"
        return PsiFileFactory.getInstance(project).createFileFromText(
            OCLanguage.getInstance(), """
            #include "${example.parentFile.name}"
            #include "code.h"
            
            $structName* ____m = nullptr;
            
            void setup() {
                ____m = new $structName();
            }
            
            void loop() {
                ____m->loop();
            }
        """.trimIndent()
        ).apply { this.name = "________main.cpp" }
    }
}
