package org.order.logic.impl.utils

import org.order.bot.send.button
import org.order.bot.send.row
import org.order.data.entities.Client
import org.order.data.entities.User
import org.order.logic.corpus.Text
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup

fun <T : Any> InlineKeyboardMarkup.switcherIn(list: List<T>, index: Int, text: (Int) -> Any, callback: (Int) -> String) =
        row {
            if (index - 1 >= 0)
                button(Text["previous-button"], callback(index - 1))
            else
                button(Text["inactive"])

            button(text(index).toString())

            if (index + 1 <= list.lastIndex)
                button(Text["next-button"], callback(index + 1))
            else
                button(Text["inactive"])
        }

fun User.clients(): List<Client> {
    val children = linkedOrNull(org.order.data.entities.Parent)
            ?.children
            ?.map { it.user.linked(Client) } ?: listOf()

    val selfClient = linkedOrNull(Client)

    val unsortedClients = children +
            if (selfClient != null)
                listOf(selfClient)
            else listOf()

    // Sorting to guarantee same clients order
    return unsortedClients.sortedBy { it.id }
}