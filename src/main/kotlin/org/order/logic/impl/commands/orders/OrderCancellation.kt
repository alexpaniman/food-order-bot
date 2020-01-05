package org.order.logic.impl.commands.orders

import org.joda.time.LocalDate
import org.order.bot.send.button
import org.order.bot.send.row
import org.order.data.entities.Client
import org.order.data.entities.Order
import org.order.data.entities.Parent
import org.order.data.entities.State.COMMAND
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.triggers.RoleTrigger
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.commands.triggers.and
import org.order.logic.commands.triggers.or
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.LOCALE

private val ORDER_CANCELLATION_WINDOW_TRIGGER = StateTrigger(COMMAND) and (RoleTrigger(Client) or RoleTrigger(Parent))
val ORDER_CANCELLATION_WINDOW = Window(
        "order-cancellation-window",
        ORDER_CANCELLATION_WINDOW_TRIGGER,
        listOf("0")) { user, (childNumStr) ->

    val childNum = childNumStr.toInt()
    val children = if (user.hasLinked(Parent))
        user.linked(Parent)
            .children
            .sortedBy { it.id.value }
    else null
    val child = children?.get(childNum)

    val client = child?.user?.linked(Client) ?: user.linked(Client)

    show(Text["suggest-order-cancellation"]) {
        if (child != null) {
            when {
                children.size == 1 -> button(child.user.name!!)
                childNum == children.size - 1 -> button(child.user.name!!, "order-cancellation-window:0")
                childNum <  children.size - 1 -> button(child.user.name!!, "order-cancellation-window:${childNum + 1}")
            }
        }

        row {
            val now = LocalDate.now()

            val orders = client.orders
                    .filter { it.orderDate.isAfter(now) }
                    .sortedBy { it.orderDate.dayOfWeek }

            val base = LocalDate.now().minusDays(now.dayOfWeek)

            for (dayOfWeek in 1..5) {
                val order = orders.singleOrNull { it.orderDate.dayOfWeek == dayOfWeek }
                if (order != null) {
                    val day = base.plusDays(dayOfWeek)
                    val dayOfWeekName = day.dayOfWeek().getAsShortText(LOCALE)

                    button(dayOfWeekName, "cancel-order:${order.id.value}")
                } else
                    button(Text["inactive"])
            }
        }

        button(Text["cancel"], "remove-window")
    }
}

val CANCEL_ORDER = CallbackProcessor("cancel-order") cancel_order@ { _, src, (orderIdStr) ->
    val orderId = orderIdStr.toInt()
    val order = Order.findById(orderId)
    if (order == null) {
        src.edit(Text["this-order-was-already-cancelled"])
        return@cancel_order
    }

    order.delete()
}