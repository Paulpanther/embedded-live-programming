package com.elp.instrumentalization

import com.elp.services.ExampleService
import com.elp.services.classService
import com.elp.services.exampleService
import com.elp.services.probeService
import com.elp.util.clone
import com.elp.util.error
import com.elp.util.struct
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hints.InlayHintsPassFactory
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.psi.OCStruct

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

                    val mainStruct = files
                        .mapNotNull { it.struct }
                        .find { it.name == example.ownMainStruct.name } ?: return@executeCommand
                    probeService.runner.executeFiles(files + createRunnerFile(project, mainStruct))

                    @Suppress("UnstableApiUsage")
                    InlayHintsPassFactory.forceHintsUpdateOnNextPass()
                    for (file in example.referencedFiles) {
                        DaemonCodeAnalyzer.getInstance(project).restart(file)
                    }
                }
            }
        }
    }

    private fun createRunnerFile(project: Project, mainStruct: OCStruct): PsiFile {
        val structName = mainStruct.name ?: "undefined"

        val constructors = mainStruct.memberFunctions
            .filter { it.name == structName }
        if (constructors.isNotEmpty() && constructors.none { it.parameters.isEmpty() })
            project.error("Executed class needs a no-arg constructor")

        val setup = mainStruct.memberFunctions.find { it.name == "setup" && it.parameters.isEmpty() }
        val loop = mainStruct.memberFunctions.find { it.name == "loop" && it.parameters.isEmpty() }
        val liveLoop = mainStruct.memberFunctions.find { it.name == "liveLoop" && it.parameters.isEmpty() }

        val m = "____m"
        val setupString = setup?.let { "$m->setup();" } ?: ""
        val loopString = liveLoop?.let { "$m->liveLoop();" }
            ?: loop?.let { "$m->loop();" }
            ?: project.error("Executed class needs a liveLoop function")

        return PsiFileFactory.getInstance(project).createFileFromText(
            OCLanguage.getInstance(), """
            #include "${mainStruct.containingFile.name}"
            #include "code.h"
            
            $structName* $m = nullptr;
            
            void setup() {
                $m = new $structName();
                $setupString
            }
            
            void loop() {
                $loopString
            }
        """.trimIndent()
        ).apply { this.name = "________main.cpp" }
    }
}
