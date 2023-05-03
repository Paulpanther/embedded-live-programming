@file:Suppress("UnstableApiUsage")

package com.elp

import com.intellij.codeInsight.hints.fireContentChanged
import com.intellij.codeInsight.hints.presentation.BasePresentation
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Graphics2D
import java.util.LinkedList
import kotlin.math.roundToInt

private class ValuesLine(val totalSize: Int) {
    private val y = LinkedList<Int>()
    private val x = (0 until totalSize).map { it }.toIntArray()

    operator fun plusAssign(value: Int) {
        if (size >= totalSize) {
            y.removeFirst()
        }

        y += value
    }

    val size get() = y.size

    fun getY() = y.toIntArray()
    fun getX() = if (size < totalSize) x.sliceArray(0 until size) else x
}

class SparklineProbe(
    private val minValue: Int,
    private val maxValue: Int
): BasePresentation() {
    private var value: Int = 0

    override val height = 10
    override val width = 50

    private var line = ValuesLine(width)

    fun update(value: Int) {
        this.value = value
        line += (value.toFloat() / height).roundToInt()
        fireContentChanged()
    }

    override fun paint(g: Graphics2D, attributes: TextAttributes) {
        g.color = JBColor.RED
        g.drawPolyline(line.getX(), line.getY(), line.size)
    }

    override fun toString() = value.toString()
}
