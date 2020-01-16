package org.order.logic.impl.utils

fun Int.orZero(range: IntRange) = if (this in range) this else 0