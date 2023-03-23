package com.elp

import com.intellij.openapi.project.ProjectManager

val openProject get() = ProjectManager.getInstance().openProjects.firstOrNull()
