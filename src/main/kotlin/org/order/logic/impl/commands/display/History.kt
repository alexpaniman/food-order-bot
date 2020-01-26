package org.order.logic.impl.commands.display

import org.order.bot.send.button
import org.order.bot.send.switcherIn
import org.order.data.entities.OrderCancellation
import org.order.data.entities.Parent
import org.order.data.tables.OrdersCancellations
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.clients

private const val WINDOW_MARKER = "history-window"

val HISTORY_WINDOW_TRIGGER = CommandTrigger(Text["history-command"])
val HISTORY_WINDOW = Window(WINDOW_MARKER, HISTORY_WINDOW_TRIGGER) { user, (clientNumStr) ->
    // ----------- Clients List -----------
    val clients = user.clients()
    // ------------------------------------

    // ---------- Current Client ----------
    val clientNum = clientNumStr.toInt()
            .coerceIn(clients.indices)

    val client = clients[clientNum]
    // ------------------------------------

    val orders = client.orders
            .filter { !it.canceled }.map { order ->
        order.registered to Text.get("history:order") {
            it["ordered-by"] = order.madeBy.name!!
            it["menu:name"] = order.menu.name
            it["order-date"] = order.orderDate.toString()
        }
    }

    val payments = client.payments.map { payment ->
        payment.registered to Text.get("history:payment") {
            it["paid-by"] = payment.madeBy.name!!
            it["amount"] = payment.amount.toString()
            it["client:name"] = payment.client.user.name!!
        }
    }

    val cancellations = client.orders
            .filter { it.canceled }.map { order ->
        val cancellation = OrderCancellation
                .find { OrdersCancellations.order eq order.id }
                .first()

        cancellation.canceled to Text.get("history:order-cancellation") {
            it["canceled-by"] = cancellation.canceledBy.name!!
            it["order:menu:name"] = order.menu.name
            it["order:order-date"] = order.orderDate.toString()
        }
    }

    val allActions = (orders + payments + cancellations)
            .groupBy { it.first }
            .mapValues { (_, displays) ->
                displays.map { (_, display) -> display }
            }

    val actionsDisplay = buildString {
        for ((date, actions) in allActions) {
            appendln(Text["history-date"])
            for (action in actions)
                appendln(action)
        }
    }

    val message = Text.get("history") {
        it["actions"] = actionsDisplay
    }

    show(message) {
        switcherIn(clients, clientNum, { clients[it].user.name!! }, { "$WINDOW_MARKER:$it" })

        button(Text["update-button"], "$WINDOW_MARKER:$clientNum")

        button(Text["cancel-button"], "remove-message")
    }
}