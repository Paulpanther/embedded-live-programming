@file:Suppress("UnstableApiUsage")

package com.elp.ui

import com.elp.model.Probe
import com.elp.util.withHints
import com.intellij.codeInsight.hints.presentation.BasePresentation
import com.intellij.codeInsight.hints.presentation.InlayTextMetrics
import com.intellij.ide.ui.AntialiasingType
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.lang.Integer.min
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

private class ValuesLine(val totalSize: Int) {
    private val y = LinkedList<Double>()

    operator fun plusAssign(value: Double) {
        if (size >= totalSize) {
            y.removeFirst()
        }

        y += value
    }

    val size get() = y.size

    fun x(start: Int) = (0 until min(size, totalSize)).map { it + start }.toIntArray()

    fun relativeY(min: Double, max: Double, start: Int, height: Int): IntArray {
        val full = max - min
        return y.map {
            val relValue = (it - min).toFloat() / full
            start + height - (relValue * height).roundToInt()
        }.toIntArray()
    }
}

class SparklineProbeWrapper(
    private val editor: EditorImpl,
): BasePresentation() {
    private var inlay: SparklineProbe<*>? = null

    override val width get() = inlay?.width ?: 100
    override val height get() = inlay?.height ?: editor.lineHeight

    fun update(probe: Probe) {
        val inlay = inlay
        if (inlay == null) {
            this.inlay = SparklineProbe.createFrom(editor, probe)
        } else {
            inlay.updateOrRecreate(probe)
        }
    }

    override fun paint(g: Graphics2D, attributes: TextAttributes) {
        inlay?.paint(g, attributes)
    }

    override fun toString() = inlay?.toString() ?: ""
}

class SparklineProbe<T>(
    private val type: String,
    private val editor: EditorImpl,
    private val toDouble: (v: T) -> Double,
    private val toString: (v: T) -> String,
    private val showLimit: Boolean,
    private val showLine: Boolean,
    private var minValue: Double? = null,
    private var maxValue: Double? = null
): BasePresentation() {
    companion object {
        fun createFrom(editor: EditorImpl, probe: Probe): SparklineProbe<*> {
            return when(probe.type) {
                "int" -> SparklineProbe<Int>(probe.type, editor, { it.toDouble() }, { it.toString() }, true, true)
                "double" -> SparklineProbe<Double>(probe.type, editor, { it }, { it.toString() }, true, true)
                "float" -> SparklineProbe<Float>(probe.type, editor, { it.toDouble() }, { it.toString() }, true, true)
                "bool" -> SparklineProbe<Boolean>(probe.type, editor, { if (it) 1.0 else 0.0 }, { if (it) "true" else "false" }, false, true, 0.0, 1.0)
                else -> SparklineProbe<String>("string", editor, { 0.0 }, { it }, false, false)
            }
        }
    }

    private var value: T? = null

    private val paddingY = 4
    private val lineWidth = 70
    private val gapX = 5
    private val valueMetrics = InlayTextMetrics.create(editor, 12)
    private val limitMetrics = InlayTextMetrics.create(editor, 9)
    private val valueStringWidth get() = valueMetrics.getStringWidth(strValue)
    private val limitStringWidth get() = max(limitMetrics.getStringWidth(minValue.toString()), limitMetrics.getStringWidth(maxValue.toString()))

    private val strValue get() = value?.let(toString) ?: ""

    override val width get(): Int {
        val limit = if (showLimit) limitStringWidth + gapX else 0
        val line = if (showLine) lineWidth + gapX else 0
        return max(limit + line + valueStringWidth, 10)
    }

    override val height = editor.lineHeight

    private var line = ValuesLine(lineWidth)

    fun updateOrRecreate(probe: Probe): SparklineProbe<*> {
        if (probe.type != type) {
            val s = createFrom(editor, probe)
            s.updateRaw(probe.value)
            return s
        }

        updateRaw(probe.value)
        return this
    }

    private fun updateRaw(value: String) {
        when (type) {
            "int" -> update(value.toInt() as T)
            "float" -> update(value.toFloat() as T)
            "double" -> update(value.toDouble() as T)
            "bool" -> update(value.toBoolean() as T)
            else -> update(value as T)
        }
    }

    fun update(value: T) {
        this.value = value

        if (showLine) {
            val dValue = toDouble(value)
            var currentMin = minValue ?: dValue
            var currentMax = maxValue ?: dValue
            if (dValue < currentMin) currentMin = dValue
            if (dValue > currentMax) currentMax = dValue
            this.minValue = currentMin
            this.maxValue = currentMax

            line += dValue
        }
    }

    override fun paint(g: Graphics2D, attributes: TextAttributes) {
        value ?: return
        val currentMin = minValue
        val currentMax = maxValue

        g.withHints(RenderingHints.KEY_TEXT_ANTIALIASING to AntialiasingType.getKeyForCurrentScope(false)) {
            g.color = JBColor.MAGENTA
            if (showLine) {
                val startLine = if (showLimit) limitStringWidth + gapX else 0
                g.drawLine(startLine, paddingY, startLine, height - paddingY)

                if (currentMin != null && currentMax != null && currentMin != currentMax) {
                    g.drawPolyline(
                        line.x(startLine),
                        line.relativeY(currentMin, currentMax, paddingY, height - paddingY * 2),
                        line.size)

                    if (showLimit) {
                        g.font = limitMetrics.font
                        val minWidth = limitMetrics.getStringWidth(minValue.toPrettyString())
                        val maxWidth = limitMetrics.getStringWidth(maxValue.toPrettyString())
                        val minMargin = if (minWidth < maxWidth) maxWidth - minWidth else 0
                        val maxMargin = if (maxWidth < minWidth) minWidth - maxWidth else 0
                        g.drawString(minValue.toPrettyString(), minMargin, height - paddingY)
                        g.drawString(maxValue.toPrettyString(), maxMargin, limitMetrics.fontBaseline + paddingY)
                    }
                }
            }

            g.font = valueMetrics.font
            val currentStringWidth = valueMetrics.getStringWidth(strValue)
            val y = height / 2 + valueMetrics.fontBaseline / 2
            g.drawString(strValue, width - currentStringWidth, y)
        }
    }

    override fun toString() = value.toString()

    private fun Double?.toPrettyString(): String {
        var str = this?.toString() ?: return "null"
        if ('.' in str) {
            str = str.trimEnd('0').trimEnd('.')
        }
        return str
    }
}
