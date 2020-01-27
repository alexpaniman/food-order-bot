package org.order.logic.impl.commands.display

import org.order.bot.send.button
import org.order.data.entities.Client
import org.order.data.entities.Parent
import org.order.data.entities.State.COMMAND
import org.order.logic.commands.triggers.*
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.clients
import org.order.logic.impl.utils.dayOfWeekAsLongText
import org.order.logic.impl.utils.dayOfWeekAsShortText

private const val WINDOW_MARKER = "my-orders-list-window"

private val MY_ORDERS_LIST_WINDOW_TRIGGER =
        CommandTrigger(Text["my-orders-command"]) and
                StateTrigger(COMMAND) and
                (RoleTrigger(Client) or RoleTrigger(Parent))

val MY_ORDERS_LIST_WINDOW = Window(
        WINDOW_MARKER,
        MY_ORDERS_LIST_WINDOW_TRIGGER,
        listOf("0")) { user, (clientNumStr) ->

    val clients = user.clients()
    val clientNum = clientNumStr.toInt().coerceIn(clients.indices)
    val client = clients[clientNum]

    val orders = client.orders
            .filter { !it.canceled }

    val ordersGroupedByDay = orders
            .groupBy { order -> order.orderDate }

    val ordersDisplay = ordersGroupedByDay.toList()
            .joinToString("\n\n") { (day, byDay) ->
                Text.get("my-orders-list:day-of-week") {
                    it["name"] = day.dayOfWeekAsLongText
                } + "\n        " +
                        byDay.joinToString("\n\n        ") { order ->
                            Text.get("my-orders-list:order") {
                                it["description"] = order.menu.buildDescription()
                            }.lines().joinToString("\n        ")
                        }
            }

    val message = Text.get("my-orders-list:list") {
        it["orders"] = ordersDisplay
    }

    show(message) {
        button(Text["update-button"], "$WINDOW_MARKER:$clientNum")

        button(Text["cancel-button"], "remove-message")
    }
}