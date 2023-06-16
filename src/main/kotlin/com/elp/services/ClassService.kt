package com.elp.services

import com.elp.recursiveChildren
import com.elp.util.UpdateListeners
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.psi.OCStruct

@Service
class ClassService(
    private val project: Project
): Disposable {
    private var files = listOf<PsiFile>()
    var classes = listOf<Clazz>()
        private set
    val onClassesChanged = UpdateListeners()
    private var hasRequestedSmartExecution = false

    init {
        update()

        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object: BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                events.mapNotNull { it.file?.toNioPath() }.any {
                    val rootPath = root?.toNioPath() ?: return@any false
                    it.startsWith(rootPath.resolve("src")) || it.startsWith(rootPath.resolve("include"))
                }
                update()
            }
        })
    }

    private val root = ModuleManager.getInstance(project).modules.firstOrNull()?.rootManager?.contentRoots?.firstOrNull()

    private fun update() {
        files = findOpenFiles()
        executeSmart {
            classes = findClasses()
            onClassesChanged.call()
        }
    }

    private fun executeSmart(op: () -> Unit) {
        val dumbService = DumbService.getInstance(project)
        if (!dumbService.isDumb) {
            op()
        } else if (!hasRequestedSmartExecution) {
            hasRequestedSmartExecution = true
            DumbService.getInstance(project).smartInvokeLater {
                op()
            }
        }
    }

    private fun findOpenFiles(): List<PsiFile> {
        val root = root ?: return listOf()
        val src = root.children.find { it.name == "src" }?.recursiveChildren ?: listOf()
        val include = root.children.find { it.name == "include" }?.recursiveChildren ?: listOf()
        val files = src + include.filter { it.name != "code.h" }
        return files.mapNotNull { PsiManager.getInstance(project).findFile(it) }
    }

    private fun findClasses(): List<Clazz> {
        return files
            .mapNotNull { PsiTreeUtil.findChildOfType(it, OCStruct::class.java) }
            .map { Clazz(it) }
    }

    override fun dispose() {}
}

data class Clazz(
    val element: OCStruct
)  {
    val name = element.name
    val file: PsiFile = element.containingFile
    private val exampleService = element.project.exampleService

    val examples get() = exampleService.examplesForClass(this)

    fun addExample(name: String, callback: (Example) -> Unit) {
        exampleService.addExampleToClass(this, name, callback)
    }
}

val Project.classService get() = service<ClassService>()
