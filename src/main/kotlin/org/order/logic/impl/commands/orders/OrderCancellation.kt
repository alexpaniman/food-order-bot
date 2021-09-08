package org.order.logic.impl.commands.orders

import org.joda.time.DateTime
import org.joda.time.DateTimeConstants.SATURDAY
import org.joda.time.DateTimeConstants.SUNDAY
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.order.bot.send.button
import org.order.bot.send.deactivatableButton
import org.order.bot.send.row
import org.order.bot.send.switcherIn
import org.order.data.entities.*
import org.order.data.entities.State.COMMAND
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.triggers.*
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.LAST_ORDER_TIME
import org.order.logic.impl.commands.modules.clearUsersSearch
import org.order.logic.impl.commands.modules.replaceWithUsersSearch
import org.order.logic.impl.commands.modules.searchUsers
import org.order.logic.impl.utils.clients
import org.order.logic.impl.utils.dayOfWeekAsLongText
import org.order.logic.impl.utils.dayOfWeekAsShortText
import org.order.logic.impl.utils.orZero

private const val WINDOW_MARKER = "order-cancellation-window"

private val ORDER_CANCELLATION_WINDOW_TRIGGER = CommandTrigger(Text["order-cancellation-command"]) and
        StateTrigger(COMMAND) and (RoleTrigger(Client) or RoleTrigger(Parent))

private const val ABORT_ORDER_CANCELLATION_WINDOW_CALLBACK = "cancellation-window-remove-message"

val ORDER_CANCELLATION_WINDOW = Window(WINDOW_MARKER, ORDER_CANCELLATION_WINDOW_TRIGGER,
        args = listOf("0", "", "0")) { user, (clientNumStr, /* Last two are only for admins */ searchResults, minusWeeksStr) ->
    clearUsersSearch(user) // Clear user search if its entry exists

    // Admins and parents are allowed to perform user search
    val userHasAdministratorRights = user.hasLinked(Admin) || user.hasLinked(Producer)

    // Means that results are search dependent
    val searchMode = userHasAdministratorRights && searchResults != ""

    // ----------- Clients List -----------
    val clients = if (searchMode)
        searchResults.split(",")
                .map { it.toInt() }
                .map { Client.findById(it) ?: error("No such client id: $it!") }
    else
        user.clients()
    // ------------------------------------

    // ---------- Current Client ----------
    val clientNum = clientNumStr.toInt()
            .coerceIn(clients.indices)

    val client = clients[clientNum]
    // ------------------------------------

    // -------- List of Done Orders --------
    // Week offset is needed in order to provide administrators the ability to remove old orders
    // For regular clients this should always be 0
    val minusWeek = minusWeeksStr.toInt()
    val nowDate = LocalDate.now()
            .plusWeeks(minusWeek)

    val nowTime = LocalTime.now()

    val weekStart = when (nowDate.dayOfWeek) {
        SUNDAY, SATURDAY -> nowDate.plusWeeks(1)
        else -> nowDate
    }

    val ordersAfterNow = client.orders
            .filter { !it.canceled }
            .filter { !it.orderDate.isBefore(nowDate) }
            .filter {
                searchMode || // <== People who can search can delete order in any time
                        it.orderDate != nowDate || LAST_ORDER_TIME.isAfter(nowTime)
            }

    val ordersWithDate = (1..5)
            .fold(mutableMapOf<LocalDate, MutableList<Order>>()) { map, dayOfWeek ->
                val day = weekStart.plusDays(dayOfWeek - weekStart.dayOfWeek)
                map.apply {
                    computeIfAbsent(day) {
                        mutableListOf()
                    } += ordersAfterNow.filter { order -> order.orderDate == day }
                }
            }
    // -------------------------------------

    show(Text["suggest-order-cancellation"]) {
        if (user.hasLinked(Parent) || userHasAdministratorRights)
            row {
                // ------------ Button with Client name for Parents ------------
                val nextClient = (clientNum + 1).orZero(clients.indices) // <== Next client id
                button(client.user.name!!, "$WINDOW_MARKER:$nextClient:$searchResults:$minusWeek")
                // -------------------------------------------------------------

                // -------------------------- Search ---------------------------
                if (userHasAdministratorRights) {
                    val searcherCallback = replaceWithUsersSearch(user, "$WINDOW_MARKER:0:{}:$minusWeek")
                    button(Text["administration:search-button"], searcherCallback)

                    val userIsParentOrClient = user.hasLinked(Parent) || user.hasLinked(Client)
                    if (searchResults != "" && userIsParentOrClient) // Empty string here means that no search is performed
                    // Send empty string as search string back to this window
                        button(Text["administration:cancel-search"], "$WINDOW_MARKER:$clientNum::0")
                }
                // -------------------------------------------------------------
            }

        if (searchMode)
            switcherIn(Int.MIN_VALUE..0, minusWeek, {
                weekStart.toString("yyyy-MM") + " [" + weekStart.weekOfWeekyear + "]"
            }) {
                "$WINDOW_MARKER:$clientNum:$searchResults:$it"
            }


        // ----- List with Days of Week When the Order Was Ordered -----
        row {
            for ((date, orders) in ordersWithDate) {
                val dateDisplay = if (searchMode)
                    "${date.dayOfMonth}"
                else date.dayOfWeekAsShortText

                val joinedOrders = orders.joinToString(":") { it.id.value.toString() }

                val restoreCallback = "$clientNum:$searchResults:$minusWeek"
                deactivatableButton(dateDisplay,
                        "cancel-orders-confirmation:$restoreCallback:$date:$joinedOrders") {
                    orders.isNotEmpty()
                }
            }
        }
        // -------------------------------------------------------------

        // ------------------ Button to Update Window ------------------
        button(Text["update-button"], "$WINDOW_MARKER:$clientNum:$searchResults:$minusWeek")
        // -------------------------------------------------------------

        // ------------------ Button to Remove Window ------------------
        button(Text["cancel-button"], ABORT_ORDER_CANCELLATION_WINDOW_CALLBACK)
        // -------------------------------------------------------------
    }
}

val ABORT_ORDER_CANCELLATION_WINDOW = CallbackProcessor(ABORT_ORDER_CANCELLATION_WINDOW_CALLBACK) {
        user, src, _ ->
    clearUsersSearch(user)
    src.delete()
}

private val ORDER_CANCELLATION_ENTRY_FOR_ADMINISTRATORS_TRIGGER =
        CommandTrigger(Text["order-cancellation-command"]) and
                StateTrigger(COMMAND) and (RoleTrigger(Admin) or RoleTrigger(Producer))
val ORDER_CANCELLATION_ENTRY_FOR_ADMINISTRATORS = TriggerCommand(ORDER_CANCELLATION_ENTRY_FOR_ADMINISTRATORS_TRIGGER) { user, _ ->
    searchUsers(user, "$WINDOW_MARKER:0:{}:0")
}

val CANCEL_ORDERS_CONFIRMATION = CallbackProcessor("cancel-orders-confirmation") { user, src, confirmationArgs ->
    val args = confirmationArgs.drop(3)

    val orders = args.drop(1)
            .map { it.toInt() }
            .map { Order.findById(it) ?: error("No such order!") }

    val text = Text.get("cancel-orders:confirmation") { map ->
        map["n"] = "${args.size - 1}"
        map["items"] = buildString {
            for ((index, order) in orders.withIndex())
                appendln(Text.get("cancel-orders:confirmation:item") {
                    it["number"] = "${index + 1}"
                    it["name"] = order.menu.name
                    it["date"] = order.registered.toString("yyyy-MM-dd HH:MM:ss")
                })
        }
    }

    val cancellationCallback = "cancel-orders:${args.joinToString(":")}"
    val restoreCallback = "order-cancellation-window:${confirmationArgs.take(3).joinToString(":")}"
    src.edit(text) {
        if (user.hasLinked(Parent) || user.hasLinked(Admin) || user.hasLinked(Producer))
            button(orders.first().client.user.name!!)

        row {
            button(Text["cancel-orders:accept"], cancellationCallback)
            button(Text["cancel-orders:reject"], restoreCallback)
        }

        if (orders.size > 1)
            for ((index, order) in orders.withIndex())
                button(Text.get("cancel-orders:only-this") {
                    it["num"] = "${index + 1}"
                }, "cancel-orders:${args.first()}:${order.id}")
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

        canceledCount++
    }

    src.edit(Text.get("successful-order-cancellation") {
        it["date"] = date.dayOfWeekAsLongText.toLowerCase()
        it["amount"] = canceledCount.toString()
    })
    // ------------------------------------
}