package com.paulmethfessel.elp.editor

import com.paulmethfessel.elp.util.childrenOfType
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.cidr.lang.psi.OCIncludeDirective

/**
 * folds includes
 */
class IncludeFoldingBuilder: FoldingBuilderEx() {
    override fun getPlaceholderText(node: ASTNode) = null
    override fun isCollapsedByDefault(node: ASTNode) = true

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val file = root.containingFile
        val imports = file.childrenOfType<OCIncludeDirective>()
        if (imports.isEmpty()) return arrayOf()

        val range = TextRange(imports.first().startOffset, imports.last().endOffset)
        return arrayOf(FoldingDescriptor(imports.first().node, range, null, setOf(), false))
    }
}
