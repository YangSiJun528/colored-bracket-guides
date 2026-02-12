package com.github.yangsijun528.coloredbracketguides.util

import com.github.yangsijun528.coloredbracketguides.settings.PluginSettings
import java.awt.Color

object ColorUtil {

    fun getColorForDepth(depth: Int, state: PluginSettings.State): Color {
        val colors = state.depthColors
        if (colors.isEmpty()) return Color.GRAY

        val index = if (state.cycleColors) {
            depth % colors.size
        } else {
            depth.coerceAtMost(colors.size - 1)
        }

        return parseHexColor(colors[index])
    }

    fun dimColor(color: Color, opacity: Float): Color {
        val alpha = (opacity * 255).toInt().coerceIn(0, 255)
        return Color(color.red, color.green, color.blue, alpha)
    }

    private fun parseHexColor(hex: String): Color {
        val cleaned = hex.removePrefix("#")
        return try {
            Color(cleaned.toInt(16))
        } catch (e: NumberFormatException) {
            Color.GRAY
        }
    }
}
