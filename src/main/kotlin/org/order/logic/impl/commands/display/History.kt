package org.order.logic.impl.commands.display

import org.order.bot.send.button
import org.order.data.entities.Parent
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text

private const val WINDOW_MARKER = "history-window"

val HISTORY_WINDOW_TRIGGER = CommandTrigger(Text["history-command"])
val HISTORY_WINDOW = Window(WINDOW_MARKER, HISTORY_WINDOW_TRIGGER) { user, _ ->

    val parentMarker = if (user.hasLinked(Parent)) ":parent" else ""

    val orders = user.orders.map { order ->
        order.registered to Text.get("history$parentMarker:order") {
            it["menu:name"] = order.menu.name
            it["order-date"] = order.orderDate.toString()
            it["client:name"] = order.client.user.name!!
        }
    }

    val payments = user.payments.map { payment ->
        payment.registered to Text.get("history$parentMarker:payment") {
            it["amount"] = payment.amount.toString()
            it["client:name"] = payment.client.user.name!!
        }
    }

    val cancellations = user.cancellations.map { cancellation ->
        cancellation.canceled to Text.get("history$parentMarker:order-cancellation") {
            it["order:menu:name"] = cancellation.order.menu.name
            it["order:order-date"] = cancellation.order.orderDate.toString()
            it["order:client:name"] = cancellation.order.client.user.name!!
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
        button(Text["update-button"], "orders-list-window")

        button(Text["cancel-button"], "remove-message")
    }
}