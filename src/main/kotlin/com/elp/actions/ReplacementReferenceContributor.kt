package com.elp.actions

import com.elp.logic.*
import com.elp.services.example
import com.elp.services.isExample
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.cidr.lang.psi.OCCallExpression
import com.jetbrains.cidr.lang.psi.OCFunctionDefinition

class ReplacementReferenceContributor: PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(OCCallExpression::class.java), object: PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                val file = element.containingFile
                val example = file.example ?: return arrayOf()
                val raw = element.text
                val members = example.missingMembers
                val member = members.find { it.name == raw } ?: return arrayOf()

                if (member is Member.Function) {
                    return arrayOf(FunctionReference(element, TextRange(0, element.textLength)))
                }
                return arrayOf()
            }
        })
    }
}

class FunctionReference(
    element: PsiElement,
    textRange: TextRange
): PsiReferenceBase<PsiElement>(element, textRange) {
    override fun resolve(): PsiElement? {
        val example = element.containingFile.example ?: return null
        val self = (element as? OCFunctionDefinition)?.asMember() ?: return null
        val original = example.clazz.element
        val functions = original.memberFunctions
        val memberRef = functions.find { it equalsIgnoreFile self } ?: return null
        return memberRef.element
    }

    override fun getVariants(): Array<Any> {
        val example = element.containingFile.example ?: return arrayOf()
        val functions = example.clazz.element.memberFunctions
        return functions.map { LookupElementBuilder.create(it) }.toTypedArray()
    }
}
