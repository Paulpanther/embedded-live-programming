package com.paulmethfessel.elp.editor

import com.paulmethfessel.elp.services.isExample
import com.paulmethfessel.elp.util.childrenOfType
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.cidr.lang.psi.OCCppNamespace

/**
 * folds replacement namespaces
 */
class NamespaceFoldingBuilder: FoldingBuilderEx() {
    override fun getPlaceholderText(node: ASTNode) = "Replacement"
    override fun isCollapsedByDefault(node: ASTNode) = true

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        if (!root.containingFile.isExample) return arrayOf()
        return root.containingFile.childrenOfType<OCCppNamespace>().mapNotNull {
            val name = it.nameIdentifier ?: return@mapNotNull null
            val range = TextRange(it.startOffset, name.endOffset)
            FoldingDescriptor(name.node, range, null, setOf(), true)
        }.toTypedArray()
    }
}
