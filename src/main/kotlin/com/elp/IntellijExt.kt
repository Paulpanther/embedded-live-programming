package com.elp

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import javax.swing.Icon

val openProject get() = ProjectManager.getInstance().openProjects.firstOrNull()

@Deprecated("Use ClassService")
fun Project.getAllOpenFiles(): List<PsiFile>? {
    val root =
        ModuleManager.getInstance(this).modules.firstOrNull()?.rootManager?.contentRoots?.firstOrNull() ?: return null
    val src = root.children.find { it.name == "src" } ?: return null
    val include = root.children.find { it.name == "include" } ?: return null
    val files = src.recursiveChildren + include.recursiveChildren.filter { it.name != "code.h" }  // TODO remove the filter
    return files.mapNotNull { PsiManager.getInstance(this).findFile(it) }
}

val VirtualFile.recursiveChildren get(): List<VirtualFile> = children.flatMap { it.recursiveChildren + it }

fun dumbActionButton(text: String, description: String, icon: Icon, callback: (event: AnActionEvent) -> Unit): ActionButton {
    val action = object: DumbAwareAction(text, description, icon) {
        override fun actionPerformed(e: AnActionEvent) {
            callback(e)
        }
    }
    return ActionButton(action, action.templatePresentation.clone(), ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
}

fun Project.error(content: String) {
    NotificationGroupManager
        .getInstance()
        .getNotificationGroup("Embedded Live Programming Notification Group")
        .createNotification(content, NotificationType.ERROR)
        .notify(this)
}

fun Document.getPsiFile(project: Project) = PsiDocumentManager.getInstance(project).getPsiFile(this)
fun VirtualFile.getPsiFile(project: Project) = PsiManager.getInstance(project).findFile(this)
val PsiFile.document get() = PsiDocumentManager.getInstance(project).getDocument(this)
val VirtualFile.document get() = FileDocumentManager.getInstance().getDocument(this)
