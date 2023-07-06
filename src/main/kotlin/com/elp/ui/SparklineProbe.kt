@file:Suppress("UnstableApiUsage")

package com.elp.ui

import com.elp.util.withHints
import com.intellij.codeInsight.hints.presentation.BasePresentation
import com.intellij.codeInsight.hints.presentation.InlayTextMetrics
import com.intellij.ide.ui.AntialiasingType
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

// TODO change to store absolute values and make relative with current limits
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
    editor: EditorImpl,
): BasePresentation() {
    private var value = 0
    private var minValue: Int? = null
    private var maxValue: Int? = null

    private val paddingY = 4
    private val lineWidth = 70
    private val gapX = 5
    private val valueMetrics = InlayTextMetrics.create(editor, 12)
    private val limitMetrics = InlayTextMetrics.create(editor, 9)
    private val valueStringWidth get() = valueMetrics.getStringWidth(value.toString())
    private val limitStringWidth get() = max(limitMetrics.getStringWidth(minValue.toString()), limitMetrics.getStringWidth(maxValue.toString()))

    override val width get() = limitStringWidth + gapX + lineWidth + gapX + valueStringWidth
    override val height = editor.lineHeight

    private var line = ValuesLine(lineWidth)

    fun update(value: Int) {
        this.value = value
        var currentMin = minValue ?: value
        var currentMax = maxValue ?: value
        if (value < currentMin) currentMin = value
        if (value > currentMax) currentMax = value
        minValue = currentMin
        maxValue = currentMax

        // interpolate value between min and max and calculate relative y position with padding
        val full = currentMax - currentMin
        if (full == 0) return
        val relValue = (value - currentMin).toFloat() / full
        val innerHeight = height - paddingY * 2
        line += paddingY + innerHeight - (relValue * innerHeight).roundToInt()
    }

    override fun paint(g: Graphics2D, attributes: TextAttributes) {
        g.withHints(RenderingHints.KEY_TEXT_ANTIALIASING to AntialiasingType.getKeyForCurrentScope(false)) {
            g.color = JBColor.MAGENTA
            val startLine = limitStringWidth + gapX
            g.drawLine(startLine, paddingY, startLine, height - paddingY)
            g.drawPolyline(line.getX().map { it + startLine }.toIntArray(), line.getY(), line.size)

            if (minValue != null && maxValue != null) {
                g.font = limitMetrics.font
                val minWidth = limitMetrics.getStringWidth(minValue.toString())
                val maxWidth = limitMetrics.getStringWidth(maxValue.toString())
                val minMargin = if (minWidth < maxWidth) maxWidth - minWidth else 0
                val maxMargin = if (maxWidth < minWidth) minWidth - maxWidth else 0
                g.drawString(minValue.toString(), minMargin, height - paddingY)
                g.drawString(maxValue.toString(), maxMargin, limitMetrics.fontBaseline + paddingY)
            }

            g.font = valueMetrics.font
            val currentStringWidth = valueMetrics.getStringWidth(value.toString())
            val y = height / 2 + valueMetrics.fontBaseline / 2
            g.drawString(value.toString(), width - currentStringWidth, y)
        }
    }

    override fun toString() = value.toString()
}