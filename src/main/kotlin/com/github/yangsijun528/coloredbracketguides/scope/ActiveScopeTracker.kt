package com.github.yangsijun528.coloredbracketguides.scope

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.github.yangsijun528.coloredbracketguides.analyzer.BracketPair
import com.github.yangsijun528.coloredbracketguides.settings.PluginSettings
import javax.swing.Timer

object ActiveScopeTracker {

    private val LISTENER_DISPOSABLE_KEY = Key.create<Disposable>(
        "colored.bracket.guides.caret.listener.disposable"
    )

    private val CACHED_PAIRS_KEY = Key.create<Pair<Long, List<BracketPair>>>(
        "colored.bracket.guides.bracket.cache"
    )

    fun findActiveScope(editor: Editor, pairs: List<BracketPair>): BracketPair? {
        val caretOffset = editor.caretModel.offset

        return pairs
            .filter { it.openOffset < caretOffset && caretOffset <= it.closeOffset }
            .minByOrNull { it.closeOffset - it.openOffset }
    }

    fun getCachedPairs(editor: Editor): List<BracketPair> {
        return editor.getUserData(CACHED_PAIRS_KEY)?.second ?: emptyList()
    }

    fun cachePairs(editor: Editor, pairs: List<BracketPair>, modStamp: Long) {
        editor.putUserData(CACHED_PAIRS_KEY, modStamp to pairs)
    }

    fun getCachedModStamp(editor: Editor): Long? {
        return editor.getUserData(CACHED_PAIRS_KEY)?.first
    }

    fun ensureCaretListenerRegistered(editor: Editor, psiFile: PsiFile) {
        if (editor.getUserData(LISTENER_DISPOSABLE_KEY) != null) return

        val disposable = Disposer.newDisposable("ColoredBracketGuides-CaretListener")
        editor.putUserData(LISTENER_DISPOSABLE_KEY, disposable)

        val settings = PluginSettings.getInstance()
        val debounceMs = settings.state.caretDebounceMs

        val timer = Timer(debounceMs) {
            if (!editor.isDisposed) {
                editor.contentComponent.repaint()
            }
        }
        timer.isRepeats = false

        val listener = object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                timer.restart()
            }
        }

        editor.caretModel.addCaretListener(listener, disposable)

        Disposer.register(disposable, Disposable {
            timer.stop()
        })
    }
}
