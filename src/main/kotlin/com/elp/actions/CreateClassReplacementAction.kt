package com.elp.actions

import com.elp.model.Example
import com.elp.services.exampleService
import com.elp.services.isExample
import com.elp.util.*
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.psi.OCCppNamespace
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.util.OCElementFactory

/**
 * Press alt + enter on a class and choose "Replace Class".
 * This will create a new replacement struct in the active example
 */
class CreateClassReplacementAction: PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "ExampleActions"
    override fun getText() = "Replace Class"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val struct = element.parent as? OCStruct ?: return false
        val file = element.containingFile
        val activeExample = project.exampleService.activeExample ?: return false
        if (file.isExample) return false
        if (activeExample.parentFile == file) return false
        if (activeExample.ownStructs.any { it.name == struct.name }) return false
        return true
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val struct = element.parent as? OCStruct ?: return
        val example = project.exampleService.activeExample ?: return
        ReplacementClassCreator.create(example, struct) {
            example.editor.requestFocusInWindow()
            example.editor.editor?.caretModel?.moveToOffset(it.functionsStartOffset)
        }
    }
}

object ReplacementClassCreator {
    /**
     * Creates a new struct with the given name in the given example.
     * Struct is wrapped in a namespace to avoid reference errors
     */
    fun create(example: Example, struct: OCStruct, callback: (struct: OCStruct) -> Unit) {
        val structName = struct.name ?: "undefined"
        val file = example.ownFile

        example.commitDocument {
            runWriteAction {
                executeCommand {
                    val namespace = OCElementFactory.codeFragment(
                        "namespace ${nextNamespaceName(example)} {}",
                        file.project,
                        file,
                        true,
                        true
                    ).childOfType<OCCppNamespace>() ?: return@executeCommand
                    val newStructFragment = OCElementFactory.codeFragment(
                        "struct $structName {};",
                        file.project,
                        namespace,
                        true,
                        true)
                    val newStruct = newStructFragment.childOfType<OCStruct>() ?: return@executeCommand

                    namespace.addAfter(newStructFragment, namespace.openingBrace)
                    val lastStruct = file.structs.lastOrNull()
                    val lastNamespace = file.childrenOfType<OCCppNamespace>().lastOrNull()
                    file.addAfter(namespace, lastNamespace ?: lastStruct)

                    val newActualStruct = file.structs.find { it.name == newStruct.name } ?: return@executeCommand
                    callback(newActualStruct)
                }
            }
        }
    }

    fun nextNamespaceName(example: Example) = NamingHelper.nextName("Replacement", example.ownReplacementNamespaces.map { it.name ?: "" })
}
