package com.elp.instrumentalization

import com.elp.model.Example
import com.elp.util.clone
import com.elp.util.struct
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.psi.PsiFile
import com.jetbrains.cidr.lang.psi.OCStruct

object FileExampleInstrumentalization {
    fun run(
        example: Example,
        files: List<PsiFile>,
        consumer: (files: List<PsiFile>, modifications: List<Modification>) -> Unit
    ) {
        val clones = files.map { it.clone() }
        val modifications = clones.flatMap { collectModifications(it, example) }

        invokeLater {
            runWriteAction {
                executeCommand {
//                    for (toRemove in modifications.filterReplacements()) {
//                        toRemove.original!!.element.delete()
//                    }
//
//                    var anchor: PsiElement? = clazz.members.lastOrNull()
//                    for (modification in modifications) {
//                        anchor = clazz.addAfter(modification.added.element, anchor)
//                    }
                    consumer(clones, modifications)
                }
            }
        }
    }

    private fun collectModifications(file: PsiFile, example: Example): List<Modification> {
        val parentStruct = file.struct ?: return listOf()
        val replacedStruct = example.ownStructs.find { it.name == parentStruct.name } ?: return listOf()

        return replacedStruct.allMembers.map { replacedMember ->
            val clazzMember = parentStruct.allMembers.find { parentMember -> replacedMember equalsIgnoreFile parentMember }
            Modification(parentStruct, replacedMember, clazzMember)
        }
    }
}

data class Modification(
    val struct: OCStruct,
    val added: Member,
    val original: Member?) {

    val isReplacement = original != null
    val isAddition = original == null
}

fun List<Modification>.filterReplacements() = filter { it.isReplacement }
fun List<Modification>.filterAdditions() = filter { it.isAddition }

