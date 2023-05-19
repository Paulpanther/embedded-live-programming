package com.elp.logic

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

fun Project.error(content: String) {
    NotificationGroupManager
        .getInstance()
        .getNotificationGroup("Embedded Live Programming Notification Group")
        .createNotification(content, NotificationType.ERROR)
        .notify(this)
}
