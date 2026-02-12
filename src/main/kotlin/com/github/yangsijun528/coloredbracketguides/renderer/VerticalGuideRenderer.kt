package com.github.yangsijun528.coloredbracketguides.renderer

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.CustomHighlighterOrder
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.github.yangsijun528.coloredbracketguides.analyzer.BracketPair
import com.github.yangsijun528.coloredbracketguides.settings.PluginSettings
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints

object VerticalGuideRenderer {

    fun createRenderer(
        pair: BracketPair,
        guideInfo: GuideInfo,
        color: Color,
        isActive: Boolean,
        state: PluginSettings.State
    ): CustomHighlighterRenderer {
        return object : CustomHighlighterRenderer {
            override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
                paintVerticalGuide(editor, pair, guideInfo, g, color, isActive, state)
            }

            override fun getOrder(): CustomHighlighterOrder = CustomHighlighterOrder.AFTER_BACKGROUND
        }
    }

    private fun paintVerticalGuide(
        editor: Editor,
        pair: BracketPair,
        guideInfo: GuideInfo,
        g: Graphics,
        color: Color,
        isActive: Boolean,
        state: PluginSettings.State
    ) {
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val document = editor.document
        val foldingModel = editor.foldingModel

        // Check if entire range is folded
        val foldRegion = foldingModel.getCollapsedRegionAtOffset(pair.openOffset)
        if (foldRegion != null && foldRegion.endOffset >= pair.closeOffset) return

        val x = guideInfo.gcX

        // Vertical line must NOT touch the bracket lines.
        // Starts at OB next line top, ends at CB line top.
        val yStart: Int
        val yEnd: Int

        if (pair.openLine + 1 <= pair.closeLine) {
            val nextLineAfterOb = pair.openLine + 1
            yStart = editor.offsetToXY(document.getLineStartOffset(nextLineAfterOb)).y
            yEnd = editor.offsetToXY(document.getLineStartOffset(pair.closeLine)).y
        } else {
            return
        }

        if (yStart >= yEnd) return

        g2d.color = color
        val lineWidth = if (isActive) state.activeLineWidth else state.lineWidth
        g2d.stroke = createStroke(lineWidth, state.activeLineStyle, isActive)
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
        }
    }
}
