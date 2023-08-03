package com.elp.execution

import com.elp.model.Example
import com.elp.services.ExampleService
import com.elp.services.classService
import com.elp.services.exampleService
import com.elp.services.probeService
import com.elp.util.*
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hints.InlayHintsPassFactory
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.parentOfType
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition
import com.jetbrains.cidr.lang.psi.OCReturnStatement
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.types.OCVoidType

object CodeExecutionManager {
    fun registerOnActiveExampleChange(exampleService: ExampleService) {
        exampleService.onActiveExampleChanged.register { run(exampleService.project) }
    }

    fun run(project: Project) {
        val original = project.classService.classes.map { it.file }
        val cppFiles = project.classService.cppFiles.mapNotNull { it.getPsiFile(project) }
        val example = project.exampleService.activeExample ?: return

        // store hash to not execute same codebase multiple times
        val hash = CurrentFileState(original, example).hashCode()
        if (probeService.lastExecutedHash == hash) return
        probeService.lastExecutedHash = hash

        invokeLater {
            runWriteAction {
                executeCommand {
                    logTime("Start ExecutionManager")
                    val exampleFile = example.ownFile.clone()

                    // check if files contain no errors
                    if (!checkFiles(original + exampleFile)) return@executeCommand

                    val files = original.map { it.clone() }

                    logTime("Start Probe Run")
                    FileProbeInjection.run(files + exampleFile)
                    logTime("Start Example Run")
                    FileReplacementInjection.run(example, files, exampleFile)

                    val mainStruct = files
                        .mapNotNull { it.struct }
                        .find { it.name == example.ownMainStruct.name } ?: return@executeCommand
                    val runner = createRunnerFile(project, mainStruct)

                    // Write Action is finished
                    invokeLater {
                        probeService.runner.executeFiles(files + runner + cppFiles)

                        logTime("Update Presentation")
                        @Suppress("UnstableApiUsage")
                        InlayHintsPassFactory.forceHintsUpdateOnNextPass()
                        for (file in example.referencedFiles) {
                            DaemonCodeAnalyzer.getInstance(project).restart(file)
                        }
                        logTime("End")
                    }
                }
            }
        }
    }

    private fun checkFiles(files: List<PsiFile>): Boolean {
        val project = files.firstOrNull()?.project ?: return false
        for (file in files) {
            if (file.childrenOfType<PsiErrorElement>().isNotEmpty()) {
                project.error("Please fix errors before saving")
                return false
            }

            val containsEmptyFunctions = file.childrenOfType<OCFunctionDefinition>().any { func ->
                val struct = func.parentOfType<OCStruct>() ?: return@any false
                func.name != struct.name && func.returnType !is OCVoidType && func.childOfType<OCReturnStatement>() == null
            }
            if (containsEmptyFunctions) {
                project.error("Please fix empty functions before saving")
                return false
            }
        }
        return true
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
            #include <memory>
            
            std::unique_ptr<$structName> $m;
            
            void setup() {
                $m = std::make_unique<$structName>();
                $setupString
            }
            
            void loop() {
                $loopString
            }
        """.trimIndent()
        ).apply { this.name = "________main.cpp" }
    }
}

class ClonedFile(val original: PsiFile, private val clone: PsiFile): PsiFile by clone {
    fun originalElement(element: PsiElement): PsiElement? {
        return original.childAtRangeOfType(element.textRange, element::class)
    }
}

fun PsiFile.clone() = ClonedFile(this, PsiFileFactory
    .getInstance(project)
    .createFileFromText(name, OCLanguage.getInstance(), text))

data class CurrentFileState(
    val files: List<Pair<String, String>>,
) {
    constructor(files: List<PsiFile>, example: Example): this((files + example.ownFile).map { it.virtualFile.path to it.text })
}
