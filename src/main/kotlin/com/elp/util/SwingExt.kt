package com.elp.util

import java.awt.Component
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.JPanel

fun panel(builder: JPanel.() -> Unit) = object: JPanel() {
    init {
        builder(this)
    }
}

operator fun JComponent.plusAssign(o: Component) { add(o) }

fun Graphics2D.withHints(vararg hints: Pair<RenderingHints.Key, Any>, scope: () -> Unit) {
    val saved = hints.associate { (key, _) -> key to getRenderingHint(key) }

    try {
        setRenderingHints(hints.toMap())
        scope()
    } finally {
        setRenderingHints(saved)
    }
}