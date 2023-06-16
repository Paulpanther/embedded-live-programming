package com.elp.ui.editor

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.psi.OCStruct

class ReplacementFoldingBuilder: FoldingBuilderEx() {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val group = FoldingGroup.newGroup("ReplacementFoldingGroup")
        val structs = PsiTreeUtil.findChildrenOfType(root, OCStruct::class.java)

        return structs.mapNotNull {
            val name = it.nameIdentifier ?: return@mapNotNull null
            FoldingDescriptor(name.node, it.headerRange, group)
        }.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode) = node.text

    override fun isCollapsedByDefault(node: ASTNode) = true
}
