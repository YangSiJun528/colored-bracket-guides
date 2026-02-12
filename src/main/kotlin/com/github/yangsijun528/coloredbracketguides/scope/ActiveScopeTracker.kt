package com.github.yangsijun528.coloredbracketguides.scope

import com.intellij.openapi.editor.Editor
import com.github.yangsijun528.coloredbracketguides.analyzer.BracketPair

object ActiveScopeTracker {

    fun findActiveScope(editor: Editor, pairs: List<BracketPair>): BracketPair? {
        val caretOffset = editor.caretModel.offset

        return pairs
            .filter { it.openOffset <= caretOffset && caretOffset <= it.closeOffset }
            .minByOrNull { it.closeOffset - it.openOffset }
    }
}
