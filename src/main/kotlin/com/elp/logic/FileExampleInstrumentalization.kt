package com.elp.logic

import com.elp.error
import com.elp.services.Example
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.types.OCStructType

object FileExampleInstrumentalization {
    fun run(
        example: Example,
        file: PsiFile,
        consumer: (file: PsiFile, className: String, modifications: List<Modification>) -> Unit
    ) {
        val clone = PsiFileFactory
            .getInstance(file.project)
            .createFileFromText(file.name, OCLanguage.getInstance(), file.text)

        val classes = PsiTreeUtil.findChildrenOfType(clone, OCStruct::class.java)
        val project = file.project

        val clazz = classes
            .find { it.name == example.clazz.name }
            ?: return project.error("Could not find class \"${example.clazz.name}\" in file \"${file.name}\"")

        val newFunctions = PsiTreeUtil.findChildrenOfType(example.file, OCFunctionDefinition::class.java)
        val oldFunctions = PsiTreeUtil.findChildrenOfType(clone, OCFunctionDefinition::class.java)
        val mapping =
            newFunctions.associateWith { newFunction -> oldFunctions.find { oldFunction -> newFunction.signature equalsIgnoreFile oldFunction.signature } }
        val functionsToRemove = mapping.values.filterNotNull()
        val functionsNewlyAdded = mapping.keys.filter { mapping[it] == null }

        val membersInExample = getMembers(example.file)
        val membersInClass = getMembers(clone)
        val membersMapping =
            membersInExample.associateWith { exampleMember -> membersInClass.find { classMember -> exampleMember.signature equalsIgnoreFile classMember.signature } }
        val membersToReplace = membersMapping.values.filterNotNull()
        val membersToAdd = membersMapping.keys.filter { membersMapping[it] == null }

        val modifications =
            functionsToRemove.map { Modification.ReplaceFunction(it.signature) } +
                    functionsNewlyAdded.map { Modification.AddedFunction(it.signature) } +
                    membersToReplace.map { Modification.ReplaceMember(it.signature) } +
                    membersToAdd.map { Modification.AddedMember(it.signature) }

        invokeLater {
            runWriteAction {
                executeCommand {
                    for (toRemove in functionsToRemove) {
                        toRemove.delete()
                    }

                    var anchor: PsiElement? = clazz.members.lastOrNull()
                    for (function in newFunctions) {
                        anchor = clazz.addAfter(function, anchor)
                    }
                    consumer(clone, clazz.name!!, modifications)
                }
            }
        }
    }

    private fun getMembers(file: PsiFile): List<OCDeclaration> {
        return PsiTreeUtil.findChildrenOfType(file, OCDeclaration::class.java)
            .filter { it !is OCFunctionDefinition && it.type !is OCStructType }
    }
}

val OCFunctionDefinition.signature get() = Signature.Function(this)
val OCDeclaration.signature get() = Signature.Field(this)

sealed class Signature(
    val file: String,
    val name: String?,
    val isStatic: Boolean,
) {
    class Function(
        file: String,
        name: String?,
        val parameters: List<String>,
        isStatic: Boolean
    ) : Signature(file, name, isStatic) {
        constructor(function: OCFunctionDefinition): this(
            function.containingFile.name,
            function.name,
            function.parameters?.map { it.name } ?: listOf(),
            function.isStatic)
    }

    class Field(
        file: String,
        name: String?,
        val type: String,
        isStatic: Boolean
    ) : Signature(file, name, isStatic) {
        constructor(field: OCDeclaration): this(
            field.containingFile.name,
            field.declarators.firstOrNull()?.name,
            field.type.name,
            field.isStatic)

        override fun equals(o: Any?) =
            o is Field && o.file == file && o.name == name && o.isStatic == isStatic
    }

    override fun equals(o: Any?) =
        o is Signature && o.file == file && o.name == name && o.isStatic == isStatic
}

sealed class Modification {
    class ReplaceFunction(val signature: Signature) : Modification()
    class AddedFunction(val signature: Signature) : Modification()
}
