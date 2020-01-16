package org.order.logic.impl.commands.display

import org.order.bot.send.button
import org.order.bot.send.deactivatableKeyButton
import org.order.bot.send.row
import org.order.data.entities.Client
import org.order.data.entities.Parent
import org.order.data.entities.Student
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.triggers.RoleTrigger
import org.order.logic.commands.triggers.and
import org.order.logic.commands.triggers.or
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text

private const val WINDOW_MARKER = "payments-list-window"

private val PAYMENTS_LIST_WINDOW_TRIGGER =
        CommandTrigger(Text["payments-list-command"]) and (RoleTrigger(Parent) or RoleTrigger(Student))

val PAYMENTS_LIST_WINDOW = Window(WINDOW_MARKER, PAYMENTS_LIST_WINDOW_TRIGGER,
        args = listOf("-1", "0")) { user, (monthNumStr, clientNumStr) ->

    // ----------- Clients List -----------
    val children = user.linkedOrNull(Parent)
            ?.children
            ?.map { it.user.linked(Client) } ?: listOf()

    val selfClient = user.linkedOrNull(Client)

    val unsortedClients = children +
            if (selfClient != null)
                listOf(selfClient)
            else listOf()

    // Sorting to guarantee same clients order
    val clients = unsortedClients
            .sortedBy { it.id }
    // ------------------------------------

    // ---------- Current Client ----------
    val clientNum = clientNumStr.toInt()
            .coerceIn(clients.indices)

    val client = clients[clientNum]
    // ------------------------------------

    // ------------ All Changes ------------
    val payments = client.payments
            .groupBy { it.registered!! }
            .mapValues { (_, value) ->
                value.map {
                    -it.amount!!
                }
            }

    val orders = client.orders
            .groupBy { it.registered }
            .mapValues { (_, value) ->
                value.map {
                    it.menu.cost
                }
            }

    val changes = (payments + orders)
            .toSortedMap(compareBy { it })
            .toList()
    // -------------------------------------

    // --- Changes only in Chosen Month ---
    if (changes.isEmpty()) {
        show(Text["there-were-no-payments-made-yet"])
        return@Window
    }

    val monthNum = monthNumStr.toInt()
            .coerceIn(changes.indices)

    val (day, changesToDisplay) = changes[monthNum]
    // ------------------------------------

    val displayDay = day.toString(Text["payments-date-format"])
    val paymentsDisplay = buildString {
        for (amount in changesToDisplay)
            appendln(Text.get("payment-display") {
                it["amount"] = amount.toString()
                it["date"] = displayDay
            })
    }

    val message = Text.get("payments-list") {
        it["payments"] = paymentsDisplay
    }

    show(message) {
        // ------------ Button with Client name for Parents ------------
        if (children.isNotEmpty())
            button(
                    client.user.name!!,
                    "$WINDOW_MARKER:$monthNum:${(clientNum + 1).coerceIn(clients.indices)}"
            )
        // -------------------------------------------------------------

        // ------------------ Buttons to Switch Month ------------------
        row {
            deactivatableKeyButton(
                    "previous-button",
                    "$WINDOW_MARKER:${monthNum - 1}:$clientNum") {
                monthNum - 1 >= 0
            }

            deactivatableKeyButton(
                    "next-button",
                    "$WINDOW_MARKER:${monthNum + 1}:$clientNum") {
                monthNum + 1 < changes.size
            }
        }
        // -------------------------------------------------------------

        // ------------------ Button to Update Window ------------------
        button(Text["update-button"], "$WINDOW_MARKER:$monthNum$clientNum")
        // -------------------------------------------------------------

        // ------------------ Button to Remove Window ------------------
        button(Text["cancel-button"], "remove-message")
        // -------------------------------------------------------------
    }
}