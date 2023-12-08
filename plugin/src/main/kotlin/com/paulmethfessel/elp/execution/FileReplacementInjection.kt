package com.paulmethfessel.elp.execution

import com.paulmethfessel.elp.model.Example
import com.paulmethfessel.elp.util.structs
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.cidr.lang.parser.OCElementType
import com.jetbrains.cidr.lang.parser.OCElementTypes
import com.jetbrains.cidr.lang.parser.OCTokenTypes
import com.jetbrains.cidr.lang.psi.OCBlockStatement
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.psi.impl.OCLazyBlockStatementImpl
import com.jetbrains.cidr.lang.symbols.OCVisibility
import com.jetbrains.cidr.lang.util.OCElementFactory
import com.paulmethfessel.elp.services.classService
import com.paulmethfessel.elp.util.childOfType
import com.paulmethfessel.elp.util.document
import com.paulmethfessel.elp.util.getPsiFile
import java.io.File

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
            var anchor: PsiElement = struct.members.lastOrNull() ?: struct
            val cppFile = files.find { it.name == struct.containingFile.virtualFile.nameWithoutExtension + ".cpp" }
            val mods = modifications.filter { it.originalStruct == struct }
            if (mods.isEmpty()) continue

            val public = OCElementFactory.create(OCVisibility.PUBLIC.elementType!!, struct)
            val colon = OCElementFactory.create(OCElementType(OCTokenTypes.COLON.debugName, OCTokenTypes.COLON.name), struct)
            anchor = struct.addAfter(public, anchor)
            anchor = struct.addAfter(colon, anchor)

            for (modification in mods) {
                val elem = modification.added.element
                if (cppFile == null || elem !is OCFunctionDefinition) {
                    anchor = struct.addAfter(elem, anchor)
                } else {
                    val declarationText =
                        "${elem.typeElement!!.text} ${elem.declarators.joinToString(" ") { it.text }}"
                    val definitionText =
                        "${elem.typeElement!!.text} ${struct.name}::${elem.declarators.joinToString(" ") { it.text }} {}"
                    val declaration = OCElementFactory.declarationFromText(declarationText, struct.containingFile)
                    val definition = OCElementFactory.declarationFromText(definitionText, cppFile, true)
                    definition.childOfType<OCBlockStatement>()!!.replace(elem.childOfType<OCBlockStatement>()!!)
                    anchor = struct.addAfter(declaration, anchor)
                    cppFile.add(definition)
                }
            }

//            val doc = struct.containingFile.document ?: error("Could not find doc")
//            val text = doc.text
//            // TODO hack
//            struct.containingFile.document?.setText(text.substring(0, begin) + "public:" + text.substring(begin))
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

