package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.bold
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
    fun displayMessage(number: Int? = null, count: Int? = null) = buildString {
        val maxLineLength = (dishes.map { it.name.length  + 3 /*padding*/ } + name.length).max()!!

        appendln(name.bold() + ":")

        for (dish in dishes)
            appendln(" - ".code() + dish.name.code())

        appendln()
        appendln("Цена: ${cost.toString().bold()}")

        if (number != null && count != null) {
            appendln()

            val numDisplay = "$number/$count"
            val numFill = (maxLineLength - numDisplay.length) / 2
            append("-".repeat(numFill).code())
            append(numDisplay.code())
            append("-".repeat(numFill).code())
        }
    }
}