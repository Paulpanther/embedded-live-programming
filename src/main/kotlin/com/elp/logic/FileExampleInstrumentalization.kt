package com.elp.logic

import com.elp.model.Example
import com.elp.util.error
import com.elp.util.struct
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.psi.OCStruct

object FileExampleInstrumentalization {
    fun run(
        example: Example,
        file: PsiFile,
        consumer: (file: PsiFile, className: String, modifications: List<Modification>) -> Unit
    ) {
        val clone = PsiFileFactory
            .getInstance(file.project)
            .createFileFromText(file.name, OCLanguage.getInstance(), file.text)
        val project = file.project

        val clazz = clone.struct
        if (clazz == null || clazz.name != example.parentClazz.name) {
            return project.error("Could not find class \"${example.parentClazz.name}\" in file \"${file.name}\"")
        }
        val exampleClazz = example.ownMainStruct

        val modifications = exampleClazz.allMembers.map { exampleMember ->
            val clazzMember = clazz.allMembers.find { classMember -> exampleMember equalsIgnoreFile classMember }
            Modification(exampleMember, clazzMember)
        }

        invokeLater {
            runWriteAction {
                executeCommand {
                    for (toRemove in modifications.filterReplacements()) {
                        toRemove.original!!.element.delete()
                    }

                    var anchor: PsiElement? = clazz.members.lastOrNull()
                    for (modification in modifications) {
                        anchor = clazz.addAfter(modification.added.element, anchor)
                    }
                    consumer(clone, clazz.name!!, modifications)
                }
            }
        }
    }
}

data class Modification(
    val added: Member,
    val original: Member?) {

    val isReplacement = original != null
    val isAddition = original == null
}

fun List<Modification>.filterReplacements() = filter { it.isReplacement }
fun List<Modification>.filterAdditions() = filter { it.isAddition }

