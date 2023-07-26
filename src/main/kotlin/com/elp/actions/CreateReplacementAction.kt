package com.elp.actions

import com.elp.execution.Member
import com.elp.execution.asMember
import com.elp.execution.memberFields
import com.elp.execution.memberFunctions
import com.elp.model.Example
import com.elp.services.classService
import com.elp.services.exampleService
import com.elp.services.isExample
import com.elp.util.childOfType
import com.elp.util.error
import com.elp.util.struct
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.cidr.lang.psi.*
import com.jetbrains.cidr.lang.util.OCElementFactory

class CreateReplacementAction : PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "ExampleActions"
    override fun getText() = "Replace member"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (element.containingFile.isExample) return false
        project.classService.findClass(element.containingFile.virtualFile) ?: return false
        project.exampleService.activeExample ?: return false
        return element.parentMember != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = element.containingFile
        val struct = file.struct ?: return

        val member = element.parentMember ?: return

        activeExampleOrCreate(struct.project) { example ->
            val success = ReplacementCreator.create(example, struct, member)
            if (!success) project.error("Already replaced in example")
        }
    }

}

object ReplacementCreator {
    fun create(example: Example, struct: OCStruct, member: Member): Boolean {
        val exampleStruct = example.ownStructs.find { it.name == struct.name }
        if (exampleStruct == null) {
            ReplacementClassCreator.create(example, struct) { invokeLater { createReplacement(member, example, it.name!!) } }
        } else {
            val exampleMembers = exampleStruct.memberFields + exampleStruct.memberFunctions
            if (exampleMembers.any { it equalsIgnoreFile member }) return false
            createReplacement(member, example, exampleStruct.name!!)
        }
        return true
    }

    private fun createReplacement(member: Member, example: Example, structName: String) {
        runWriteAction {
            executeCommand {
                val struct = example.ownStructs.find { it.name == structName } ?: return@executeCommand
                val element = when (member) {
                    is Member.Function -> OCElementFactory.codeFragment(
                        "${member.type} ${member.name}${member.parameterString} {}",
                        struct.project,
                        struct,
                        false,
                        true
                    ).childOfType<OCFunctionDefinition>() ?: return@executeCommand

                    is Member.Field -> OCElementFactory.codeFragment(
                        "${member.type} ${member.name}${member.value?.let { " = $it" } ?: ""};",
                        struct.project,
                        struct,
                        false,
                        true
                    ).childOfType<OCDeclaration>() ?: return@executeCommand
                }
                val anchor = struct.children.findLast { it is OCDeclaration && it !is OCFunctionDeclaration  } ?: struct.openingBrace
                struct.addAfter(element, anchor)
                CodeStyleManager.getInstance(struct.project).reformat(element)

                example.editor.requestFocusInWindow()
                example.editor.editor?.caretModel?.moveToOffset(element.endOffset - 2)
            }
        }
    }
}

val PsiElement.parentMember get(): Member? {
    val declaration = this.parentOfType<OCDeclaration>() ?: return null
    val struct = this.parentOfType<OCStruct>() ?: return null
    if (declaration.parent != struct) return null
    // If declaration is part of function body it shouldn't return function
    if (declaration is OCFunctionDeclaration && this.parentOfType<OCBlockStatement>() != null) return null
    val function = declaration as? OCFunctionDeclaration
    return function?.asMember() ?: declaration.asMember()
}

