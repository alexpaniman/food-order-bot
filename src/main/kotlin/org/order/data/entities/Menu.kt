package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Dishes
import org.order.data.tables.Menus
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.Schedule

class Menu(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Menu>(Menus)

    var name by Menus.name
    var cost by Menus.cost
    var schedule by Menus.schedule
            .transform(
                    { it.toString() },
                    { Schedule.parse(it) }
            )

    val dishes by Dish referrersOn Dishes.menu

    fun buildDescription(): String {
        val dishes = buildString {
            for (dish in dishes) {
//                val rates = PollAnswer TODO
//                        .find { PollAnswers.dish eq dish.id }
//                        .groupBy { it.order.orderDate }
//                        .toSortedMap(compareByDescending { it })
//                        .toList().take(2)
//                        .flatMap { (_, answers) -> answers }
//                        .map { it.rate }

//                val rate = if (rates.isNotEmpty())
//                    rates.sum() / rates.size.toFloat()
//                else 0f

                appendln(Text.get("dish-description") {
                    it["name"] = dish.name
                })
//                } + if (rate != 0f)
//                    " " + Text.get("dish-rate") {
//                        it["rate"] = rate.toString()
//                    } else ""
            }
        }

        return Text.get("menu-description") {
            it["name"] = name
            it["cost"] = cost.toString()
            it["dishes"] = dishes
        }
    }
}