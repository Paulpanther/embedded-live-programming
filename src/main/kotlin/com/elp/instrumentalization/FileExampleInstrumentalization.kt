package com.elp.instrumentalization

import com.elp.model.Example
import com.elp.util.struct
import com.elp.util.structs
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.cidr.lang.psi.OCStruct

object FileExampleInstrumentalization {
    fun run(example: Example, files: List<PsiFile>, exampleFile: PsiFile) {
        val modifications = files.flatMap { collectModifications(it, exampleFile) }
        executeModifications(files, modifications)
        example.modifications = modifications
    }

    private fun executeModifications(files: List<PsiFile>, modifications: List<Modification>) {
        for (toRemove in modifications.filterReplacements()) {
            toRemove.original!!.element.delete()
        }
        val structs = files.mapNotNull { it.struct }
        for (struct in structs) {
            var anchor: PsiElement? = struct.members.lastOrNull()
            for (modification in modifications.filter { it.originalStruct == struct }) {
                anchor = struct.addAfter(modification.added.element, anchor)
            }
        }
    }

    private fun collectModifications(file: PsiFile, exampleFile: PsiFile): List<Modification> {
        val parentStruct = file.struct ?: return listOf()
        val replacedStruct = exampleFile.structs.find { it.name == parentStruct.name } ?: return listOf()

        return replacedStruct.allMembers.map { replacedMember ->
            val clazzMember =
                parentStruct.allMembers.find { parentMember -> replacedMember equalsIgnoreFile parentMember }
            Modification(parentStruct, replacedMember, clazzMember)
        }
    }
}

data class Modification(
    val originalStruct: OCStruct,
    val added: Member,
    val original: Member?
) {

    val isReplacement = original != null
    val isAddition = original == null
}

fun List<Modification>.filterReplacements() = filter { it.isReplacement }
fun List<Modification>.filterAdditions() = filter { it.isAddition }

