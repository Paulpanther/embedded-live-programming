package com.elp.logic

import com.elp.Example
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.psi.OCClassDeclaration
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

        val exampleFunctions = PsiTreeUtil.findChildrenOfType(example.file, OCFunctionDefinition::class.java)
        val setupFunction = exampleFunctions.find { it.name == "setup" }
            ?: return project.error("Could not find function \"setup\" in example")

        val returnType = setupFunction.returnType as? OCPointerType
            ?: return project.error("Return type of function \"setup\" must be a pointer to class \"${clazz.name}\"")

        if (returnType.refType.name != clazz.name)
            return project.error("Return type of function \"setup\" must be a pointer to class \"${clazz.name}\"")

        if (!setupFunction.isStatic)
            return project.error("Function \"setup\" must be static")

        if (setupFunction.parameters?.isNotEmpty() == true)
            return project.error("Function \"setup\" must not have any parameters")

        val loopFunction = exampleFunctions.find { it.name == "loop" }
            ?: return project.error("Could not find function \"loop\" in example")

        if (loopFunction.parameters?.isNotEmpty() == true)
            return project.error("Function \"loop\" must not have any parameters")

        val setupMethod = OCElementFactory.codeFragment(setupFunction.text, project, clazz, false, false)
        val loopMethod = OCElementFactory.codeFragment(loopFunction.text, project, clazz, false, false)

        val clazzName = clazz.name
            ?: return project.error("Example class must have a name")

        invokeLater {
            runWriteAction {
                executeCommand {
                    val addedMethod = clazz.addAfter(setupMethod, clazz.members.lastOrNull())
                    clazz.addAfter(loopMethod, addedMethod)
                    consumer(clone, clazzName)
                }
            }
        }
    }

}
