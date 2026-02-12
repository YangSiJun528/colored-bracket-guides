package com.github.yangsijun528.coloredbracketguides.renderer

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.github.yangsijun528.coloredbracketguides.analyzer.BracketPair
import com.github.yangsijun528.coloredbracketguides.analyzer.BracketPairAnalyzer
import com.github.yangsijun528.coloredbracketguides.settings.PluginSettings
import com.github.yangsijun528.coloredbracketguides.scope.ActiveScopeTracker
import com.github.yangsijun528.coloredbracketguides.util.ColorUtil

class GuideLineHighlightingPass(
    project: Project,
    private val editor: Editor
) : TextEditorHighlightingPass(project, editor.document) {

    private val analyzer = BracketPairAnalyzer(editor)
    private val verticalRenderer = VerticalGuideRenderer()
    private val horizontalRenderer = HorizontalGuideRenderer()
    private var bracketPairs: List<BracketPair> = emptyList()
    private val highlighters = mutableListOf<RangeHighlighter>()

    override fun doCollectInformation(progress: ProgressIndicator) {
        val settings = PluginSettings.getInstance()
        if (!settings.state.enabled) return

        val visibleArea = editor.scrollingModel.visibleArea
        val startOffset = editor.xyToLogicalPosition(java.awt.Point(0, visibleArea.y)).let {
            editor.logicalPositionToOffset(it)
        }
        val endOffset = editor.xyToLogicalPosition(
            java.awt.Point(0, visibleArea.y + visibleArea.height)
        ).let {
            editor.logicalPositionToOffset(it)
        }

        bracketPairs = analyzer.analyzeBrackets(startOffset, endOffset)
    }

    override fun doApplyInformationToEditor() {
        clearHighlighters()

        val settings = PluginSettings.getInstance()
        if (!settings.state.enabled) return

        val activePair = ActiveScopeTracker.findActiveScope(editor, bracketPairs)

        for (pair in bracketPairs) {
            val isActive = pair == activePair
            val color = ColorUtil.getColorForDepth(pair.depth, settings.state)
            val effectiveColor = if (isActive) color else ColorUtil.dimColor(color, settings.state.inactiveOpacity)

            if (settings.state.displayMode == PluginSettings.DisplayMode.ACTIVE_ONLY && !isActive) continue

            if (settings.state.verticalGuideEnabled) {
                val renderer = verticalRenderer.createRenderer(effectiveColor, isActive, settings.state)
                addHighlighter(pair.openOffset, pair.closeOffset, renderer)
            }

            if (settings.state.horizontalGuideEnabled && isActive) {
                val openRenderer = horizontalRenderer.createRenderer(effectiveColor, isActive, settings.state)
                addHighlighter(pair.openOffset, pair.openOffset + 1, openRenderer)

                if (pair.closeOffset < editor.document.textLength) {
                    val closeRenderer = horizontalRenderer.createRenderer(effectiveColor, isActive, settings.state)
                    addHighlighter(pair.closeOffset, pair.closeOffset + 1, closeRenderer)
                }
            }
        }
    }

    private fun addHighlighter(startOffset: Int, endOffset: Int, renderer: CustomHighlighterRenderer) {
        if (startOffset >= endOffset || endOffset > editor.document.textLength) return

        val highlighter = editor.markupModel.addRangeHighlighter(
            startOffset,
            endOffset,
            HighlighterLayer.SELECTION,
            null,
            HighlighterTargetArea.EXACT_RANGE
        )
        highlighter.customRenderer = renderer
        highlighters.add(highlighter)
    }

    private fun clearHighlighters() {
        for (highlighter in highlighters) {
            editor.markupModel.removeHighlighter(highlighter)
        }
        highlighters.clear()
    }
}
