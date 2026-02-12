package com.github.yangsijun528.coloredbracketguides.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class PluginConfigurable : Configurable {

    private var settingsPanel: PluginSettingsPanel? = null

    override fun getDisplayName(): String = "Colored Bracket Guides"

    override fun createComponent(): JComponent {
        settingsPanel = PluginSettingsPanel()
        return settingsPanel!!.panel
    }

    override fun isModified(): Boolean {
        val settings = PluginSettings.getInstance()
        return settingsPanel?.isModified(settings.state) ?: false
    }

    override fun apply() {
        val settings = PluginSettings.getInstance()
        settingsPanel?.applyTo(settings.state)
        settings.fireSettingsChanged()
    }

    override fun reset() {
        val settings = PluginSettings.getInstance()
        settingsPanel?.resetFrom(settings.state)
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
