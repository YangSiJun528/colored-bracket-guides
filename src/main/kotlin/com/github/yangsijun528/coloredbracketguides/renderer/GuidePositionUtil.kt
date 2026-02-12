package com.github.yangsijun528.coloredbracketguides.renderer

import com.intellij.openapi.editor.Editor
import com.github.yangsijun528.coloredbracketguides.analyzer.BracketPair

/**
 * Guide Column (GC) calculation result.
 *
 * @param gcX the pixel x-coordinate for the vertical guide
 * @param obCol column of the opening bracket's first non-whitespace on its line
 * @param cbCol column of the closing bracket's first non-whitespace on its line
 * @param closeBracketCol column of the closing bracket character itself
 * @param needsObConnector true if OB is to the right of GC (col_start > GC)
 * @param needsCbConnector true if CB is to the right of GC (col_end > GC)
 * @param gcMatchesCbChar true if GC x-position matches the closing bracket character position
 */
data class GuideInfo(
    val gcX: Int,
    val obCol: Int,
    val cbCol: Int,
    val closeBracketCol: Int,
    val needsObConnector: Boolean,
    val needsCbConnector: Boolean,
    val gcMatchesCbChar: Boolean
)

object GuidePositionUtil {

    /**
     * Calculate the Guide Column (GC) and connector requirements.
     *
     * Algorithm:
     *   col_start = first non-ws column of OB line
     *   col_end   = first non-ws column of CB line
     *   IE_cols   = first non-ws columns of body lines (openLine+1 .. closeLine-1), non-empty only
     *   GC = min(col_end, min(IE_cols))
     *   (col_start is excluded — OB line text does not influence GC position)
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

        // GC starts from CB column; OB line text does not pull the guide left
        var gcCol = cbCol

        // Check interior/body lines (openLine+1 .. closeLine-1) for minimum indent
        for (line in (pair.openLine + 1) until pair.closeLine) {
            val lineStart = document.getLineStartOffset(line)
            val lineEnd = document.getLineEndOffset(line)
            val col = firstNonWsCol(chars, lineStart, lineEnd) ?: continue // skip empty/whitespace-only
            gcCol = minOf(gcCol, col)
        }

        // Column of the closing bracket character itself (not the first non-ws on its line)
        val cbLineStart = document.getLineStartOffset(pair.closeLine)
        val closeBracketCol = pair.closeOffset - cbLineStart

        // Convert gcCol to pixel x — use CB line as reference for offset→pixel mapping
        val gcX = editor.offsetToXY(cbLineStart + gcCol.coerceAtMost(
            document.getLineEndOffset(pair.closeLine) - cbLineStart
        )).x

        return GuideInfo(
            gcX = gcX,
            obCol = obCol,
            cbCol = cbCol,
            closeBracketCol = closeBracketCol,
            needsObConnector = obCol > gcCol,
            needsCbConnector = cbCol > gcCol,
            gcMatchesCbChar = gcCol == closeBracketCol
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
