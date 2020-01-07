package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.order.data.tables.Dishes
import org.order.data.tables.Menus
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.LAST_ORDER_TIME

class Menu(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Menu>(Menus)

    var name by Menus.name
    var cost by Menus.cost
    private var schedule by Menus.schedule
    private var active by Menus.active
    private val dishes by Dish referrersOn Dishes.menu

    fun isAvailableNow(): Boolean {
        val now = LocalDate.now()

        val bound = LocalDate.now().toDateTime(LAST_ORDER_TIME)

        val weekSkip = schedule / 7 // Number of weeks that this menu skips
        val dayOfWeek = schedule % 7 // Day of week when this menu is active

        val workDate = DateTime.now().plusDays(dayOfWeek - now.dayOfWeek)

        return active && if (now.dayOfWeek <= DateTimeConstants.FRIDAY)
            now.weekOfWeekyear % weekSkip == 0 && !workDate.isAfter(bound)
        else
            now.weekOfWeekyear % weekSkip == 1
    }

    fun nextActiveDate(): LocalDate {
        val now = LocalDate.now()

        val weekSkip = schedule / 7
        val dayOfWeek = schedule % 7

        return now.plusDays(dayOfWeek - now.dayOfWeek)
                .plusWeeks(now.weekOfWeekyear % weekSkip)
    }

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