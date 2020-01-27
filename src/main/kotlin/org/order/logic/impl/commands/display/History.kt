package org.order.logic.impl.commands.display

import org.joda.time.DateTime
import org.order.bot.send.button
import org.order.bot.send.switcherIn
import org.order.data.entities.Client
import org.order.data.entities.OrderCancellation
import org.order.data.entities.Parent
import org.order.data.tables.OrdersCancellations
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.triggers.RoleTrigger
import org.order.logic.commands.triggers.and
import org.order.logic.commands.triggers.or
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.clients
import kotlin.math.absoluteValue

private const val WINDOW_MARKER = "history-window"

val HISTORY_WINDOW_TRIGGER =
        CommandTrigger(Text["history-command"]) and (RoleTrigger(Client) or RoleTrigger(Parent))

private data class HistoryAction(val time: DateTime, val balanceChange: Float, val description: String)

val HISTORY_WINDOW = Window(WINDOW_MARKER, HISTORY_WINDOW_TRIGGER,
        args = listOf("0")) { user, (clientNumStr) ->
    // ----------- Clients List -----------
    val clients = user.clients()
    // ------------------------------------

    // ---------- Current Client ----------
    val clientNum = clientNumStr.toInt()
            .coerceIn(clients.indices)

    val client = clients[clientNum]
    // ------------------------------------

    val orders = client.orders
            .map { order ->
                val description = Text.get("history:order") {
                    it["ordered-by"] = order.madeBy.name!!
                    it["menu:name"] = order.menu.name
                    it["order-date"] = order.orderDate.toString()
                }

                HistoryAction(order.registered, - order.menu.cost, description)
            }

    val payments = client.payments.map { payment ->
        val description = Text.get("history:payment") {
            it["paid-by"] = payment.madeBy.name!!
            it["amount"] = payment.amount.toString()
            it["client:name"] = payment.client.user.name!!
        }

        HistoryAction(payment.registered!!, payment.amount!!, description)
    }

    val cancellations = client.orders
            .filter { it.canceled }.map { order ->
                val cancellation = OrderCancellation
                        .find { OrdersCancellations.order eq order.id }
                        .first()

                val description = Text.get("history:order-cancellation") {
                    it["canceled-by"] = cancellation.canceledBy.name!!
                    it["order:menu:name"] = order.menu.name
                    it["order:order-date"] = order.orderDate.toString()
                }

                HistoryAction(cancellation.canceled, cancellation.order.menu.cost, description)
            }

    val allActions = (orders + payments + cancellations)
            .groupBy { it.time.toLocalDate() }
            .mapValues {(_, action) ->
                action.sortedBy { it.time }
            }

    var balance = 0f
    val actionsDisplay = allActions.toList()
            .joinToString("\n\n\n") { (date, actions) ->
                Text.get("history-date") {
                    it["date"] = date!!.toString("yyyy-MM-dd")
                } + "\n" + actions.joinToString("\n\n") { action ->
                    action.description + "\n" + Text.get("history-balance") {
                        balance += action.balanceChange
                        it["balance"] = balance.toString()
                        it["delta"] = (if (action.balanceChange >= 0) " + " else " - ") +
                                action.balanceChange.absoluteValue.toString()
                    }
                }
            }

    val message =
            if (allActions.isNotEmpty())
                Text.get("history") {
                    it["actions"] = actionsDisplay
                }
            else Text["history-empty"]

    show(message) {
        if (user.hasLinked(Parent))
            switcherIn(clients, clientNum, { clients[it].user.name!! }, { "$WINDOW_MARKER:$it" })

        button(Text["update-button"], "$WINDOW_MARKER:$clientNum")

        button(Text["cancel-button"], "remove-message")
    }
}