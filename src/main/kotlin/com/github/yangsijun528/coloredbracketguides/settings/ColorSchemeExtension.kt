package com.github.yangsijun528.coloredbracketguides.settings

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class ColorSchemeExtension : ColorSettingsPage {

    override fun getIcon(): Icon? = null

    override fun getHighlighter() = NullHighlighter()

    override fun getDemoText(): String = "// Bracket guide colors are configured here"

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "Colored Bracket Guides"

    companion object {
        private const val MAX_DEPTH_COLORS = 6

        val DEPTH_KEYS: Array<TextAttributesKey> = Array(MAX_DEPTH_COLORS) { i ->
            TextAttributesKey.createTextAttributesKey("COLORED_BRACKET_GUIDE_DEPTH_${i + 1}")
        }

        private val DESCRIPTORS: Array<AttributesDescriptor> = Array(MAX_DEPTH_COLORS) { i ->
            AttributesDescriptor("Depth ${i + 1}", DEPTH_KEYS[i])
        }
    }
}
