package com.elp.actions

import com.elp.document
import com.elp.error
import com.elp.logic.*
import com.elp.services.Example
import com.elp.services.classService
import com.elp.services.exampleService
import com.elp.services.isExample
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCDeclarator
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition

class CreateReplacementAction : PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "ExampleActions"
    override fun getText() = "Replace member"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (element.containingFile.isExample) return false
        val clazz = project.classService.findClass(element.containingFile.virtualFile) ?: return false
        val example = project.exampleService.activeExample ?: return false
        if (example !in clazz.examples) return false

        return true
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = element.containingFile
        val clazz = project.classService.findClass(file.virtualFile) ?: return
        val example = project.exampleService.activeExample ?: return
        if (example !in clazz.examples) return project.error("Class is not part of active example")

        val field = (element.parent as? OCDeclarator)?.parent as? OCDeclaration ?: return
        val function = field as? OCFunctionDefinition
        val member = function?.asMember() ?: field.asMember()
        val exampleStruct = example.file.struct ?: return
        val exampleMembers = exampleStruct.memberFields + exampleStruct.memberFunctions
        if (exampleMembers.any { it equalsIgnoreFile member }) return project.error("Already replaced in example")

        createReplacement(member, example)
    }

    private fun createReplacement(member: Member, example: Example) {
        val file = example.file
        val offset = file.struct?.functionsStartOffset ?: error("Could not find struct")
        val doc = file.document ?: return

        runWriteAction {
            executeCommand {
                val memberString = when (member) {
                    is Member.Function -> "\t${member.type} ${member.name}${member.parameterString} {}\n"
                    is Member.Field -> "\t${member.type} ${member.name} = ;\n"
                }
                doc.insertString(offset, memberString)
                example.editor.requestFocusInWindow()
                example.editor.editor?.caretModel?.moveToOffset(offset + memberString.length - 2)
            }
        }
    }
}
