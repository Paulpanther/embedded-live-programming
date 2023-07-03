package com.elp.actions

import com.elp.instrumentalization.Member
import com.elp.instrumentalization.asMember
import com.elp.instrumentalization.memberFields
import com.elp.instrumentalization.memberFunctions
import com.elp.model.Example
import com.elp.services.classService
import com.elp.services.exampleService
import com.elp.services.isExample
import com.elp.util.document
import com.elp.util.error
import com.elp.util.struct
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCDeclarator
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition

class CreateReplacementAction : PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "ExampleActions"
    override fun getText() = "Replace member"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (element.containingFile.isExample) return false
        project.classService.findClass(element.containingFile.virtualFile) ?: return false
        project.exampleService.activeExample ?: return false

        return true
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = element.containingFile
        val struct = file.struct ?: return
        val example = project.exampleService.activeExample ?: return

        val field = (element.parent as? OCDeclarator)?.parent as? OCDeclaration ?: return
        val function = field as? OCFunctionDeclaration
        val member = function?.asMember() ?: field.asMember()

        val exampleStruct = example.ownStructs.find { it.name == struct.name }
        if (exampleStruct == null) {
            ReplacementClassCreator.create(example, struct) { createReplacement(member, example, it) }
        } else {
            val exampleMembers = exampleStruct.memberFields + exampleStruct.memberFunctions
            if (exampleMembers.any { it equalsIgnoreFile member }) return project.error("Already replaced in example")
            val offset = exampleStruct.functionsStartOffset
            createReplacement(member, example, offset)
        }
    }

    private fun createReplacement(member: Member, example: Example, offset: Int) {
        val file = example.ownFile
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
