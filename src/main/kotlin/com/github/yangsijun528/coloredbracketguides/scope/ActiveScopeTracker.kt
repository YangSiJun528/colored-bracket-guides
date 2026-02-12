package com.github.yangsijun528.coloredbracketguides.scope

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.github.yangsijun528.coloredbracketguides.analyzer.BracketPair
import com.github.yangsijun528.coloredbracketguides.settings.PluginSettings
import javax.swing.Timer

object ActiveScopeTracker {

    private val LISTENER_DISPOSABLE_KEY = Key.create<Disposable>(
        "colored.bracket.guides.caret.listener.disposable"
    )

    fun findActiveScope(editor: Editor, pairs: List<BracketPair>): BracketPair? {
        val caretOffset = editor.caretModel.offset

        return pairs
            .filter { it.openOffset <= caretOffset && caretOffset <= it.closeOffset }
            .minByOrNull { it.closeOffset - it.openOffset }
    }

    fun ensureCaretListenerRegistered(editor: Editor, psiFile: PsiFile) {
        if (editor.getUserData(LISTENER_DISPOSABLE_KEY) != null) return

        val project = psiFile.project
        val disposable = Disposer.newDisposable("ColoredBracketGuides-CaretListener")
        editor.putUserData(LISTENER_DISPOSABLE_KEY, disposable)

        val settings = PluginSettings.getInstance()
        val debounceMs = settings.state.caretDebounceMs

        val timer = Timer(debounceMs) {
            if (!project.isDisposed && !editor.isDisposed) {
                val currentPsiFile = PsiManager.getInstance(project).findFile(
                    editor.virtualFile ?: return@Timer
                ) ?: return@Timer
                DaemonCodeAnalyzer.getInstance(project).restart(currentPsiFile)
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
