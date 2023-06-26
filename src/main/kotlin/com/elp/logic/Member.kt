package com.elp.logic

import com.elp.services.Example
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.types.OCStructType

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

        override fun toString() = "$staticStr$name$parameterString"
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

fun OCFunctionDefinition.asMember() = Member.Function(this)
fun OCDeclaration.asMember() = Member.Field(this)

val OCStruct.memberFunctions get() = children.filterIsInstance<OCFunctionDefinition>().map { it.asMember() }
val OCStruct.memberFields
    get() = children
        .filterIsInstance<OCDeclaration>()
        .filter { it !is OCFunctionDefinition && it.type !is OCStructType }
        .map { it.asMember() }

val Example.missingMembers get(): List<Member> {
    val originalClass = clazz.element
    val originalMembers = originalClass.memberFields + originalClass.memberFunctions
    val exampleClass = file.struct ?: return listOf()
    val exampleMembers = exampleClass.memberFields + exampleClass.memberFunctions
    return originalMembers.filter { exampleMembers.none(it::equalsIgnoreFile) }
}