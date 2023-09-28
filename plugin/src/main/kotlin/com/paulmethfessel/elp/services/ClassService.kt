package com.paulmethfessel.elp.services

import com.paulmethfessel.elp.model.Example
import com.paulmethfessel.elp.util.getPsiFile
import com.paulmethfessel.elp.util.recursiveChildren
import com.paulmethfessel.elp.util.struct
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

@Service(Service.Level.PROJECT)
class ClassService(
    private val project: Project
): Disposable {
    private var files = listOf<VirtualFile>()
    var cppFiles = listOf<VirtualFile>()
        private set
    var classes = listOf<Clazz>()
        private set
    private var hasRequestedSmartExecution = false

    val currentClass get(): Clazz? {
        val editor = FileEditorManager.getInstance(project).selectedEditor
        return editor?.file?.let { findClass(it) }
    }

    init {
        update()

        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object: BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                val relevant = events.mapNotNull { it.file?.toNioPath() }.any {
                    val rootPath = root?.toNioPath() ?: return@any false
                    it.startsWith(rootPath.resolve("src")) || it.startsWith(rootPath.resolve("include"))
                }
                if (relevant) {
                    update()
                }
            }
        })
    }

    private val root get() = ModuleManager.getInstance(project).modules.firstOrNull()?.rootManager?.contentRoots?.firstOrNull()

    private fun update() {
        val allFiles = findOpenFiles()
        files = allFiles.filter { it.extension == "h" }
        cppFiles = allFiles.filter { it.extension == "cpp" }
        executeSmart {
            classes = findClasses()
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

    private fun findOpenFiles(): List<VirtualFile> {
        val root = root ?: return listOf()
        val src = root.children.find { it.name == "src" }?.recursiveChildren ?: listOf()
        val include = root.children.find { it.name == "include" }?.recursiveChildren ?: listOf()
        return src + include.filter { it.name != "code.h" }
    }

    fun findClass(virtualFile: VirtualFile) = classes.find { it.virtualFile == virtualFile }

    private fun findClasses() = files.mapNotNull { try { Clazz(project, it) } catch(e: Exception) { null } }

    override fun dispose() {}
}

class Clazz(
    val project: Project,
    val virtualFile: VirtualFile
)  {
    val element get() = file.struct ?: error("Could not find class in file '${file.name}'")
    val name = element.name
    val file get() = virtualFile.getPsiFile(project) ?: error("Could not get Psi File for class")
    private val exampleService get() = project.exampleService

    val examples get() = exampleService.examplesForClass(this)

    fun addExample(name: String, callback: (Example) -> Unit) {
        exampleService.addExampleToClass(this, name, callback)
    }

    override fun equals(other: Any?) = other is Clazz && virtualFile == other.virtualFile
    override fun hashCode() = virtualFile.hashCode()

    override fun toString() = name ?: "undefined"
}

val Project.classService get() = service<ClassService>()
