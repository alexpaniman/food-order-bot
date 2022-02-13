package org.order.logic.impl.utils

import com.jakewharton.picnic.Table
import com.jakewharton.picnic.TableDsl
import com.jakewharton.picnic.TextAlignment

fun Int.orZero(range: IntRange) = if (this in range) this else 0
operator fun <T> List<T>.component6() = this[5]
operator fun <T> List<T>.component7() = this[6]

fun TableDsl.defaultSettings() =
        cellStyle {
            border = true
            alignment = TextAlignment.MiddleCenter
            paddingRight = 1; paddingLeft = 1
        }


fun Table.stringifyTable() = this.toString()
        .replace('─', '-')
        .replace('│', '|')
        .replace("[┼┐┌└┘┤├┬┴]".toRegex(), " ")
        .lines().joinToString("\n") { "`$it`" }