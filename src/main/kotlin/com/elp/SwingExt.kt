package com.elp

import javax.swing.JPanel

fun panel(builder: JPanel.() -> Unit) = object: JPanel() {
    init {
        builder(this)
    }
}
