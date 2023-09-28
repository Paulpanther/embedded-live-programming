package com.paulmethfessel.elp.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object ExampleNotification {
    fun notifyError(project: Project?, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Embedded Live Programming Notification Group")
            .createNotification(content, NotificationType.ERROR)
            .notify(project)
    }
}
