package com.github.yangsijun528.coloredbracketguides.renderer

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.CustomHighlighterOrder
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
import java.awt.Color
import java.awt.Graphics

class GuideLineHighlightingPass(
    project: Project,
    private val editor: Editor,
    private val psiFile: PsiFile
) : TextEditorHighlightingPass(project, editor.document) {

    private var bracketPairs: List<BracketPair> = emptyList()

    override fun doCollectInformation(progress: ProgressIndicator) {
        val settings = PluginSettings.getInstance()
        if (!settings.state.enabled) return

        val document = editor.document
        val currentModStamp = document.modificationStamp
        val cachedModStamp = ActiveScopeTracker.getCachedModStamp(editor)

        if (cachedModStamp != null && cachedModStamp == currentModStamp) {
            bracketPairs = ActiveScopeTracker.getCachedPairs(editor)
            return
        }

        val analyzer = BracketPairAnalyzer(editor, psiFile.fileType)
        bracketPairs = analyzer.analyzeBrackets(0, document.textLength)
        ActiveScopeTracker.cachePairs(editor, bracketPairs, currentModStamp)
    }

    override fun doApplyInformationToEditor() {
        ActiveScopeTracker.ensureCaretListenerRegistered(editor, psiFile)

        clearHighlighters()

        val settings = PluginSettings.getInstance()
        if (!settings.state.enabled) return

        for (pair in bracketPairs) {
            val color = ColorUtil.getColorForDepth(pair.depth, settings.state)
            val isMultiLine = pair.closeLine > pair.openLine
            val guideInfo = GuidePositionUtil.calcGuideInfo(editor, pair)

            if (guideInfo != null) {
                val document = editor.document

                if (isMultiLine) {
                    if (settings.state.verticalGuideEnabled) {
                        val renderer = createDynamicRenderer(pair, color) { ed, p, c, active, st ->
                            VerticalGuideRenderer.createRenderer(p, guideInfo, c, active, st)
                        }
                        addHighlighter(pair.openOffset, pair.closeOffset, renderer)
                    }

                    if (settings.state.horizontalGuideEnabled) {
                        val obLineStart = document.getLineStartOffset(pair.openLine)
                        val obLineEnd = (pair.openOffset + 1).coerceAtMost(document.textLength)
                        if (obLineStart < obLineEnd) {
                            val renderer = createDynamicRenderer(pair, color) { ed, p, c, active, st ->
                                HorizontalGuideRenderer.createObConnector(p, guideInfo, c, active, st)
                            }
                            addLineHighlighter(obLineStart, obLineEnd, renderer)
                        }

                        val cbLineStart = document.getLineStartOffset(pair.closeLine)
                        val cbLineEnd = (pair.closeOffset + 1).coerceAtMost(document.textLength)
                        if (cbLineStart < cbLineEnd) {
                            val renderer = createDynamicRenderer(pair, color) { ed, p, c, active, st ->
                                HorizontalGuideRenderer.createCbConnector(p, guideInfo, c, active, st)
                            }
                            addLineHighlighter(cbLineStart, cbLineEnd, renderer)
                        }
                    }
                } else if (settings.state.horizontalGuideEnabled) {
                    val lineStart = document.getLineStartOffset(pair.openLine)
                    val lineEnd = (pair.closeOffset + 1).coerceAtMost(document.textLength)
                    if (lineStart < lineEnd) {
                        val renderer = createDynamicRenderer(pair, color) { ed, p, c, active, st ->
                            HorizontalGuideRenderer.createSingleLineConnector(p, c, active, st)
                        }
                        addLineHighlighter(lineStart, lineEnd, renderer)
                    }
                }
            }

            // Rainbow-color bracket characters — dynamic active check
            addDynamicBracketHighlighter(pair, pair.openOffset)
            if (pair.closeOffset + 1 <= editor.document.textLength) {
                addDynamicBracketHighlighter(pair, pair.closeOffset)
            }
        }
    }

    /**
     * Creates a renderer that dynamically resolves active scope and color at paint time.
     * This way, caret movement only needs repaint() — no highlighter rebuild needed.
     */
    private fun createDynamicRenderer(
        pair: BracketPair,
        baseColor: Color,
        factory: (Editor, BracketPair, Color, Boolean, PluginSettings.State) -> CustomHighlighterRenderer
    ): CustomHighlighterRenderer {
        return object : CustomHighlighterRenderer {
            override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
                val settings = PluginSettings.getInstance()
                val pairs = ActiveScopeTracker.getCachedPairs(editor)
                val activePair = ActiveScopeTracker.findActiveScope(editor, pairs)
                val isActive = pair == activePair

                if (settings.state.displayMode == PluginSettings.DisplayMode.ACTIVE_ONLY && !isActive) return

                val effectiveColor = if (isActive) baseColor else ColorUtil.dimColor(baseColor, settings.state.inactiveOpacity)
                val delegate = factory(editor, pair, effectiveColor, isActive, settings.state)
                delegate.paint(editor, highlighter, g)
            }

            override fun getOrder(): CustomHighlighterOrder = CustomHighlighterOrder.AFTER_BACKGROUND
        }
    }

    private fun addDynamicBracketHighlighter(pair: BracketPair, offset: Int) {
        if (offset + 1 > editor.document.textLength) return

        val settings = PluginSettings.getInstance()
        val fgColor = ColorUtil.getBracketForeground(pair.depth, settings.state)
        val attrs = TextAttributes().apply { foregroundColor = fgColor }

        val highlighter = editor.markupModel.addRangeHighlighter(
            offset,
            offset + 1,
            HighlighterLayer.LAST + 100,
            attrs,
            HighlighterTargetArea.EXACT_RANGE
        )

        highlighter.customRenderer = object : CustomHighlighterRenderer {
            override fun paint(editor: Editor, hl: RangeHighlighter, g: Graphics) {
                val st = PluginSettings.getInstance().state
                if (!st.bracketBackgroundEnabled) return

                val pairs = ActiveScopeTracker.getCachedPairs(editor)
                val activePair = ActiveScopeTracker.findActiveScope(editor, pairs)
                if (pair != activePair) return

                val bgColor = ColorUtil.getBracketBackground(pair.depth, st)
                val point = editor.offsetToXY(offset)
                val nextX = editor.offsetToXY(offset + 1).x
                val g2d = g as java.awt.Graphics2D
                g2d.color = bgColor
                g2d.fillRect(point.x, point.y, nextX - point.x, editor.lineHeight)
            }

            override fun getOrder(): CustomHighlighterOrder = CustomHighlighterOrder.AFTER_BACKGROUND
        }

        val list = editor.getUserData(HIGHLIGHTERS_KEY) ?: mutableListOf<RangeHighlighter>().also {
            editor.putUserData(HIGHLIGHTERS_KEY, it)
        }
        list.add(highlighter)
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
