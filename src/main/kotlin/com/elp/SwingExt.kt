package com.elp

import java.awt.Component
import javax.swing.JComponent
import javax.swing.JPanel

fun panel(builder: JPanel.() -> Unit) = object: JPanel() {
    init {
        builder(this)
    }
}

operator fun JComponent.plusAssign(o: Component) { add(o) }
