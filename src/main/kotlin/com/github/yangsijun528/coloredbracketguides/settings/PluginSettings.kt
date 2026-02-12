package com.github.yangsijun528.coloredbracketguides.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic

@State(
    name = "ColoredBracketGuides",
    storages = [Storage("ColoredBracketGuides.xml")]
)
class PluginSettings : PersistentStateComponent<PluginSettings.State> {

    data class State(
        var enabled: Boolean = true,
        var verticalGuideEnabled: Boolean = true,
        var horizontalGuideEnabled: Boolean = true,
        var displayMode: DisplayMode = DisplayMode.ALL,
        var lineWidth: Float = 0.7f,
        var activeLineWidth: Float = 1.0f,
        var activeLineStyle: LineStyle = LineStyle.SOLID,
        var inactiveOpacity: Float = 0.15f,

        var depthColors: MutableList<String> = mutableListOf(
            "#FFD700", "#DA70D6", "#179FFF",
            "#00CC7A", "#FF6B6B", "#CC8833"
        ),
        var cycleColors: Boolean = true,

        var caretDebounceMs: Int = 50,
        var maxNestingDepth: Int = 30
    )

    enum class DisplayMode { ALL, ACTIVE_ONLY }
    enum class LineStyle { SOLID, DASHED, DOTTED }

    private var state = State()

    override fun getState(): State = state
    override fun loadState(state: State) {
        this.state = state
    }

    fun fireSettingsChanged() {
        ApplicationManager.getApplication().messageBus
            .syncPublisher(SETTINGS_CHANGED_TOPIC)
            .onSettingsChanged()
    }

    companion object {
        val SETTINGS_CHANGED_TOPIC = Topic.create(
            "ColoredBracketGuides settings changed",
            SettingsChangedListener::class.java
        )

        fun getInstance(): PluginSettings =
            ApplicationManager.getApplication()
                .getService(PluginSettings::class.java)
    }

    fun interface SettingsChangedListener {
        fun onSettingsChanged()
    }
}
