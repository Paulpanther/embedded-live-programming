package com.elp.actions

import com.elp.logic.Member
import com.elp.logic.memberFields
import com.elp.logic.memberFunctions
import com.elp.services.example
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentsOfType
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.types.OCStructType

class ReplacementCompletionContributor: CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.completionType != CompletionType.BASIC) return
        val file = parameters.originalFile
        val example = file.example ?: return
        val input = parameters.originalPosition ?: return
        val parent = input.parentOfType<OCStruct>() ?: return

        if (input.parentsOfType<OCDeclaration>().any { it.text != input.text && it.type !is OCStructType }) return

        val originalClass = example.clazz.element
        val originalMembers = originalClass.memberFields + originalClass.memberFunctions
        val exampleMembers = parent.memberFields + parent.memberFunctions
        val missingMembers = originalMembers.filter { exampleMembers.none(it::equalsIgnoreFile) }

        for (member in missingMembers) {
            val element = when (member) {
                is Member.Field -> LookupElementBuilder
                    .create(member.name)
                    .withTypeText(member.type)
                    .withInsertHandler { ctx, _ ->
                        ctx.document.insertString(ctx.startOffset, "${member.type} ")
                        ctx.document.insertString(ctx.tailOffset, " = ;")
                        ctx.editor.caretModel.moveToOffset(ctx.tailOffset - 1)
                    }
                is Member.Function -> LookupElementBuilder
                    .create(member.name)
                    .withTypeText(member.type)
                    .withTailText("(${member.parameters.joinToString(", ")})", true)
                    .withInsertHandler { ctx, _ ->
                        ctx.document.insertString(ctx.startOffset, "${member.type} ")
                        val params = "(${member.parameters.joinToString(", ")}) {}"
                        ctx.document.insertString(ctx.tailOffset, params)
                        ctx.editor.caretModel.moveToOffset(ctx.tailOffset - 1)
                    }
            }
            result.addElement(element)
        }
    }
}
