package com.github.yangsijun528.coloredbracketguides.analyzer

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.lang.BracePair
import com.intellij.openapi.editor.Document

data class BracketPair(
    val openOffset: Int,
    val closeOffset: Int,
    val depth: Int
)

class BracketPairAnalyzer(private val editor: Editor) {

    private val cache = BracketPairCache()

    fun analyzeBrackets(startOffset: Int, endOffset: Int): List<BracketPair> {
        val cached = cache.get(startOffset, endOffset)
        if (cached != null) return cached

        val pairs = findBracketPairs(startOffset, endOffset)
        cache.put(startOffset, endOffset, pairs)
        return pairs
    }

    private fun findBracketPairs(startOffset: Int, endOffset: Int): List<BracketPair> {
        val highlighter = editor.highlighter ?: return emptyList()
        val iterator = highlighter.createIterator(startOffset)
        val stack = ArrayDeque<Int>()
        val pairs = mutableListOf<BracketPair>()

        while (!iterator.atEnd() && iterator.start < endOffset) {
            val tokenType = iterator.tokenType

            if (isBraceToken(iterator, true)) {
                stack.addLast(iterator.start)
            } else if (isBraceToken(iterator, false)) {
                if (stack.isNotEmpty()) {
                    val openOffset = stack.removeLast()
                    pairs.add(BracketPair(openOffset, iterator.start, stack.size))
                }
            }

            iterator.advance()
        }

        return pairs
    }

    private fun isBraceToken(iterator: HighlighterIterator, left: Boolean): Boolean {
        val fileType = editor.project?.let {
            editor.virtualFile?.fileType
        } ?: return false

        val matcher = com.intellij.codeInsight.highlighting.BraceMatchingUtil.getBraceMatcher(
            fileType, iterator
        ) ?: return false

        return if (left) {
            matcher.isLBraceToken(iterator, editor.document.text, fileType)
        } else {
            matcher.isRBraceToken(iterator, editor.document.text, fileType)
        }
    }

    fun invalidateCache() {
        cache.invalidate()
    }
}
