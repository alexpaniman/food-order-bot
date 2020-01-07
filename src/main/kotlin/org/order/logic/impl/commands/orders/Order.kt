package org.order.logic.impl.commands.orders

import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.order.bot.send.button
import org.order.bot.send.row
import org.order.data.entities.Client
import org.order.data.entities.Menu
import org.order.data.entities.Order
import org.order.data.entities.Parent
import org.order.data.entities.State.COMMAND
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.triggers.*
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.LOCALE

private val ORDER_WINDOW_TRIGGER = CommandTrigger(Text["order-command"]) and
        StateTrigger(COMMAND) and
        (RoleTrigger(Client) or RoleTrigger(Parent))

val ORDER_WINDOW = Window("order-window", ORDER_WINDOW_TRIGGER, listOf("0", "0", "0")) { user, (dayStr, numStr, childNumStr) ->
    val active = Menu.all()
            .filter { it.isAvailableNow() }
            .fold(mutableMapOf<LocalDate, MutableList<Menu>>()) { map, menu ->
                map.apply {
                    computeIfAbsent(menu.nextActiveDate()) {
                        mutableListOf()
                    } += menu
                }
            }
            .toList()
            .sortedBy { it.first }

    val day = dayStr.toInt()

    val numInt = numStr.toInt()

    val activeToday = active[day].second

    val num = if (numInt in activeToday.indices)
        numInt
    else 0
    val currentMenu = activeToday[num]

    val message = Text.get("suggest-menu") {
        it["menu"] = currentMenu.buildDescription()
    }

    val childNum = childNumStr.toInt()
    val children = if (user.hasLinked(Parent))
        user.linked(Parent).children
                .sortedBy { it.id } // Exact same order guaranteed
    else null
    val child = children?.get(childNum)

    val client = child?.user?.linked(Client) ?: user.linked(Client)

    show(message) {
        if (child != null) {
            val name = child.user.name!!

            when {
                children.size == 1 -> button(name)
                childNum == children.size - 1 -> button(name, "order-window:$day:$num:0")
                childNum < children.size - 1 -> button(name, "order-window:$day:$num:${childNum + 1}")
            }
        }

        row {
            if (day - 1 >= 0)
                button(Text["previous-day"], "order-window:${day - 1}:$num:$childNum")
            else
                button(Text["inactive"])

            button(active[day].first.dayOfWeek().getAsShortText(LOCALE))

            if (day + 1 < active.size)
                button(Text["next-day"], "order-window:${day + 1}:$num:$childNum")
            else
                button(Text["inactive"])
        }

        row {
            if (num - 1 >= 0)
                button(Text["previous-menu"], "order-window:$day:${num - 1}:$childNum")
            else
                button(Text["inactive"])

            button("$num")

            if (num + 1 < activeToday.size)
                button(Text["next-menu"], "order-window:$day${num + 1}:$childNum")
            else
                button(Text["inactive"])
        }

        // TODO deny ordering more then one menu

        button(Text["make-order"], "make-order:${currentMenu.id.value}:${client.id.value}")

        button(Text["cancel"], "remove-message")
    }
}

val MAKE_ORDER = CallbackProcessor("make-order") make_order@{ user, src, (menuIdStr, clientIdStr) ->
    val menuId = menuIdStr.toInt()

    val menu = Menu.findById(menuId)
    if (menu == null || !menu.isAvailableNow()) {
        src.edit(Text["menu-is-not-available-now"])
        return@make_order
    }

    val clientId = clientIdStr.toInt()
    val client = Client.findById(clientId) ?: error("There's no client with id: $clientId")

    Order.new {
        registered = DateTime.now() // Date and time when the order created
        orderDate = menu.nextActiveDate() // Date when the order will be completed
        this.madeBy = user // The user who ordered this order
        this.menu = menu // The menu that was ordered
        this.client = client // A client that will get the order.
    }

    user.linked(Client).balance -= menu.cost
}