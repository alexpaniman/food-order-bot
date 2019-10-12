package org.order.logic.text

object TextCorpus {
    private val corpus = TextCorpus::class.java.getResource("text-corpus.txt")
            .readText().lines()
            .filter { it.isNotBlank() }
            .map { it.substringBefore(":") to it.substringAfter(":") }
            .toMap()

    fun get(key: String, init: (MutableMap<String, String>) -> Unit): String {
        val map = mutableMapOf<String, String>()
        init(map)

        var text = corpus[key] ?: error("illegal key")
        for ((occurrence, value) in map)
            text = text.replace("`$occurrence`", value)

        return text
    }

    operator fun get(key: String) = get(key) {}
}