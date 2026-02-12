package com.github.yangsijun528.coloredbracketguides.settings

import com.intellij.ui.ColorPanel
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class PluginSettingsPanel {

    // ── 1. General ──
    private val enabledCheckBox = JBCheckBox("Enable Colored Bracket Guides")
    private val displayModeCombo = JComboBox(PluginSettings.DisplayMode.entries.toTypedArray())

    // ── 2. Color Preset ──
    private val colorPanels = mutableListOf<ColorPanel>()
    private val colorListPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 4))
    private val addColorButton = JButton("+")
    private val removeColorButton = JButton("-")
    private val cycleColorsCheckBox = JBCheckBox("Cycle colors when depth exceeds palette size")

    // ── 3. Guide Lines ──
    private val verticalGuideCheckBox = JBCheckBox("Show vertical guide lines")
    private val horizontalGuideCheckBox = JBCheckBox("Show horizontal guide lines")
    private val lineWidthSpinner = JSpinner(SpinnerNumberModel(0.7, 0.1, 5.0, 0.1))
    private val activeLineWidthSpinner = JSpinner(SpinnerNumberModel(1.0, 0.1, 5.0, 0.1))
    private val lineStyleCombo = JComboBox(PluginSettings.LineStyle.entries.toTypedArray())
    private val inactiveOpacitySpinner = JSpinner(SpinnerNumberModel(0.15, 0.0, 1.0, 0.05))

    // ── 4. Bracket Highlight ──
    private val bracketFgPanel = ColorPanel()
    private val bracketFgClearButton = JButton("Clear")
    private var bracketFgCustom: Boolean = false

    private val bracketBgEnabledCheckBox = JBCheckBox("Show background highlight on active brackets")
    private val bracketBgPanel = ColorPanel()
    private val bracketBgClearButton = JButton("Clear")
    private var bracketBgCustom: Boolean = false
    private val bracketBgOpacitySpinner = JSpinner(SpinnerNumberModel(0.25, 0.0, 1.0, 0.05))

    val panel: JPanel

    init {
        // Section 1: General
        val generalSection = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckBox)
            .addLabeledComponent(JBLabel("Display mode:"), displayModeCombo)
            .panel
        generalSection.border = IdeBorderFactory.createTitledBorder("General")

        // Section 2: Color Preset
        val colorButtonsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            add(addColorButton)
            add(removeColorButton)
        }
        addColorButton.addActionListener { addColorSwatch(null) }
        removeColorButton.addActionListener { removeLastColorSwatch() }

        val colorPresetContent = JPanel(BorderLayout()).apply {
            add(colorListPanel, BorderLayout.CENTER)
            add(colorButtonsPanel, BorderLayout.SOUTH)
        }

        val colorSection = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Depth colors:"), colorPresetContent)
            .addComponent(cycleColorsCheckBox)
            .panel
        colorSection.border = IdeBorderFactory.createTitledBorder("Color Preset")

        // Section 3: Guide Lines
        val guideSection = FormBuilder.createFormBuilder()
            .addComponent(verticalGuideCheckBox)
            .addComponent(horizontalGuideCheckBox)
            .addLabeledComponent(JBLabel("Inactive line width:"), lineWidthSpinner)
            .addLabeledComponent(JBLabel("Active line width:"), activeLineWidthSpinner)
            .addLabeledComponent(JBLabel("Active line style:"), lineStyleCombo)
            .addLabeledComponent(JBLabel("Inactive opacity:"), inactiveOpacitySpinner)
            .panel
        guideSection.border = IdeBorderFactory.createTitledBorder("Guide Lines")

        // Section 4: Bracket Highlight
        val bracketFgRow = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            add(bracketFgPanel)
            add(bracketFgClearButton)
            add(JBLabel("(blank = use rainbow)"))
        }
        bracketFgClearButton.addActionListener {
            bracketFgCustom = false
            bracketFgPanel.selectedColor = null
        }

        val bracketBgRow = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            add(bracketBgPanel)
            add(bracketBgClearButton)
            add(JBLabel("(blank = use rainbow)"))
        }
        bracketBgClearButton.addActionListener {
            bracketBgCustom = false
            bracketBgPanel.selectedColor = null
        }

        val bracketSection = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Foreground color:"), bracketFgRow)
            .addComponent(bracketBgEnabledCheckBox)
            .addLabeledComponent(JBLabel("Background color:"), bracketBgRow)
            .addLabeledComponent(JBLabel("Background opacity:"), bracketBgOpacitySpinner)
            .panel
        bracketSection.border = IdeBorderFactory.createTitledBorder("Bracket Highlight")

        panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(generalSection)
            add(colorSection)
            add(guideSection)
            add(bracketSection)
        }
    }

    fun isModified(state: PluginSettings.State): Boolean {
        return enabledCheckBox.isSelected != state.enabled
                || displayModeCombo.selectedItem != state.displayMode
                || getColorList() != state.depthColors
                || cycleColorsCheckBox.isSelected != state.cycleColors
                || verticalGuideCheckBox.isSelected != state.verticalGuideEnabled
                || horizontalGuideCheckBox.isSelected != state.horizontalGuideEnabled
                || spinnerFloat(lineWidthSpinner) != state.lineWidth
                || spinnerFloat(activeLineWidthSpinner) != state.activeLineWidth
                || lineStyleCombo.selectedItem != state.activeLineStyle
                || spinnerFloat(inactiveOpacitySpinner) != state.inactiveOpacity
                || colorToHex(bracketFgPanel.selectedColor) != state.bracketForegroundColor
                || bracketBgEnabledCheckBox.isSelected != state.bracketBackgroundEnabled
                || colorToHex(bracketBgPanel.selectedColor) != state.bracketBackgroundColor
                || spinnerFloat(bracketBgOpacitySpinner) != state.bracketBackgroundOpacity
    }

    fun applyTo(state: PluginSettings.State) {
        state.enabled = enabledCheckBox.isSelected
        state.displayMode = displayModeCombo.selectedItem as PluginSettings.DisplayMode
        state.depthColors = getColorList()
        state.cycleColors = cycleColorsCheckBox.isSelected
        state.verticalGuideEnabled = verticalGuideCheckBox.isSelected
        state.horizontalGuideEnabled = horizontalGuideCheckBox.isSelected
        state.lineWidth = spinnerFloat(lineWidthSpinner)
        state.activeLineWidth = spinnerFloat(activeLineWidthSpinner)
        state.activeLineStyle = lineStyleCombo.selectedItem as PluginSettings.LineStyle
        state.inactiveOpacity = spinnerFloat(inactiveOpacitySpinner)
        state.bracketForegroundColor = colorToHex(bracketFgPanel.selectedColor)
        state.bracketBackgroundEnabled = bracketBgEnabledCheckBox.isSelected
        state.bracketBackgroundColor = colorToHex(bracketBgPanel.selectedColor)
        state.bracketBackgroundOpacity = spinnerFloat(bracketBgOpacitySpinner)
    }

    fun resetFrom(state: PluginSettings.State) {
        enabledCheckBox.isSelected = state.enabled
        displayModeCombo.selectedItem = state.displayMode
        setColorList(state.depthColors)
        cycleColorsCheckBox.isSelected = state.cycleColors
        verticalGuideCheckBox.isSelected = state.verticalGuideEnabled
        horizontalGuideCheckBox.isSelected = state.horizontalGuideEnabled
        lineWidthSpinner.value = state.lineWidth.toDouble()
        activeLineWidthSpinner.value = state.activeLineWidth.toDouble()
        lineStyleCombo.selectedItem = state.activeLineStyle
        inactiveOpacitySpinner.value = state.inactiveOpacity.toDouble()
        bracketFgPanel.selectedColor = hexToColor(state.bracketForegroundColor)
        bracketBgEnabledCheckBox.isSelected = state.bracketBackgroundEnabled
        bracketBgPanel.selectedColor = hexToColor(state.bracketBackgroundColor)
        bracketBgOpacitySpinner.value = state.bracketBackgroundOpacity.toDouble()
    }

    // ── Color list helpers ──

    private fun addColorSwatch(color: Color?) {
        val cp = ColorPanel()
        cp.selectedColor = color ?: Color.WHITE
        colorPanels.add(cp)
        colorListPanel.add(cp)
        colorListPanel.revalidate()
        colorListPanel.repaint()
    }

    private fun removeLastColorSwatch() {
        if (colorPanels.isEmpty()) return
        val last = colorPanels.removeLast()
        colorListPanel.remove(last)
        colorListPanel.revalidate()
        colorListPanel.repaint()
    }

    private fun getColorList(): MutableList<String> {
        return colorPanels.mapNotNull { cp ->
            cp.selectedColor?.let { colorToHex(it) }
        }.filter { it.isNotEmpty() }.toMutableList()
    }

    private fun setColorList(colors: List<String>) {
        colorPanels.clear()
        colorListPanel.removeAll()
        for (hex in colors) {
            val c = hexToColor(hex)
            if (c != null) addColorSwatch(c)
        }
        colorListPanel.revalidate()
        colorListPanel.repaint()
    }

    // ── Conversion helpers ──

    private fun colorToHex(color: Color?): String {
        if (color == null) return ""
        return String.format("#%06X", color.rgb and 0xFFFFFF)
    }

    private fun hexToColor(hex: String): Color? {
        val cleaned = hex.trim().removePrefix("#")
        if (cleaned.length != 6) return null
        return try {
            Color(cleaned.toInt(16))
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun spinnerFloat(spinner: JSpinner): Float {
        return (spinner.value as Double).toFloat()
    }
}
