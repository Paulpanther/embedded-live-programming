package com.elp.editor

import com.elp.actions.ReplacementClassCreator
import com.elp.instrumentalization.*
import com.elp.model.Example
import com.elp.services.classService
import com.elp.services.example
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiFile
import com.intellij.psi.util.leavesAroundOffset
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
        val project = file.project
        val input = parameters.originalPosition
        val parent = input?.parentOfType<OCStruct>()

        if (parent == null || parent.nameIdentifier == input) {
            val textBefore = file.text.subSequence(0, parameters.offset).trim()
            val hasClass = textBefore.endsWith("class") || textBefore.endsWith("struct")
            addClassCompletion(example, result, hasClass)
            return
        }

        if (input.parentsOfType<OCDeclaration>().any { it.text != input.text && it.type !is OCStructType }) return

        val originalClass = project.classService.classes.map { it.element }.find { it.name == parent.name } ?: return
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
                is Member.Function -> createFunctionLookup(member.name, member.type, member.parameters)
            }
            result.addElement(element)
        }

        val hasLoop = originalClass.loop != null || example.ownMainStruct.loop != null
        val hasSetup = originalClass.setup != null || example.ownMainStruct.setup != null
        if (!hasLoop) result.addElement(createFunctionLookup("liveLoop", "void", listOf()))
        if (!hasSetup) result.addElement(createFunctionLookup("setup", "void", listOf()))
    }

    private fun createFunctionLookup(name: String, type: String, parameters: List<String>): LookupElementBuilder {
        return LookupElementBuilder
            .create(name)
            .withTypeText(type)
            .withTailText("(${parameters.joinToString(", ")})", true)
            .withInsertHandler { ctx, _ ->
                ctx.document.insertString(ctx.startOffset, "$type ")
                val params = "(${parameters.joinToString(", ")}) {}"
                ctx.document.insertString(ctx.tailOffset, params)
                ctx.editor.caretModel.moveToOffset(ctx.tailOffset - 1)
            }
    }

    private fun addClassCompletion(example: Example, result: CompletionResultSet, hasClass: Boolean) {
        val allClasses = example.project.classService.classes.mapNotNull { it.name }
        val usedClasses = example.ownStructs.mapNotNull { it.name }
        val remainingClasses = allClasses - usedClasses.toSet()
        remainingClasses.map { result.addElement(LookupElementBuilder
            .create("${if (hasClass) "" else "struct "}$it")
            .withTailText("{};")
            .withInsertHandler { ctx, _ ->
                val namespaceName = ReplacementClassCreator.nextNamespaceName(example)
                ctx.document.insertString(ctx.startOffset, "namespace $namespaceName {\n\t")
                ctx.document.insertString(ctx.tailOffset, " {};\n}")
                ctx.editor.caretModel.moveToOffset(ctx.tailOffset - 4)
            })
        }
    }
}
