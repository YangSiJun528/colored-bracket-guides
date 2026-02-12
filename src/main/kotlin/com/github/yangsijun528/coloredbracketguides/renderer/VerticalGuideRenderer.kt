package com.github.yangsijun528.coloredbracketguides.renderer

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.github.yangsijun528.coloredbracketguides.settings.PluginSettings
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D

class VerticalGuideRenderer {

    fun createRenderer(color: Color, isActive: Boolean, state: PluginSettings.State): CustomHighlighterRenderer {
        return CustomHighlighterRenderer { editor, highlighter, g ->
            paintVerticalGuide(editor, highlighter, g, color, isActive, state)
        }
    }

    private fun paintVerticalGuide(
        editor: Editor,
        highlighter: RangeHighlighter,
        g: Graphics,
        color: Color,
        isActive: Boolean,
        state: PluginSettings.State
    ) {
        val g2d = g as Graphics2D
        val startOffset = highlighter.startOffset
        val endOffset = highlighter.endOffset

        val startLine = editor.document.getLineNumber(startOffset)
        val endLine = editor.document.getLineNumber(endOffset)

        val lineStartOffset = editor.document.getLineStartOffset(startLine)
        val indent = startOffset - lineStartOffset

        val x = editor.offsetToXY(lineStartOffset).x + indent * editor.columnWidth()
        val yStart = editor.offsetToXY(editor.document.getLineStartOffset(startLine + 1)).y
        val yEnd = editor.offsetToXY(editor.document.getLineStartOffset(endLine)).y

        if (yStart >= yEnd) return

        g2d.color = color
        val lineWidth = if (isActive) state.activeLineWidth else state.lineWidth
        g2d.stroke = createStroke(lineWidth.toFloat(), state.activeLineStyle, isActive)
        g2d.drawLine(x, yStart, x, yEnd)
    }

    private fun createStroke(width: Float, style: PluginSettings.LineStyle, isActive: Boolean): BasicStroke {
        if (!isActive || style == PluginSettings.LineStyle.SOLID) {
            return BasicStroke(width)
        }
        return when (style) {
            PluginSettings.LineStyle.DASHED -> BasicStroke(
                width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, floatArrayOf(6.0f, 4.0f), 0.0f
            )
            PluginSettings.LineStyle.DOTTED -> BasicStroke(
                width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER,
                10.0f, floatArrayOf(2.0f, 4.0f), 0.0f
            )
            else -> BasicStroke(width)
        }
    }

    private fun Editor.columnWidth(): Int {
        return this.contentComponent.getFontMetrics(this.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)).charWidth(' ')
    }
}
