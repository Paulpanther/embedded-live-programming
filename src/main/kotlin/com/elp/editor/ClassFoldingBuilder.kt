package com.elp.editor

import com.elp.util.structs
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement

/**
 * folds class and struct keywords to only show the class name
 */
class ClassFoldingBuilder: FoldingBuilderEx() {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        return root.containingFile.structs.mapNotNull {
            val name = it.nameIdentifier ?: return@mapNotNull null
            FoldingDescriptor(name.node, it.headerRange, null, setOf(), true)
        }.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode) = node.text
    override fun isCollapsedByDefault(node: ASTNode) = true
}
