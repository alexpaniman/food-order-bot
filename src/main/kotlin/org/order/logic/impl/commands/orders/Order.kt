package org.order.logic.impl.commands.orders

import org.jetbrains.exposed.sql.and
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.order.bot.send.button
import org.order.bot.send.inline
import org.order.bot.send.row
import org.order.bot.send.switcherIn
import org.order.data.entities.*
import org.order.data.entities.State.COMMAND
import org.order.data.tables.Orders
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.triggers.*
import org.order.logic.commands.window.Window
import org.order.logic.commands.window.WindowContext
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.MAX_DEBT
import org.order.logic.impl.utils.*

private const val WINDOW_MARKER = "order-window"

private val ORDER_WINDOW_TRIGGER = CommandTrigger(Text["order-command"]) and
        StateTrigger(COMMAND) and
        (RoleTrigger(Client) or RoleTrigger(Parent))

private fun WindowContext.suggestMakingOrder(user: User, dayNumStr: String, menuNumStr: String, clientNumStr: String) {
    val active = Menu.all()
            .map { menu ->
                menu.availableList().map { date ->
                    date to menu
                }
            }
            .flatten()
            .groupBy { (date, _) -> date }
            .mapValues { (_, values) ->
                values.map { (_, menu) ->
                    menu
                }.sortedBy { it.name }
            }
            .toSortedMap()

    val dayNum = dayNumStr.toInt().coerceIn(active.values.indices)
    val (day, activeToday) = active.entries.elementAt(dayNum)

    val menuNum = menuNumStr.toInt().coerceIn(activeToday.indices)
    val currentMenu = activeToday[menuNum]

    val clients = user.clients()
    val clientNum = clientNumStr.toInt().coerceIn(clients.indices)
    val client = clients[clientNum]

    if (client.balance <= - MAX_DEBT) {
        show(Text["not-enough-money-to-order"]) {
            if (user.hasLinked(Parent))
                button(client.user.name!!, "$WINDOW_MARKER:$dayNum:$menuNum:${(clientNum + 1).orZero(clients.indices)}")

            button(Text["cancel-button"], "remove-message")
        }

        return
    }

    val ordersToday = client.orders
            .filter { it.orderDate == day && it.menu == currentMenu && !it.canceled }
            .count()

    val message = Text.get("suggest-menu") {
        it["menu-description"] = currentMenu.buildDescription()
    } + "\n" + if (ordersToday > 0)
        Text.get("order-amount") {
            it["amount"] = ordersToday.toString()
        }
    else ""

    val activeList = active.values.toList()
    val orderDayDisplay = day.dayOfWeekAsShortText

    show(message) {
        if (user.hasLinked(Parent))
            button(client.user.name!!, "$WINDOW_MARKER:$dayNum:$menuNum:${(clientNum + 1).orZero(clients.indices)}")

        switcherIn(activeList, dayNum, { orderDayDisplay }, { "$WINDOW_MARKER:$it:$menuNum:$clientNum" })

        switcherIn(activeToday, menuNum, { num ->
            Text.get("menu-label") {
                it["name"] = activeToday[num].name
            }
        }, { "$WINDOW_MARKER:$dayNum:$it:$clientNum" })

        val makeOrderText = if (ordersToday > 0)
            Text["make-another-order"]
        else
            Text["make-order"]

        button(makeOrderText, "make-order:${currentMenu.id.value}:$day:${client.id.value}:$dayNum:$menuNum:$clientNum:false")

        button(Text["cancel-button"], "remove-message")
    }
}

val ORDER_WINDOW = Window("order-window", ORDER_WINDOW_TRIGGER,
        args = listOf("0", "0", "0")) { user, (dayNumStr, menuNumStr, clientNumStr) ->

    suggestMakingOrder(user, dayNumStr, menuNumStr, clientNumStr)
}

val MAKE_ORDER = CallbackProcessor("make-order") make_order@ { user, src, (menuIdStr, dayStr, clientIdStr, dayNumStr, menuNumStr, clientNumStr, forcedStr) ->
    val orderDate = LocalDate.parse(dayStr)
    val isForced = forcedStr.toBoolean()

    val menuId = menuIdStr.toInt()

    val menu = Menu.findById(menuId) ?: error("There's no menu with id: $menuId!")
    if (orderDate !in menu.availableList())
        src.edit(Text["menu-is-not-available-now"])
    else {
        val clientId = clientIdStr.toInt()
        val client = Client.findById(clientId) ?: error("There's no client with id: $clientId!")

        val now = DateTime.now()

        if (!isForced) {
            val orders = Order
                    .find {
                        Orders.client eq client.id and
                                (Orders.orderDate eq dayStr)
                    }

            if (!orders.empty()) {
                src.edit(Text["do-you-want-to-order-yet-another-time"]) {
                    row {
                        button(Text["confirm-another-order"], "make-order:$menuIdStr:$dayStr:$clientIdStr:$dayNumStr:$menuNumStr:$clientNumStr:true")
                        button(Text["dismiss-another-order"], "$WINDOW_MARKER:$dayNumStr:$menuNumStr:$clientNumStr")
                    }

                    button(Text["cancel-button"], "remove-message")
                }
                return@make_order
            }
        }

        Order.new {
            this.registered = now // Date and time when the order created
            this.madeBy = user
            this.orderDate = orderDate
            this.menu = menu // The menu that was ordered
            this.client = client // A client that will get the order.
        }

        client.balance -= menu.cost

        WindowContext(this, src, user)
                .suggestMakingOrder(user, dayNumStr, menuNumStr, clientNumStr)
    }
}