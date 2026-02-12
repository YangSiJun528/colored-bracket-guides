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

    /**
     * Resolve bracket foreground color.
     * If customHex is blank, falls back to rainbow color for the given depth.
     */
    fun getBracketForeground(depth: Int, state: PluginSettings.State): Color {
        val custom = state.bracketForegroundColor.trim()
        if (custom.isNotEmpty()) {
            val parsed = tryParseHex(custom)
            if (parsed != null) return parsed
        }
        return getColorForDepth(depth, state)
    }

    /**
     * Resolve bracket background color.
     * If customHex is blank, falls back to rainbow color for the given depth
     * with bracketBackgroundOpacity applied.
     */
    fun getBracketBackground(depth: Int, state: PluginSettings.State): Color {
        val custom = state.bracketBackgroundColor.trim()
        if (custom.isNotEmpty()) {
            val parsed = tryParseHex(custom)
            if (parsed != null) return dimColor(parsed, state.bracketBackgroundOpacity)
        }
        return dimColor(getColorForDepth(depth, state), state.bracketBackgroundOpacity)
    }

    fun dimColor(color: Color, opacity: Float): Color {
        val alpha = (opacity * 255).toInt().coerceIn(0, 255)
        return Color(color.red, color.green, color.blue, alpha)
    }

    private fun parseHexColor(hex: String): Color {
        return tryParseHex(hex) ?: Color.GRAY
    }

    private fun tryParseHex(hex: String): Color? {
        val cleaned = hex.trim().removePrefix("#")
        if (!cleaned.matches(Regex("[0-9A-Fa-f]{6}"))) return null
        return try {
            Color(cleaned.toInt(16))
        } catch (e: NumberFormatException) {
            null
        }
    }
}
