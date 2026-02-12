package com.github.yangsijun528.coloredbracketguides.settings

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.psi.tree.IElementType
import com.intellij.openapi.editor.highlighter.HighlighterClient
import com.intellij.lexer.Lexer
import com.intellij.lexer.EmptyLexer

class NullHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer(): Lexer = EmptyLexer()
    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = emptyArray()
}
