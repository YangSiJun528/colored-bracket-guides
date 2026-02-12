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

object HorizontalGuideRenderer {

    /**
     * OB horizontal connector: GC → OB at line bottom.
     */
    fun createObConnector(
        pair: BracketPair,
        guideInfo: GuideInfo,
        color: Color,
        isActive: Boolean,
        state: PluginSettings.State
    ): CustomHighlighterRenderer {
        return createConnectorRenderer(pair.openOffset, guideInfo, color, isActive, state)
    }

    /**
     * CB horizontal connector: GC → CB at line bottom.
     */
    fun createCbConnector(
        pair: BracketPair,
        guideInfo: GuideInfo,
        color: Color,
        isActive: Boolean,
        state: PluginSettings.State
    ): CustomHighlighterRenderer {
        return createConnectorRenderer(pair.closeOffset, guideInfo, color, isActive, state)
    }

    /**
     * Single-line pair: horizontal underline from OB to CB at line bottom.
     */
    fun createSingleLineConnector(
        pair: BracketPair,
        color: Color,
        isActive: Boolean,
        state: PluginSettings.State
    ): CustomHighlighterRenderer {
        return object : CustomHighlighterRenderer {
            override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val document = editor.document
                // OB right edge to CB left edge (not touching either bracket)
                val obRightX = if (pair.openOffset + 1 <= document.textLength) {
                    editor.offsetToXY(pair.openOffset + 1).x
                } else {
                    editor.offsetToXY(pair.openOffset).x
                }
                val cbLeftX = editor.offsetToXY(pair.closeOffset).x
                if (obRightX >= cbLeftX) return

                val lineTopY = editor.offsetToXY(document.getLineStartOffset(pair.openLine)).y
                val y = lineTopY + editor.lineHeight

                val lineWidth = if (isActive) state.activeLineWidth else state.lineWidth
                g2d.color = color
                g2d.stroke = BasicStroke(lineWidth)
                g2d.drawLine(obRightX, y, cbLeftX, y)
            }

            override fun getOrder(): CustomHighlighterOrder = CustomHighlighterOrder.AFTER_BACKGROUND
        }
    }

    private fun createConnectorRenderer(
        bracketOffset: Int,
        guideInfo: GuideInfo,
        color: Color,
        isActive: Boolean,
        state: PluginSettings.State
    ): CustomHighlighterRenderer {
        return object : CustomHighlighterRenderer {
            override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val gcX = guideInfo.gcX
                // Draw from GC to bracket's left edge (not touching the bracket)
                val bracketLeftX = editor.offsetToXY(bracketOffset).x

                val leftX = minOf(gcX, bracketLeftX)
                val rightX = maxOf(gcX, bracketLeftX)
                if (leftX >= rightX) return

                val document = editor.document
                val bracketLine = document.getLineNumber(bracketOffset)
                val lineTopY = editor.offsetToXY(document.getLineStartOffset(bracketLine)).y
                val y = lineTopY + editor.lineHeight

                val lineWidth = if (isActive) state.activeLineWidth else state.lineWidth
                g2d.color = color
                g2d.stroke = BasicStroke(lineWidth)
                g2d.drawLine(leftX, y, rightX, y)
            }

            override fun getOrder(): CustomHighlighterOrder = CustomHighlighterOrder.AFTER_BACKGROUND
        }
    }
}
