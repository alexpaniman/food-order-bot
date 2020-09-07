package org.order.logic.impl.commands.display

import org.joda.time.DateTimeConstants.*
import org.joda.time.LocalDate
import org.order.bot.send.button
import org.order.bot.send.deactivatableKeyButton
import org.order.bot.send.row
import org.order.data.entities.*
import org.order.data.entities.State.COMMAND
import org.order.data.tables.Orders
import org.order.logic.commands.triggers.*
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.dayOfWeekAsShortText
import org.order.logic.impl.utils.grade

private val ORDERS_LIST_WINDOW_TRIGGER = CommandTrigger(Text["orders-list-command"]) and
        StateTrigger(COMMAND) and (RoleTrigger(Client) or RoleTrigger(Parent) or RoleTrigger(Producer))

val ORDERS_LIST_WINDOW = Window("orders-list-window", ORDERS_LIST_WINDOW_TRIGGER,
        args = listOf("0")) { _, (dayNumStr) ->
    val now = LocalDate.now()

    val days = when (now.dayOfWeek) {
        // In sunday and saturday you can see entire next week
        SATURDAY, SUNDAY -> {
            val nextMonday = now.plusWeeks(1)
                    .plusDays(MONDAY - now.dayOfWeek)

            (0..4).map { nextMonday.plusDays(it) }
        }
        // Else you can see orders from today and until the Friday inclusive
        else -> {
            (0..(FRIDAY - now.dayOfWeek))
                    .map { now.plusDays(it) }
        }
    }
    val dayNum = dayNumStr.toInt()
            .coerceIn(days.indices)

    val chosenDate = days[dayNum]
    val orders = Order
            .find { Orders.orderDate eq chosenDate.toString() }
            .filter { !it.canceled }

    val ordersDisplay = buildString {
        val groupedByGrade = orders
                .groupBy { it.client.user.grade }

        for ((grade, byGrade) in groupedByGrade) {
            appendln(Text.get("grade-display") {
                it["grade"] = grade
                it["orders-count"] = "${byGrade.size}"
            })

            val groupedByMenu = byGrade
                    .groupBy { it.menu.name }

            for ((menu, byMenu) in groupedByMenu) {
                appendln(Text.get("menu-display") {
                    it["menu-name"] = menu
                    it["orders-count"] = "${byMenu.size}"
                })

                appendln("```") // TODO?
                for (order in byMenu)
                    appendln(Text.get("user-display") {
                        it["user-name"] = order.client.user.name!!
                    })
                append("```") // TODO?

                appendln()
            }

            appendln()
        }

        val groupedByMenu = orders
                .groupBy { it.menu }

        for ((menu, byMenu) in groupedByMenu)
            appendln(Text.get("orders-list-total:menu-display") {
                it["name"] = menu.name
                it["count"] = byMenu.size.toString()
            })

        appendln()
        append(Text.get("orders-list-total:total-display") {
            it["count"] = orders.size.toString()
        })
    }

    val message = if (ordersDisplay.isNotBlank())
        Text.get("orders-list") {
            it["orders"] = ordersDisplay
        }
    else
        Text["there-were-no-orders-made-yet"]

    show(message) {
        row {
            deactivatableKeyButton("previous-button", "orders-list-window:${dayNum - 1}") {
                dayNum - 1 >= 0
            }

            button(chosenDate.dayOfWeekAsShortText)

            deactivatableKeyButton("next-button", "orders-list-window:${dayNum + 1}") {
                dayNum + 1 < days.size
            }
        }

        button(Text["update-button"], "orders-list-window:$dayNum")

        button(Text["cancel-button"], "remove-message") // TODO some weird stuff happens with this button fix it
    }
}