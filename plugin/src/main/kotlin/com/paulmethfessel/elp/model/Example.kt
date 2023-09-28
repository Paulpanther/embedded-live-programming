package com.paulmethfessel.elp.model

import com.paulmethfessel.elp.execution.Modification
import com.paulmethfessel.elp.services.Clazz
import com.paulmethfessel.elp.services.classService
import com.paulmethfessel.elp.services.exampleKey
import com.paulmethfessel.elp.services.exampleService
import com.paulmethfessel.elp.util.*
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.EditorTextField
import com.jetbrains.cidr.lang.OCFileType
import com.jetbrains.cidr.lang.psi.OCCppNamespace

class Example(
    val project: Project,
    val parentClazz: Clazz,
    val ownVirtualFile: VirtualFile,
    var name: String,
) {
    var modifications = listOf<Modification>()
    val editor = EditorTextField(ownDocument, project, OCFileType.INSTANCE, false, false)

    init {
        ownDocument.putUserData(exampleKey, this)
    }

    val ownDocument get() = ownVirtualFile.document ?: error("Could not get document of example '$name'")
    val ownFile get() = ownVirtualFile.getPsiFile(project) ?: error("Could not get psi file of example '$name'")
    val parentFile get() = parentClazz.file

    val ownStructs get() = ownFile.structs
    val ownMainStruct get() = ownStructs.find { it.name == parentClazz.name } ?: error("Missing class in example '$name'")
    val ownReplacedStructs get() = ownStructs - ownMainStruct
    val ownReplacementNamespaces get() = ownFile.childrenOfType<OCCppNamespace>()

    val referencedStructs get() = ownStructs.mapNotNull { own -> project.classService.classes.find { it.name == own.name }?.element }
    val referencedFiles get() = referencedStructs.map { it.containingFile }

    val parentStruct get() = parentFile.struct ?: error("Missing class in file '${parentFile.name}")

    fun activate() {
        project.exampleService.activeExample = this
    }

    fun navigateTo(descriptor: OpenFileDescriptor) {
        editor.component.requestFocusInWindow()
        descriptor.navigateIn(editor.editor ?: return project.error("Could not navigate to element"))
    }

    fun commitDocument(callback: () -> Unit) {
        val manager = PsiDocumentManager.getInstance(project)
        if (manager.isCommitted(ownDocument)) {
            callback()
        } else {
            runWriteAction {
                manager.commitDocument(ownDocument)
                callback()
            }
        }
    }

    override fun toString() = name
}
