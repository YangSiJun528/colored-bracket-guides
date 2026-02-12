package com.github.yangsijun528.coloredbracketguides.renderer

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.github.yangsijun528.coloredbracketguides.settings.PluginSettings
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D

class HorizontalGuideRenderer {

    fun createRenderer(color: Color, isActive: Boolean, state: PluginSettings.State): CustomHighlighterRenderer {
        return CustomHighlighterRenderer { editor, highlighter, g ->
            paintHorizontalGuide(editor, highlighter, g, color, isActive, state)
        }
    }

    private fun paintHorizontalGuide(
        editor: Editor,
        highlighter: RangeHighlighter,
        g: Graphics,
        color: Color,
        isActive: Boolean,
        state: PluginSettings.State
    ) {
        val g2d = g as Graphics2D
        val offset = highlighter.startOffset
        val line = editor.document.getLineNumber(offset)
        val lineStartOffset = editor.document.getLineStartOffset(line)

        val bracketX = editor.offsetToXY(offset).x
        val guideX = editor.offsetToXY(lineStartOffset).x

        val y = editor.offsetToXY(offset).y + editor.lineHeight

        if (guideX >= bracketX) return

        g2d.color = color
        val lineWidth = if (isActive) state.activeLineWidth else state.lineWidth
        g2d.stroke = BasicStroke(lineWidth.toFloat())
        g2d.drawLine(guideX, y, bracketX, y)
    }
}
