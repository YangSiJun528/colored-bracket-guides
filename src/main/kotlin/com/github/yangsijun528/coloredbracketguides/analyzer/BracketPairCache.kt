package com.github.yangsijun528.coloredbracketguides.analyzer

class BracketPairCache {

    private data class CacheKey(val startOffset: Int, val endOffset: Int)

    private var cachedPairs: Map<CacheKey, List<BracketPair>> = emptyMap()

    fun get(startOffset: Int, endOffset: Int): List<BracketPair>? {
        return cachedPairs[CacheKey(startOffset, endOffset)]
    }

    fun put(startOffset: Int, endOffset: Int, pairs: List<BracketPair>) {
        cachedPairs = cachedPairs + (CacheKey(startOffset, endOffset) to pairs)
    }

    fun invalidate() {
        cachedPairs = emptyMap()
    }
}
