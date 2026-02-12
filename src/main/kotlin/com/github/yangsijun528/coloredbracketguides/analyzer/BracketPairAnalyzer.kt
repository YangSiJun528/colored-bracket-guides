package com.github.yangsijun528.coloredbracketguides.analyzer

import com.intellij.codeInsight.highlighting.BraceMatchingUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType

data class BracketPair(
    val openOffset: Int,
    val closeOffset: Int,
    val depth: Int,
    val openLine: Int,
    val closeLine: Int
)

class BracketPairAnalyzer(
    private val editor: Editor,
    private val fileType: FileType
) {

    fun analyzeBrackets(viewportStartOffset: Int, viewportEndOffset: Int): List<BracketPair> {
        val highlighter = editor.highlighter
        val document = editor.document
        val text = document.charsSequence
        val iterator = highlighter.createIterator(0)
        val stack = ArrayDeque<Pair<Int, Int>>() // (offset, depth at push time)
        val pairs = mutableListOf<BracketPair>()

        while (!iterator.atEnd()) {
            val tokenStart = iterator.start

            if (BraceMatchingUtil.isLBraceToken(iterator, text, fileType)) {
                val depth = stack.size
                stack.addLast(tokenStart to depth)
            } else if (BraceMatchingUtil.isRBraceToken(iterator, text, fileType)) {
                if (stack.isNotEmpty()) {
                    val (openOffset, depth) = stack.removeLast()
                    val openLine = document.getLineNumber(openOffset)
                    val closeLine = document.getLineNumber(tokenStart)

                    pairs.add(BracketPair(openOffset, tokenStart, depth, openLine, closeLine))
                }
            }

            iterator.advance()
        }

        return pairs
    }
}
