package com.paulmethfessel.elp.editor

import com.intellij.openapi.fileTypes.LanguageFileType
import com.jetbrains.cidr.lang.OCLanguage
import icons.CidrLangIcons.FileTypes

class ComponentFile: LanguageFileType(OCLanguage.getInstance()) {
    override fun getName() = "Component"

    override fun getDescription() = "A component with fields and methods"

    override fun getDefaultExtension() = "h"

    override fun getIcon() = FileTypes.Cpp
}