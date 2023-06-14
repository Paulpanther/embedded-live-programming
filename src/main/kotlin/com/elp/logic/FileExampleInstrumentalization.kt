package com.elp.logic

import com.elp.services.Example
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.types.OCPointerType
import com.jetbrains.cidr.lang.util.OCElementFactory

object FileExampleInstrumentalization {
    fun run(example: Example, file: PsiFile, consumer: (file: PsiFile, className: String) -> Unit) {
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
        val mapping = newFunctions.associateWith { newFunction -> oldFunctions.find { oldFunction -> newFunction.signature == oldFunction.signature } }
        val functionsToRemove = mapping.values.filterNotNull()

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
                    consumer(clone, clazz.name!!)
                }
            }
        }
    }
}

private val OCFunctionDefinition.signature get() = Signature(this)

private data class Signature(
    val name: String?,
    val parameters: List<String>,
    val isStatic: Boolean
) {
    constructor(function: OCFunctionDeclaration)
            : this(function.name, function.parameters?.map { it.name } ?: listOf(), function.isStatic)

    override fun toString() = "${if (isStatic) "static " else ""}${name}(${parameters.joinToString(", ")})"
}
