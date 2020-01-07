package org.order.logic.impl.commands.display

import org.joda.time.DateTimeConstants.FRIDAY
import org.joda.time.DateTimeConstants.MONDAY
import org.joda.time.LocalDate
import org.order.bot.send.button
import org.order.bot.send.row
import org.order.data.entities.Order
import org.order.data.entities.State.COMMAND
import org.order.data.entities.Student
import org.order.data.tables.Orders
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.commands.triggers.and
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.LOCALE

private val ORDERS_LIST_WINDOW_TRIGGER = CommandTrigger(Text["orders-list-command"]) and StateTrigger(COMMAND)
val ORDERS_LIST_WINDOW = Window("orders-list-window", ORDERS_LIST_WINDOW_TRIGGER, listOf("0")) { user, (numStr) ->
    val now = LocalDate.now()

    val numInt = numStr.toInt()
    val num = if (numInt + now.dayOfWeek <= FRIDAY)
        numInt
    else 0

    val bound = now.plusDays(num)

    val orders = buildString {
        val groupedByGrade = Order.find { Orders.orderDate eq "$bound" }
                .groupBy {
                    if (user.hasLinked(Student))
                        user.linked(Student).grade!!.name
                    else Text["empty-class"]
                }

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

                for (order in byMenu)
                    appendln(Text.get("user-display") {
                        it["user-name"] = order.madeBy.name!!
                    })
            }
        }
    }

    val message = Text.get("orders-list") {
        it["orders"] = orders
    }

    val dayOfWeek = now.dayOfWeek

    show(message) {
        row {
            if (dayOfWeek > MONDAY)
                button(Text["previous-order-day"], "orders-list-window:${num - 1}")
            else
                button(Text["inactive"])

            button(now.dayOfWeek().getAsShortText(LOCALE))

            if (dayOfWeek < FRIDAY)
                button(Text["next-order-day"], "orders-list-window:${num + 1}")
            else
                button(Text["inactive"])
        }

        button(Text["update-order-window"], "orders-list-window:$num")

        button(Text["cancel"], "remove-window")
    }
}