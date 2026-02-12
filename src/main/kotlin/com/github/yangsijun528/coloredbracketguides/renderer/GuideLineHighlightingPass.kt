package com.github.yangsijun528.coloredbracketguides.renderer

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.github.yangsijun528.coloredbracketguides.analyzer.BracketPair
import com.github.yangsijun528.coloredbracketguides.analyzer.BracketPairAnalyzer
import com.github.yangsijun528.coloredbracketguides.scope.ActiveScopeTracker
import com.github.yangsijun528.coloredbracketguides.settings.PluginSettings
import com.github.yangsijun528.coloredbracketguides.util.ColorUtil

class GuideLineHighlightingPass(
    project: Project,
    private val editor: Editor,
    private val psiFile: PsiFile
) : TextEditorHighlightingPass(project, editor.document) {

    private var bracketPairs: List<BracketPair> = emptyList()

    override fun doCollectInformation(progress: ProgressIndicator) {
        val settings = PluginSettings.getInstance()
        if (!settings.state.enabled) return

        val analyzer = BracketPairAnalyzer(editor, psiFile.fileType)
        bracketPairs = analyzer.analyzeBrackets(0, editor.document.textLength)
    }

    override fun doApplyInformationToEditor() {
        ActiveScopeTracker.ensureCaretListenerRegistered(editor, psiFile)

        clearHighlighters()

        val settings = PluginSettings.getInstance()
        if (!settings.state.enabled) return

        val activePair = ActiveScopeTracker.findActiveScope(editor, bracketPairs)

        for (pair in bracketPairs) {
            val isActive = pair == activePair
            val color = ColorUtil.getColorForDepth(pair.depth, settings.state)
            val effectiveColor = if (isActive) color else ColorUtil.dimColor(color, settings.state.inactiveOpacity)

            if (settings.state.displayMode == PluginSettings.DisplayMode.ACTIVE_ONLY && !isActive) continue

            val isMultiLine = pair.closeLine > pair.openLine
            val guideInfo = GuidePositionUtil.calcGuideInfo(editor, pair)

            if (guideInfo != null) {
                val document = editor.document

                if (isMultiLine) {
                    // Vertical line at GC
                    if (settings.state.verticalGuideEnabled) {
                        val renderer = VerticalGuideRenderer.createRenderer(pair, guideInfo, effectiveColor, isActive, settings.state)
                        addHighlighter(pair.openOffset, pair.closeOffset, renderer)
                    }

                    if (settings.state.horizontalGuideEnabled) {
                        // OB horizontal line: GC → OB bracket right edge
                        val obLineStart = document.getLineStartOffset(pair.openLine)
                        val obLineEnd = (pair.openOffset + 1).coerceAtMost(document.textLength)
                        if (obLineStart < obLineEnd) {
                            val renderer = HorizontalGuideRenderer.createObConnector(pair, guideInfo, effectiveColor, isActive, settings.state)
                            addLineHighlighter(obLineStart, obLineEnd, renderer)
                        }

                        // CB horizontal line: GC → CB bracket right edge
                        val cbLineStart = document.getLineStartOffset(pair.closeLine)
                        val cbLineEnd = (pair.closeOffset + 1).coerceAtMost(document.textLength)
                        if (cbLineStart < cbLineEnd) {
                            val renderer = HorizontalGuideRenderer.createCbConnector(pair, guideInfo, effectiveColor, isActive, settings.state)
                            addLineHighlighter(cbLineStart, cbLineEnd, renderer)
                        }
                    }
                } else if (settings.state.horizontalGuideEnabled) {
                    // Single-line pair: horizontal underline from OB to CB
                    val lineStart = document.getLineStartOffset(pair.openLine)
                    val lineEnd = (pair.closeOffset + 1).coerceAtMost(document.textLength)
                    if (lineStart < lineEnd) {
                        val renderer = HorizontalGuideRenderer.createSingleLineConnector(pair, effectiveColor, isActive, settings.state)
                        addLineHighlighter(lineStart, lineEnd, renderer)
                    }
                }
            }

            // FR-06: Rainbow-color all bracket characters
            val bracketAttrs = TextAttributes().apply {
                foregroundColor = color
                if (isActive) {
                    backgroundColor = ColorUtil.dimColor(color, 0.25f)
                }
            }
            addBracketHighlighter(pair.openOffset, pair.openOffset + 1, bracketAttrs)
            if (pair.closeOffset + 1 <= editor.document.textLength) {
                addBracketHighlighter(pair.closeOffset, pair.closeOffset + 1, bracketAttrs)
            }
        }
    }

    private fun addHighlighter(startOffset: Int, endOffset: Int, renderer: CustomHighlighterRenderer) {
        addHighlighterInternal(startOffset, endOffset, renderer, HighlighterTargetArea.EXACT_RANGE)
    }

    private fun addLineHighlighter(startOffset: Int, endOffset: Int, renderer: CustomHighlighterRenderer) {
        addHighlighterInternal(startOffset, endOffset, renderer, HighlighterTargetArea.LINES_IN_RANGE)
    }

    private fun addHighlighterInternal(
        startOffset: Int, endOffset: Int,
        renderer: CustomHighlighterRenderer,
        targetArea: HighlighterTargetArea
    ) {
        if (startOffset >= endOffset || endOffset > editor.document.textLength) return

        val highlighter = editor.markupModel.addRangeHighlighter(
            startOffset,
            endOffset,
            HighlighterLayer.FIRST - 100,
            null,
            targetArea
        )
        highlighter.customRenderer = renderer

        val list = editor.getUserData(HIGHLIGHTERS_KEY) ?: mutableListOf<RangeHighlighter>().also {
            editor.putUserData(HIGHLIGHTERS_KEY, it)
        }
        list.add(highlighter)
    }

    private fun addBracketHighlighter(startOffset: Int, endOffset: Int, attrs: TextAttributes) {
        if (startOffset >= endOffset || endOffset > editor.document.textLength) return

        val highlighter = editor.markupModel.addRangeHighlighter(
            startOffset,
            endOffset,
            HighlighterLayer.LAST + 100,
            attrs,
            HighlighterTargetArea.EXACT_RANGE
        )

        val list = editor.getUserData(HIGHLIGHTERS_KEY) ?: mutableListOf<RangeHighlighter>().also {
            editor.putUserData(HIGHLIGHTERS_KEY, it)
        }
        list.add(highlighter)
    }

    private fun clearHighlighters() {
        val list = editor.getUserData(HIGHLIGHTERS_KEY) ?: return
        for (highlighter in list) {
            if (highlighter.isValid) {
                editor.markupModel.removeHighlighter(highlighter)
            }
        }
        list.clear()
    }

    companion object {
        private val HIGHLIGHTERS_KEY = Key.create<MutableList<RangeHighlighter>>(
            "colored.bracket.guides.highlighters"
        )
    }
}
