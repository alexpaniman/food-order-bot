package org.order.logic.impl.commands.orders

import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.order.bot.send.button
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
import org.order.logic.impl.utils.availableList
import org.order.logic.impl.utils.clients
import org.order.logic.impl.utils.orZero
import org.order.logic.impl.utils.switcherIn

private const val WINDOW_MARKER = "order-window"

private val ORDER_WINDOW_TRIGGER = CommandTrigger(Text["order-command"]) and
        StateTrigger(COMMAND) and
        (RoleTrigger(Client) or RoleTrigger(Parent))

val ORDER_WINDOW = Window("order-window", ORDER_WINDOW_TRIGGER,
        args = listOf("0", "0", "0")) { user, (dayNumStr, menuNumStr, clientNumStr) ->

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

    val message = Text.get("suggest-menu") {
        it["menu"] = currentMenu.buildDescription()
    }

    val activeList = active.values.toList()
    val orderDayDisplay = day.dayOfWeek().getAsShortText(LOCALE)

    show(message) {
        if (user.hasLinked(Parent))
            button(client.user.name!!, "$WINDOW_MARKER:$dayNum:$menuNum:${(clientNum + 1).orZero(clients.indices)}")

        switcherIn(activeList, dayNum, { orderDayDisplay }, { "$WINDOW_MARKER:$it:$menuNum:$clientNum" })

        switcherIn(activeToday, menuNum, { it }, { "$WINDOW_MARKER:$dayNum:$it:$clientNum" })

        button(Text["make-order"], "make-order:${currentMenu.id.value}:$day:${client.id.value}")

        button(Text["cancel-button"], "remove-message")
    } // TODO Add information about order amount
}

val MAKE_ORDER = CallbackProcessor("make-order") { _, src, (menuIdStr, dayStr, clientIdStr) ->
    val menuId = menuIdStr.toInt()

    val menu = Menu.findById(menuId) ?: error("There's no menu with id: $menuId!")
    if (LocalDate.now() !in menu.availableList())
        src.edit(Text["menu-is-not-available-now"])
    else {
        val orderDate = LocalDate.parse(dayStr)

        val clientId = clientIdStr.toInt()
        val client = Client.findById(clientId) ?: error("There's no client with id: $clientId!")

        val now = DateTime.now()

        Order.new {
            this.registered = now // Date and time when the order created
            this.orderDate = orderDate
            this.menu = menu // The menu that was ordered
            this.client = client // A client that will get the order.
        }

        client.balance -= menu.cost

        // TODO add message about order
    }
}