@file:Suppress("UnstableApiUsage")

package com.elp.ext

import com.elp.logic.struct
import com.intellij.model.Pointer
import com.intellij.model.Symbol
import com.intellij.model.psi.ImplicitReferenceProvider
import com.intellij.navigation.NavigatableSymbol
import com.intellij.navigation.NavigationTarget
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.psi.OCReferenceElement

class ReplacementImplicitReferenceProvider: ImplicitReferenceProvider {
    override fun resolveAsReference(element: PsiElement): List<Symbol> {
        if (element !is OCReferenceElement) return listOf()
       val clazz = element.containingFile.struct ?: return listOf()

        return listOf(ReplacementSymbol())
    }
}

class ReplacementSymbol: Symbol, NavigatableSymbol {
    override fun createPointer(): Pointer<out Symbol> {
        return Pointer.hardPointer(this)
    }

    override fun getNavigationTargets(project: Project): List<NavigationTarget> {
        return listOf()
    }
}
