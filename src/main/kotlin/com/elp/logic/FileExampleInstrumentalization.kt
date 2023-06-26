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

data class Modification(
    val added: Member,
    val original: Member?) {

    val isReplacement = original != null
    val isAddition = original == null
}

fun List<Modification>.filterReplacements() = filter { it.isReplacement }
fun List<Modification>.filterAdditions() = filter { it.isAddition }

val PsiFile.struct get() = PsiTreeUtil.findChildOfType(this, OCStruct::class.java)

val PsiElement.navigable get() = OpenFileDescriptor(project, containingFile.virtualFile, navigationElement.startOffset)
