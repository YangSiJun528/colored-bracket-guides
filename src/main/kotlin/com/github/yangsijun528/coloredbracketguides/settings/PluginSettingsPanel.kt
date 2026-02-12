package com.github.yangsijun528.coloredbracketguides.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class PluginSettingsPanel {

    private val enabledCheckBox = JBCheckBox("Enable Colored Bracket Guides")
    private val verticalGuideCheckBox = JBCheckBox("Enable vertical guide lines")
    private val horizontalGuideCheckBox = JBCheckBox("Enable horizontal guide lines")

    val panel: JPanel = FormBuilder.createFormBuilder()
        .addComponent(enabledCheckBox)
        .addComponent(verticalGuideCheckBox)
        .addComponent(horizontalGuideCheckBox)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    fun isModified(state: PluginSettings.State): Boolean {
        return enabledCheckBox.isSelected != state.enabled
                || verticalGuideCheckBox.isSelected != state.verticalGuideEnabled
                || horizontalGuideCheckBox.isSelected != state.horizontalGuideEnabled
    }

    fun applyTo(state: PluginSettings.State) {
        state.enabled = enabledCheckBox.isSelected
        state.verticalGuideEnabled = verticalGuideCheckBox.isSelected
        state.horizontalGuideEnabled = horizontalGuideCheckBox.isSelected
    }

    fun resetFrom(state: PluginSettings.State) {
        enabledCheckBox.isSelected = state.enabled
        verticalGuideCheckBox.isSelected = state.verticalGuideEnabled
        horizontalGuideCheckBox.isSelected = state.horizontalGuideEnabled
    }
}
