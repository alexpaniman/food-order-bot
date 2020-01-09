package org.order.logic.impl.commands.display

import org.order.data.entities.State.COMMAND
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.commands.triggers.and
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text

private const val WINDOW_MARKER = "my-orders-list-window"

private val MY_ORDERS_LIST_WINDOW_TRIGGER = CommandTrigger(Text["my-orders-window"]) and StateTrigger(COMMAND)
val MY_ORDERS_LIST_WINDOW = Window(
        WINDOW_MARKER,
        MY_ORDERS_LIST_WINDOW_TRIGGER,
        listOf()) { user, (dayNumStr) ->
    // TODO

}