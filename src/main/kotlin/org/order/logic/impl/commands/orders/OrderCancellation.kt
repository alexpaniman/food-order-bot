package org.order.logic.impl.commands.orders

import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.order.bot.send.button
import org.order.bot.send.deactivatableButton
import org.order.bot.send.row
import org.order.data.entities.Client
import org.order.data.entities.Order
import org.order.data.entities.OrderCancellation
import org.order.data.entities.Parent
import org.order.data.entities.State.COMMAND
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.triggers.*
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.LOCALE
import org.order.logic.impl.utils.clients
import org.order.logic.impl.utils.orZero

private const val WINDOW_MARKER = "order-cancellation-window"

private val ORDER_CANCELLATION_WINDOW_TRIGGER = CommandTrigger(Text["order-cancellation-command"]) and
        StateTrigger(COMMAND) and (RoleTrigger(Client) or RoleTrigger(Parent))

val ORDER_CANCELLATION_WINDOW = Window(WINDOW_MARKER, ORDER_CANCELLATION_WINDOW_TRIGGER,
        args = listOf("0")) { user, (clientNumStr) ->

    // ----------- Clients List -----------
    val clients = user.clients()
    // ------------------------------------

    // ---------- Current Client ----------
    val clientNum = clientNumStr.toInt()
            .coerceIn(clients.indices)

    val client = clients[clientNum]
    // ------------------------------------

    // -------- List of Done Orders --------
    val now = LocalDate.now()

    val ordersAfterNow = client.orders
            .filter { !it.canceled }
            .filter { it.orderDate.isAfter(now) }

    val ordersWithDate = (1..5)
            .fold(mutableMapOf<LocalDate, MutableList<Order>>()) { map, dayOfWeek ->
                val day = now.plusDays(dayOfWeek - now.dayOfWeek)
                map.apply {
                    computeIfAbsent(day) {
                        mutableListOf()
                    } += ordersAfterNow.filter { order -> order.orderDate == day }
                }
            }
    // -------------------------------------

    show(Text["suggest-order-cancellation"]) {
        // ------------ Button with Client name for Parents ------------
        if (user.hasLinked(Parent))
            button(
                    client.user.name!!,
                    "$WINDOW_MARKER:${(clientNum + 1).orZero(clients.indices)}"
            )
        // -------------------------------------------------------------

        // ----- List with Days of Week When the Order Was Ordered -----
        row {
            for ((date, orders) in ordersWithDate) {
                val dateDisplay = date.dayOfWeek().getAsShortText(LOCALE)
                val joinedOrders = orders.joinToString(":")

                deactivatableButton(dateDisplay, "cancel-orders:$date:$joinedOrders") {
                    orders.isNotEmpty()
                }
            }
        }
        // -------------------------------------------------------------

        // ------------------ Button to Update Window ------------------
        button(Text["update-button"], "$WINDOW_MARKER:$clientNum")
        // -------------------------------------------------------------

        // ------------------ Button to Remove Window ------------------
        button(Text["cancel-button"], "remove-message")
        // -------------------------------------------------------------
    }
}

val CANCEL_ORDER = CallbackProcessor("cancel-orders") { user, src, args ->
    val date = LocalDate.parse(args.first())
    val orders = args.drop(1)

    // ------- Find Order to Cancel -------
    var canceledCount = 0
    for (orderIdStr in orders) {
        val order = Order.findById(orderIdStr.toInt()) ?: continue

        // ----------- Cancel Order -----------
        order.client.balance += order.menu.cost
        OrderCancellation.new {
            this.canceledBy = user
            this.canceled = DateTime.now()
            this.order = order
        }
        order.canceled = true
        // ------------------------------------

        canceledCount ++
    }

    src.edit(Text.get("successful-order-cancellation") {
        it["date"] = date.dayOfWeek().getAsShortText(LOCALE)
        it["amount"] = canceledCount.toString()
    })
    // ------------------------------------
}