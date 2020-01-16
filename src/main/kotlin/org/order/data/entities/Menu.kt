package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.joda.time.*
import org.joda.time.DateTimeConstants.*
import org.order.data.tables.Dishes
import org.order.data.tables.Menus
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.LAST_ORDER_TIME
import org.order.logic.impl.utils.Schedule
import kotlin.math.ceil

class Menu(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Menu>(Menus)

    var name by Menus.name
    var cost by Menus.cost
    var schedule by Menus.schedule
            .transform(
                    { it.toString() },
                    { Schedule.parse(it) }
            )

    var active by Menus.active
    private val dishes by Dish referrersOn Dishes.menu

    fun buildDescription(): String {
        val dishes = buildString {
            for (dish in dishes)
                appendln(Text.get("dish-description") {
                    it["name"] = dish.name
                })
        }

        return Text.get("menu-description") {
            it["name"] = name
            it["cost"] = cost.toString()
            it["dishes"] = dishes
        }
    }
}