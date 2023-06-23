package com.elp.events

import com.elp.error
import com.elp.logic.FileExampleInstrumentalization
import com.elp.logic.FileProbeInstrumentalization
import com.elp.openProject
import com.elp.services.classService
import com.elp.services.exampleService
import com.elp.services.probeService
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hints.InlayHintsPassFactory
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.cidr.lang.OCLanguage

class FileChangeListener : FileDocumentManagerListener {
    init {
        FileOpenListener.register()
    }

    override fun beforeDocumentSaving(document: Document) {
        val project = openProject ?: return
        val psiFiles = project.classService.classes.map { it.file }

        FileProbeInstrumentalization.run(psiFiles) { oldToNewFiles ->
            buildRunnableProject(project, oldToNewFiles) {
                probeService.runner.executeFiles(it)
            }
        }
    }

    private fun buildRunnableProject(
        project: Project,
        oldToNewFiles: Map<PsiFile, PsiFile>,
        finishedFilesConsumer: (files: List<PsiFile>) -> Unit
    ) {
        val example = project.exampleService.activeExample
            ?: return project.error("Create an example to run the project")

        val file = example.clazz.file
        val newFile = oldToNewFiles[file]
            ?: return project.error("Could not generate instrumentalized classes")

        FileExampleInstrumentalization.run(example, newFile) { exampleFile, exampleClassName, modifications ->
            // Replace file in list
            val finishedFiles = oldToNewFiles.map { (oldFile, newFile) ->
                if (oldFile == file) exampleFile else newFile
            }
            finishedFilesConsumer(finishedFiles + createRunnerFile(project, exampleClassName, exampleFile.name))

            example.modifications = modifications
            @Suppress("UnstableApiUsage")
            InlayHintsPassFactory.forceHintsUpdateOnNextPass()
            DaemonCodeAnalyzer.getInstance(project).restart(file)
        }
    }

    private fun createRunnerFile(project: Project, exampleClassName: String, exampleFileName: String) =
        PsiFileFactory.getInstance(project).createFileFromText(OCLanguage.getInstance(), """
            #include "$exampleFileName"
            #include "code.h"
            
            $exampleClassName* ____m = nullptr;
            
            void setup() {
                ____m = $exampleClassName::setup();
            }
            
            void loop() {
                ____m->loop();
            }
        """.trimIndent())
            .apply { name = "________main.cpp" }
}

