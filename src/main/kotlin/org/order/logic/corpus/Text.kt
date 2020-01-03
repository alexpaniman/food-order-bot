package org.order.logic.corpus

import java.io.File
import java.lang.StringBuilder

object Text {
    private fun String.substringBetween(first: Char, second: Char) =
            substringAfter(first).substringBefore(second)

    private fun String.trimAfter(delimiter: Char) =
            substringAfter(delimiter).trim()

    private val corpus: Map<String, String> = File("src/main/resources/text-corpus.txt")
            .readText().lines()
            .fold(mutableListOf<Pair<String, StringBuilder>>()) { list, text ->
                list.apply {
                    when {
                        text matches "^\\[[a-z:\\-]+].*$".toRegex() -> {
                            this += Pair(
                                    text.substringBetween('[', ']'),
                                    StringBuilder(text.trimAfter(']'))
                            )
                        }

                        text matches "^[^#].*$".toRegex() -> {
                            this
                                    .lastOrNull()
                                    ?.second
                                    ?.append(text.trim())
                        }
                    }
                }
            }
            .map { (key, value) ->
                Pair(key, value.replace("\\[(br|tab|space)]".toRegex()) {
                    when (it.value) {
                        "[br]" -> "\n"
                        "[tab]" -> "\t"
                        "[space]" -> " "
                        else -> error("impossible")
                    }
                })
            }
            .toMap()

    fun get(key: String, init: (MutableMap<String, String>) -> Unit): String {
        val map = mutableMapOf<String, String>()
        init(map)

        for ((textKey, text) in corpus)
            println("$textKey \n  $text \n\n")

        var text = corpus[key] ?: error("illegal key: $key")
        for ((occurrence, value) in map)
            text = text.replace("`$occurrence`", value)

        return text
    }

    operator fun get(key: String) = get(key) {}
}