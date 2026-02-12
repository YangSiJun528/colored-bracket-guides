package com.github.yangsijun528.coloredbracketguides.renderer

import com.intellij.openapi.editor.Editor
import com.github.yangsijun528.coloredbracketguides.analyzer.BracketPair

/**
 * Guide Column (GC) calculation result.
 *
 * @param gcCol the column index (0-based character count) for the vertical guide
 * @param gcX the pixel x-coordinate for the vertical guide
 * @param obCol column of the opening bracket's first non-whitespace on its line
 * @param cbCol column of the closing bracket's first non-whitespace on its line
 * @param needsObConnector true if OB is to the right of GC (col_start > GC)
 * @param needsCbConnector true if CB is to the right of GC (col_end > GC)
 */
data class GuideInfo(
    val gcX: Int,
    val obCol: Int,
    val cbCol: Int,
    val needsObConnector: Boolean,
    val needsCbConnector: Boolean
)

object GuidePositionUtil {

    /**
     * Calculate the Guide Column (GC) and connector requirements.
     *
     * Algorithm:
     *   col_start = first non-ws column of OB line
     *   col_end   = first non-ws column of CB line
     *   IE_cols   = first non-ws columns of body lines (openLine+1 .. closeLine-1), non-empty only
     *   GC = min(col_start, col_end, min(IE_cols))
     *
     * Returns null if no valid position found.
     */
    fun calcGuideInfo(editor: Editor, pair: BracketPair): GuideInfo? {
        val document = editor.document
        val chars = document.charsSequence

        val obCol = firstNonWsCol(chars, document.getLineStartOffset(pair.openLine), document.getLineEndOffset(pair.openLine))
            ?: return null
        val cbCol = firstNonWsCol(chars, document.getLineStartOffset(pair.closeLine), document.getLineEndOffset(pair.closeLine))
            ?: return null

        var gcCol = minOf(obCol, cbCol)

        // Check interior/body lines for minimum indent
        for (line in (pair.openLine + 1) until pair.closeLine) {
            val lineStart = document.getLineStartOffset(line)
            val lineEnd = document.getLineEndOffset(line)
            val col = firstNonWsCol(chars, lineStart, lineEnd) ?: continue // skip empty/whitespace-only
            gcCol = minOf(gcCol, col)
        }

        // Convert gcCol to pixel x — use OB line as reference for offset→pixel mapping
        val refLineStart = document.getLineStartOffset(pair.openLine)
        val gcX = editor.offsetToXY(refLineStart + gcCol.coerceAtMost(
            document.getLineEndOffset(pair.openLine) - refLineStart
        )).x

        return GuideInfo(
            gcX = gcX,
            obCol = obCol,
            cbCol = cbCol,
            needsObConnector = obCol > gcCol,
            needsCbConnector = cbCol > gcCol
        )
    }

    /**
     * Returns the column index of the first non-whitespace character in the given line range,
     * or null if the line is empty or whitespace-only.
     */
    private fun firstNonWsCol(chars: CharSequence, lineStart: Int, lineEnd: Int): Int? {
        if (lineStart >= lineEnd) return null
        for (i in lineStart until lineEnd) {
            if (!chars[i].isWhitespace()) return i - lineStart
        }
        return null
    }
}
