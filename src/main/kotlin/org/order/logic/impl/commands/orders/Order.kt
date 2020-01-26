package org.order.logic.impl.commands.orders

import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.order.bot.send.button
import org.order.bot.send.switcherIn
import org.order.data.entities.*
import org.order.data.entities.State.COMMAND
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.triggers.*
import org.order.logic.commands.window.Window
import org.order.logic.commands.window.WindowContext
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.LOCALE
import org.order.logic.impl.utils.*

private const val WINDOW_MARKER = "order-window"

private val ORDER_WINDOW_TRIGGER = CommandTrigger(Text["order-command"]) and
        StateTrigger(COMMAND) and
        (RoleTrigger(Client) or RoleTrigger(Parent))

private fun WindowContext.suggestMakingOrder (user: User, dayNumStr: String, menuNumStr: String, clientNumStr: String) {
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
                }
            }
            .toSortedMap(compareBy { it })

    val dayNum = dayNumStr.toInt().coerceIn(active.values.indices)
    val (day, activeToday) = active.entries.elementAt(dayNum)

    val menuNum = menuNumStr.toInt().coerceIn(activeToday.indices)
    val currentMenu = activeToday[menuNum]

    val clients = user.clients()
    val clientNum = clientNumStr.toInt().coerceIn(clients.indices)
    val client = clients[clientNum]

    val now = LocalDate.now()
    val ordersToday = client.orders
            .filter { it.orderDate == day }
            .count()

    val message = Text.get("suggest-menu") {
        it["menu-description"] = currentMenu.buildDescription()
    } + "\n" + if (ordersToday > 0)
        Text.get("order-amount") {
            it["amount"] = ordersToday.toString()
        }
    else ""

    val activeList = active.values.toList()
    val orderDayDisplay = day.dayOfWeek().getAsShortText(LOCALE)

    show(message) {
        if (user.hasLinked(Parent))
            button(client.user.name!!, "$WINDOW_MARKER:$dayNum:$menuNum:${(clientNum + 1).orZero(clients.indices)}")

        switcherIn(activeList, dayNum, { orderDayDisplay }, { "$WINDOW_MARKER:$it:$menuNum:$clientNum" })

        switcherIn(activeToday, menuNum, { it }, { "$WINDOW_MARKER:$dayNum:$it:$clientNum" })

        val makeOrderText = if (ordersToday > 0)
            Text["make-another-order"]
        else
            Text["make-order"]

        button(makeOrderText, "make-order:${currentMenu.id.value}:$day:${client.id.value}:$dayNum:$menuNum:$clientNum")

        button(Text["cancel-button"], "remove-message")
    }
}

val ORDER_WINDOW = Window("order-window", ORDER_WINDOW_TRIGGER,
        args = listOf("0", "0", "0")) { user, (dayNumStr, menuNumStr, clientNumStr) ->

    suggestMakingOrder(user, dayNumStr, menuNumStr, clientNumStr)
}

val MAKE_ORDER = CallbackProcessor("make-order") { user, src, (menuIdStr, dayStr, clientIdStr, dayNumStr, menuNumStr, clientNumStr) ->
    val orderDate = LocalDate.parse(dayStr)

    val menuId = menuIdStr.toInt()

    val menu = Menu.findById(menuId) ?: error("There's no menu with id: $menuId!")
    if (orderDate !in menu.availableList())
        src.edit(Text["menu-is-not-available-now"])
    else {
        val clientId = clientIdStr.toInt()
        val client = Client.findById(clientId) ?: error("There's no client with id: $clientId!")

        val now = DateTime.now()

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