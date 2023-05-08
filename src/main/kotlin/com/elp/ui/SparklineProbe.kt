@file:Suppress("UnstableApiUsage")

package com.elp.ui

import com.intellij.codeInsight.hints.presentation.BasePresentation
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Graphics2D
import java.util.*
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
    private val maxValue: Int,
    override val height: Int
): BasePresentation() {
    private var value: Int = 0

    val paddingY = 4
    override val width = 50

    private var line = ValuesLine(width)

    fun update(value: Int) {
        this.value = value
        if (value < minValue || value > maxValue) error("Invalid value $value. Should be between $minValue and $maxValue")

        // interpolate value between min and max and calculate relative y position with padding
        val relValue = (value - minValue).toFloat() / (maxValue - minValue)
        val innerHeight = height - paddingY * 2
        line += paddingY + innerHeight - (relValue * innerHeight).roundToInt()
    }

    override fun paint(g: Graphics2D, attributes: TextAttributes) {
        g.color = JBColor.RED
        g.drawPolyline(line.getX(), line.getY(), line.size)
    }

    override fun toString() = value.toString()
}
