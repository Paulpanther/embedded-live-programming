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

        val membersInExample = exampleClazz.memberFunctions + exampleClazz.memberFields
        val membersInClass = clazz.memberFunctions + clazz.memberFields
        val modifications = membersInExample.map { exampleMember ->
            val clazzMember = membersInClass.find { classMember -> exampleMember equalsIgnoreFile classMember }
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

fun OCFunctionDefinition.asMember() = Member.Function(this)
fun OCDeclaration.asMember() = Member.Field(this)

sealed class Member(
    val element: OCDeclaration
) {
    val isStatic = element.isStatic
    val file = element.containingFile.name
    abstract val name: String
    abstract val navigable: OpenFileDescriptor?

    class Function(
        function: OCFunctionDefinition
    ) : Member(function) {
        val parameters = function.parameters?.map { it.name } ?: listOf()
        override val name = function.name ?: "undefined"
        override val navigable = function.nameIdentifier?.navigable
        val type = function.type.name

        val parameterString = "(${parameters.joinToString(", ")})"

        override fun equalsIgnoreFile(other: Member) =
            super.equalsIgnoreFile(other) && other is Function && other.parameters == parameters

        override fun equals(other: Any?) =
            super.equals(other) && other is Function && other.parameters == parameters

        override fun hashCode() = super.hashCode() * 31 + parameters.hashCode()

        override fun toString() = "$staticStr$name$parameterString)"
    }

    class Field(
        field: OCDeclaration
    ) : Member(field) {
        val type = field.type.name
        override val name = field.declarators.firstOrNull()?.name ?: "undefined"
        override val navigable = field.declarators.firstOrNull()?.nameIdentifier?.navigable
        val value = field.declarators.firstOrNull()?.initializer?.text

        override fun equalsIgnoreFile(other: Member) =
            super.equalsIgnoreFile(other) && other is Field && other.type == type

        override fun equals(other: Any?) =
            super.equals(other) && other is Field && other.type == type

        override fun hashCode() = super.hashCode() * 31 + type.hashCode()

        override fun toString() = "$staticStr$type $name"
    }

    open infix fun equalsIgnoreFile(other: Member) =
        other.name == name && other.isStatic == isStatic

    override fun equals(other: Any?) =
        other is Member && other.file == file && other.name == name && other.isStatic == isStatic

    override fun hashCode() = (file.hashCode() * 31 + (name.hashCode())) * 31 + isStatic.hashCode()

    protected val staticStr = if (isStatic) "static " else ""
}

data class Modification(
    val added: Member,
    val original: Member?) {

    val isReplacement = original != null
    val isAddition = original == null
}

fun List<Modification>.filterReplacements() = filter { it.isReplacement }
fun List<Modification>.filterAdditions() = filter { it.isAddition }

val OCStruct.memberFunctions get() = children.filterIsInstance<OCFunctionDefinition>().map { it.asMember() }
val OCStruct.memberFields
    get() = children
        .filterIsInstance<OCDeclaration>()
        .filter { it !is OCFunctionDefinition && it.type !is OCStructType }
        .map { it.asMember() }

val PsiFile.struct get() = PsiTreeUtil.findChildOfType(this, OCStruct::class.java)
val PsiFile.structs get() = PsiTreeUtil.findChildrenOfType(this, OCStruct::class.java).toList()

val PsiElement.navigable get() = OpenFileDescriptor(project, containingFile.virtualFile, navigationElement.startOffset)
