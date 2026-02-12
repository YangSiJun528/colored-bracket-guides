package com.github.yangsijun528.coloredbracketguides.settings

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import java.io.File

object SettingsImportExport {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun exportSettings(project: Project?) {
        val descriptor = FileSaverDescriptor(
            "Export Colored Bracket Guides Settings",
            "Save settings to a JSON file",
            "json"
        )
        val wrapper = FileChooserFactory.getInstance()
            .createSaveFileDialog(descriptor, project)
            .save("colored-bracket-guides-settings.json") ?: return

        val state = PluginSettings.getInstance().state
        val json = gson.toJson(state)
        wrapper.file.writeText(json)
    }

    fun importSettings(project: Project?) {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withFileFilter { it.extension == "json" }
            .withTitle("Import Colored Bracket Guides Settings")

        val files = FileChooserFactory.getInstance()
            .createFileChooser(descriptor, project, null)
            .choose(project)

        if (files.isEmpty()) return

        val file = File(files[0].path)
        val json = file.readText()
        val imported = gson.fromJson(json, PluginSettings.State::class.java)
        PluginSettings.getInstance().loadState(imported)
        PluginSettings.getInstance().fireSettingsChanged()
    }
}
