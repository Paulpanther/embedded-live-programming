package com.paulmethfessel.elp.execution

import com.paulmethfessel.elp.model.Example
import com.paulmethfessel.elp.util.structs
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.cidr.lang.psi.OCStruct

object FileReplacementInjection {
    fun run(example: Example, files: List<PsiFile>, exampleFile: PsiFile) {
        // finds all replacements
        val modifications = files.flatMap { collectModifications(it, exampleFile) }

        // Rewrites original files to replace methods and fields
        executeModifications(files, modifications)

        // Stores replacements to display them as inlays later
        example.modifications = modifications
    }

    private fun executeModifications(files: List<PsiFile>, modifications: List<Modification>) {
        for (toRemove in modifications.filterReplacements()) {
            toRemove.original!!.element.delete()
        }
        val structs = files.flatMap { it.structs }
        for (struct in structs) {
            var anchor: PsiElement? = struct.members.lastOrNull()
            for (modification in modifications.filter { it.originalStruct == struct }) {
                anchor = struct.addAfter(modification.added.element, anchor)
            }
        }
    }

    private fun collectModifications(file: PsiFile, exampleFile: PsiFile): List<Modification> {
        val parentStructs = file.structs
        val modifications = mutableListOf<Modification>()

        for (parentStruct in parentStructs) {
            val replacedStruct = exampleFile.structs.find { it.name == parentStruct.name } ?: continue

            modifications += replacedStruct.allMembers.map { replacedMember ->
                val clazzMember =
                    parentStruct.allMembers.find { parentMember -> replacedMember equalsIgnoreFile parentMember }
                Modification(parentStruct, replacedMember, clazzMember)
            }
        }
        return modifications
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

