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
import com.jetbrains.cidr.lang.util.OCElementFactory

object FileExampleInstrumentalization {
    fun run(example: Example, file: PsiFile) {
        val clone = PsiFileFactory
            .getInstance(file.project)
            .createFileFromText(file.name, OCLanguage.getInstance(), file.text)

        val classes = PsiTreeUtil.findChildrenOfType(clone, OCStruct::class.java)
        val project = file.project

        val clazz = classes
            .find { it.name == example.activeClass?.name }
            ?: return project.error("Could not find class \"${example.activeClass?.name}\" in file \"${file.name}\"")

        val exampleFunctions = PsiTreeUtil.findChildrenOfType(example.file, OCFunctionDefinition::class.java)
        val setupFunction = exampleFunctions.find { it.name == "setup" }
            ?: return project.error("Could not find function \"setup\" in example")

        if (setupFunction.returnType.name != clazz.name)
            return project.error("Return type \"${setupFunction.returnType.name}\" of function \"setup\" does not match class \"${clazz.name}\"")

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

        invokeLater {
            runWriteAction {
                executeCommand {
                    val addedMethod = clazz.addAfter(setupMethod, clazz.members.lastOrNull())
                    clazz.addAfter(loopMethod, addedMethod)
                }
            }
        }
    }

    private fun Project.error(content: String) {
        NotificationGroupManager
            .getInstance()
            .getNotificationGroup("Embedded Live Programming Notification Group")
            .createNotification(content, NotificationType.ERROR)
            .notify(this)
    }
}
