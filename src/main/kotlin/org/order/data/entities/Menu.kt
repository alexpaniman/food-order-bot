package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.code
import org.order.data.tables.Dishes
import org.order.data.tables.Menus

class Menu(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Menu>(Menus)

    var name by Menus.name
    var cost by Menus.cost

    var active   by Menus.active
    var schedule by Menus.schedule

    private val dishes by Dish referrersOn Dishes.menu
    fun displayMessage(number: Int? = null, count: Int? = null, maxLineLength: Int = 0) = buildString {
        appendln("Меню".code() + " " + name.code() + ":" + System.lineSeparator())

        for (dish in dishes)
            appendln(" - ".code() + dish.name.code())

        appendln()
        append("Цена: " + cost.toString().code())

        if (number != null && count != null) {
            appendln()

            val numDisplay = "$number/$count"
            val fillLength = ((maxLineLength - numDisplay.length) / 2).coerceAtLeast(0)
            val fill = "-".repeat(fillLength).code()
            append(fill + numDisplay.code() + fill)
        }
    }
}