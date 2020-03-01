package org.order.logic.impl.utils

fun Int.orZero(range: IntRange) = if (this in range) this else 0
operator fun <T> List<T>.component6() = this[5]
operator fun <T> List<T>.component7() = this[6]