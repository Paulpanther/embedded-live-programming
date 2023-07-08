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
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.findParentOfType
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.cidr.lang.psi.OCBlockStatement
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCDeclarator
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.util.OCElementFactory

class CreateReplacementAction : PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "ExampleActions"
    override fun getText() = "Replace member"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (element.containingFile.isExample) return false
        project.classService.findClass(element.containingFile.virtualFile) ?: return false
        project.exampleService.activeExample ?: return false
        return getParentMember(element) != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = element.containingFile
        val struct = file.struct ?: return
        val example = project.exampleService.activeExample ?: return

        val member = getParentMember(element) ?: return

        val exampleStruct = example.ownStructs.find { it.name == struct.name }
        if (exampleStruct == null) {
            ReplacementClassCreator.create(example, struct) { createReplacement(member, example, it) }
        } else {
            val exampleMembers = exampleStruct.memberFields + exampleStruct.memberFunctions
            if (exampleMembers.any { it equalsIgnoreFile member }) return project.error("Already replaced in example")
            createReplacement(member, example, exampleStruct)
        }
    }

    private fun getParentMember(element: PsiElement): Member? {
        val declaration = element.parentOfType<OCDeclaration>() ?: return null
        val struct = element.parentOfType<OCStruct>() ?: return null
        if (declaration.parent != struct) return null
        // If declaration is part of function body it shouldn't return function
        if (declaration is OCFunctionDeclaration && element.parentOfType<OCBlockStatement>() != null) return null
        val function = declaration as? OCFunctionDeclaration
        return function?.asMember() ?: declaration.asMember()
    }

    private fun createReplacement(member: Member, example: Example, struct: OCStruct) {
        runWriteAction {
            executeCommand {
                val element = when (member) {
                    is Member.Function -> OCElementFactory.methodFromSignature("${member.type} ${member.name}${member.parameterString}", struct, true, true)
                    is Member.Field -> OCElementFactory.declaration(member.name, member.typeElement, member.initializer, struct)
                }
                val anchor = struct.children.findLast { it is OCDeclaration && it !is OCFunctionDeclaration  } ?: struct.closingBrace
                struct.addAfter(element, anchor)
                CodeStyleManager.getInstance(struct.project).reformat(element)

                example.editor.requestFocusInWindow()
                example.editor.editor?.caretModel?.moveToOffset(element.endOffset - 2)
            }
        }
    }
}
