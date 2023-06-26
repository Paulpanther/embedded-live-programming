package com.elp.actions

import com.elp.logic.Member
import com.elp.logic.memberFields
import com.elp.logic.memberFunctions
import com.elp.logic.missingMembers
import com.elp.services.example
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentsOfType
import com.jetbrains.cidr.cpp.assertions.assertNotNull
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCExpressionStatement
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.types.OCStructType

class ReplacementCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.completionType != CompletionType.BASIC) return
        val file = parameters.originalFile
        val example = file.example ?: return
        val input = parameters.originalPosition ?: return
        val missingMembers = example.missingMembers

        val hasDeclaration = input.parentsOfType<OCDeclaration>().any { it.text != input.text && it.type !is OCStructType }
        if (input.prevSibling is OCExpressionStatement && !input.prevSibling.text.endsWith(";")) return
        val members = if (hasDeclaration) {
            completeUsage(missingMembers)
        } else {
            completeMemberDefinition(missingMembers)
        }

        members.forEach { result.addElement(it) }
    }

    private fun completeUsage(members: List<Member>): List<LookupElementBuilder> {
        return members.map { member ->
            when (member) {
                is Member.Field -> fieldLookupElement(member)
                is Member.Function -> functionLookupElement(member) { ctx ->
                    ctx.document.insertString(ctx.tailOffset, "()")
                    ctx.editor.caretModel.moveToOffset(ctx.tailOffset - 1)
                }
            }
        }
    }

    private fun completeMemberDefinition(members: List<Member>): List<LookupElementBuilder> {
        return members.map { member ->
            when (member) {
                is Member.Field -> fieldLookupElement(member) { ctx ->
                    ctx.document.insertString(ctx.startOffset, "${member.type} ")
                    ctx.document.insertString(ctx.tailOffset, " = ;")
                    ctx.editor.caretModel.moveToOffset(ctx.tailOffset - 1)
                }
                is Member.Function -> functionLookupElement(member) { ctx ->
                    ctx.document.insertString(ctx.startOffset, "${member.type} ")
                    ctx.document.insertString(ctx.tailOffset, "${member.parameterString} {}")
                    ctx.editor.caretModel.moveToOffset(ctx.tailOffset - 1)
                }
            }
        }
    }

    private fun fieldLookupElement(member: Member.Field, insertHandler: (ctx: InsertionContext) -> Unit = {}) = LookupElementBuilder
        .create(member.name)
        .withTypeText(member.type)
        .withInsertHandler { ctx, _ -> insertHandler(ctx) }

    private fun functionLookupElement(member: Member.Function, insertHandler: (ctx: InsertionContext) -> Unit = {}) = LookupElementBuilder
        .create(member.name)
        .withTypeText(member.type)
        .withTailText("(${member.parameters.joinToString(", ")})", true)
        .withInsertHandler { ctx, _ -> insertHandler(ctx) }

}
