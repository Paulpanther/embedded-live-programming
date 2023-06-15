package com.elp.logic

import com.elp.error
import com.elp.services.Example
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition
import com.jetbrains.cidr.lang.psi.OCStruct

object FileExampleInstrumentalization {
    fun run(example: Example, file: PsiFile, consumer: (file: PsiFile, className: String, modifications: List<Modification>) -> Unit) {
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

        val modifications = functionsToRemove.map { Modification.ReplaceFunction(it.signature) }

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
}

val OCFunctionDefinition.signature get() = Signature(this)

data class Signature(
    val file: String,
    val name: String?,
    val parameters: List<String>,
    val isStatic: Boolean
) {
    constructor(function: OCFunctionDeclaration)
            : this(function.containingFile.name, function.name, function.parameters?.map { it.name } ?: listOf(), function.isStatic)

    infix fun equalsIgnoreFile(o: Signature) = name == o.name && parameters == o.parameters && isStatic == o.isStatic

    override fun toString() = "${if (isStatic) "static " else ""}${name}(${parameters.joinToString(", ")})"
}

sealed class Modification {
    class ReplaceFunction(val signature: Signature): Modification()
}
