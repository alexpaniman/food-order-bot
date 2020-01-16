package org.order.logic.impl.commands.display

import org.order.data.entities.State.COMMAND
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.commands.triggers.and
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.clients
import org.order.logic.impl.utils.dayOfWeekAsText

private const val WINDOW_MARKER = "my-orders-list-window"

private val MY_ORDERS_LIST_WINDOW_TRIGGER = CommandTrigger(Text["my-orders-window"]) and StateTrigger(COMMAND)
val MY_ORDERS_LIST_WINDOW = Window(
        WINDOW_MARKER,
        MY_ORDERS_LIST_WINDOW_TRIGGER,
        listOf()) { user, (clientNumStr) ->

    val clients = user.clients()
    val clientNum = clientNumStr.toInt().coerceIn(clients.indices)
    val client = clients[clientNum]

    val orders = client.orders
    val ordersGroupedByDay = orders
            .groupBy { order ->
        order.orderDate
    }

    val ordersDisplay = buildString {
        for ((day, byDay) in ordersGroupedByDay) {
            appendln(Text.get("my-orders-list:day-of-week") {
                it["name"] = day.dayOfWeekAsText
            })

            for (order in byDay)
                appendln(Text.get("my-orders-list:day-of-week") {
                    it["description"] = order.menu.buildDescription()
                })
        }
    }

    val message = Text.get("my-orders-list:list") {
        it["orders"] = ordersDisplay
    }

    show(message) {

    }
}