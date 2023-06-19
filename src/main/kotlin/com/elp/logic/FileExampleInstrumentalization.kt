package com.elp.logic

import com.elp.error
import com.elp.services.Example
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
import com.jetbrains.cidr.lang.psi.OCDeclaration
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
        val project = file.project

        val clazz = clone.struct
        if (clazz == null || clazz.name != example.clazz.name) {
            return project.error("Could not find class \"${example.clazz.name}\" in file \"${file.name}\"")
        }
        val exampleClazz = example.file.struct ?: return project.error("Could not find class in example")

        val newFunctions = exampleClazz.functions
        val oldFunctions = clazz.functions
        val mapping =
            newFunctions.associateWith { newFunction -> oldFunctions.find { oldFunction -> newFunction.signature equalsIgnoreFile oldFunction.signature } }
        val functionsToRemove = mapping.filter { it.value != null }
        val functionsNewlyAdded = mapping.keys.filter { mapping[it] == null }

        val membersInExample = exampleClazz.fields
        val membersInClass = clazz.fields
        val membersMapping =
            membersInExample.associateWith { exampleMember -> membersInClass.find { classMember -> exampleMember.signature equalsIgnoreFile classMember.signature } }
        val membersToReplace = membersMapping.filter { it.value != null }
        val membersToAdd = membersMapping.keys.filter { membersMapping[it] == null }

        val modifications =
            functionsToRemove.map { Modification.ReplaceFunction(it.value!!.signature, it.key) } +
                    functionsNewlyAdded.map { Modification.AddedFunction(it.signature, it) } +
                    membersToReplace.map {
                        Modification.ReplaceMember(
                            it.value!!.signature,
                            it.key.declarators.firstOrNull()?.initializer?.text,
                            it.key)
                    } +
                    membersToAdd.map { Modification.AddedMember(it.signature, it) }

        invokeLater {
            runWriteAction {
                executeCommand {
                    for (toRemove in functionsToRemove) {
                        toRemove.value!!.delete()
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
        constructor(function: OCFunctionDefinition) : this(
            function.containingFile.name,
            function.name,
            function.parameters?.map { it.name } ?: listOf(),
            function.isStatic)

        override fun equalsIgnoreFile(other: Signature) =
            super.equalsIgnoreFile(other) && other is Function && other.parameters == parameters

        override fun equals(other: Any?) =
            super.equals(other) && other is Function && other.parameters == parameters

        override fun hashCode() = super.hashCode() * 31 + parameters.hashCode()

        override fun toString() = "$staticStr$name(${parameters.joinToString(", ")})"
    }

    class Field(
        file: String,
        name: String?,
        val type: String,
        isStatic: Boolean
    ) : Signature(file, name, isStatic) {
        constructor(field: OCDeclaration) : this(
            field.containingFile.name,
            field.declarators.firstOrNull()?.name,
            field.type.name,
            field.isStatic
        )

        override fun equalsIgnoreFile(other: Signature) =
            super.equalsIgnoreFile(other) && other is Field && other.type == type

        override fun equals(other: Any?) =
            super.equals(other) && other is Field && other.type == type

        override fun hashCode() = super.hashCode() * 31 + type.hashCode()

        override fun toString() = "$staticStr$type $name"
    }

    open infix fun equalsIgnoreFile(other: Signature) =
        other.name == name && other.isStatic == isStatic

    override fun equals(other: Any?) =
        other is Signature && other.file == file && other.name == name && other.isStatic == isStatic

    override fun hashCode() = (file.hashCode() * 31 + (name?.hashCode() ?: 0)) * 31 + isStatic.hashCode()

    protected val staticStr = if (isStatic) "static " else ""
}

sealed class Modification {
    class ReplaceFunction(
        val signature: Signature.Function,
        val original: OCFunctionDefinition
    ) : Modification()

    class AddedFunction(
        val signature: Signature.Function,
        val original: OCFunctionDefinition
    ) : Modification()

    class ReplaceMember(
        val signature: Signature.Field,
        val value: String?,
        val original: OCDeclaration
    ) : Modification()

    class AddedMember(
        val signature: Signature.Field,
        val original: OCDeclaration
    ) : Modification()
}

val OCStruct.functions get() = children.filterIsInstance<OCFunctionDefinition>()
val OCStruct.fields
    get() = children
        .filterIsInstance<OCDeclaration>()
        .filter { it !is OCFunctionDefinition && it.type !is OCStructType }

val PsiFile.struct get() = PsiTreeUtil.findChildOfType(this, OCStruct::class.java)

val PsiElement.navigable get() = OpenFileDescriptor(project, containingFile.virtualFile, navigationElement.startOffset)
