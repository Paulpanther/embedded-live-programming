package com.paulmethfessel.elp.ui

import com.intellij.openapi.util.IconLoader

object ELPIcons {
    @JvmField
    val logo = IconLoader.getIcon("/icons/logo.svg", ELPIcons::class.java)

    @JvmField
    val createExample = IconLoader.getIcon("/icons/create_example_dark.svg", ELPIcons::class.java)

    @JvmField
    val createReplacement = IconLoader.getIcon("/icons/create_replacement_dark.svg", ELPIcons::class.java)
}
