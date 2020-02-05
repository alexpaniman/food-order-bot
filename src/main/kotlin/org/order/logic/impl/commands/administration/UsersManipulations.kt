package org.order.logic.impl.commands.administration

private fun longestCommonSubstring(s1: String, s2: String): Int {
    val dynamicLine = IntArray(s1.length + 1) { 0 }
    var max = 0

    for (charS2 in s2) {
        for ((indexS1, charS1) in s1.withIndex().reversed())
            dynamicLine[indexS1 + 1] =
                    if (charS1 == charS2)
                        dynamicLine[indexS1] + 1
                    else 0

        val currentMax = dynamicLine.max() ?: 0

        if (max < currentMax)
            max = currentMax

    }

    return max
}

// TODO