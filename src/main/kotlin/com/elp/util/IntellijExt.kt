package com.elp.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementsAroundOffsetUp
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.psi.OCStruct
import javax.swing.Icon

val openProject get() = ProjectManager.getInstance().openProjects.firstOrNull()

@Deprecated("Use ClassService")
fun Project.getAllOpenFiles(): List<PsiFile>? {
    val root =
        ModuleManager.getInstance(this).modules.firstOrNull()?.rootManager?.contentRoots?.firstOrNull() ?: return null
    val src = root.children.find { it.name == "src" } ?: return null
    val include = root.children.find { it.name == "include" } ?: return null
    val files =
        src.recursiveChildren + include.recursiveChildren.filter { it.name != "code.h" }  // TODO remove the filter
    return files.mapNotNull { PsiManager.getInstance(this).findFile(it) }
}

val VirtualFile.recursiveChildren get(): List<VirtualFile> = children.flatMap { it.recursiveChildren + it }

fun dumbActionButton(
    text: String,
    description: String,
    icon: Icon,
    callback: (event: AnActionEvent) -> Unit
): ActionButton {
    val action = object : DumbAwareAction(text, description, icon) {
        override fun actionPerformed(e: AnActionEvent) {
            callback(e)
        }
    }
    return ActionButton(
        action,
        action.templatePresentation.clone(),
        ActionPlaces.UNKNOWN,
        ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    )
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

val PsiFile.struct get() = childOfType<OCStruct>()
val PsiFile.structs get() = childrenOfType<OCStruct>()

fun PsiFile.clone() = PsiFileFactory
    .getInstance(project)
    .createFileFromText(name, OCLanguage.getInstance(), text)

val PsiElement.navigable get() = OpenFileDescriptor(project, containingFile.virtualFile, navigationElement.startOffset)

inline fun <reified T: PsiElement> PsiElement.childrenOfType(): List<T> = PsiTreeUtil.findChildrenOfType(this, T::class.java).toList()
inline fun <reified T: PsiElement> PsiElement.childOfType(): T? = PsiTreeUtil.findChildOfType(this, T::class.java)

inline fun <reified T: PsiElement> PsiElement.childAtRangeOfType(range: TextRange): T? {
    var element = findElementAt(range.startOffset) ?: return null
    while (element.textRange != range || element !is T) {
        if (element.textRange !in range) return null
        element = element.parentOfType<T>() ?: return null
    }

    return element
}