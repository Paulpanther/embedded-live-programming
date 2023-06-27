package com.elp.model

import com.elp.instrumentalization.Modification
import com.elp.services.Clazz
import com.elp.services.exampleKey
import com.elp.services.exampleService
import com.elp.util.*
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorTextField
import com.jetbrains.cidr.lang.OCFileType
import com.jetbrains.cidr.lang.psi.OCStruct

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

    val replacedStructs get(): List<OCStruct> {
        val main = ownMainStruct
        return ownStructs.filter { it != main }
    }

    val parentStruct get() = parentFile.struct ?: error("Missing class in file '${parentFile.name}")

    fun activate() {
        project.exampleService.activeExample = this
    }

    fun navigateTo(descriptor: OpenFileDescriptor) {
        editor.component.requestFocusInWindow()
        descriptor.navigateIn(editor.editor ?: return project.error("Could not navigate to element"))
    }

    override fun toString() = name
}
