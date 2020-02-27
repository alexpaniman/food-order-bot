package org.order.logic.impl.utils

import org.order.bot.send.button
import org.order.bot.send.reply
import org.order.bot.send.row
import org.order.data.entities.*
import org.order.logic.corpus.Text
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

fun User.clients(): List<Client> {
    val children = linkedOrNull(Parent)
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

fun SendMessage.appendMainKeyboard(user: User) = reply {
    val isClient = user.hasLinked(Client)
    val isParent = user.hasLinked(Parent)

    val isAdmin = user.hasLinked(Admin)
    val isProducer = user.hasLinked(Producer)

    val canReplenishAccount = isAdmin || isProducer

    val canOrderAndPay = isClient || isParent

    if (canOrderAndPay)
        row {
            button(Text["order-command"])
            button(Text["order-cancellation-command"])
        }
    row {
        if (canOrderAndPay)
            button(Text["my-orders-command"])
        button(Text["orders-list-command"])
    }

    if (canOrderAndPay)
        row {
            button(Text["pay-command"])
            button(Text["payments-list-command"])
        }
    row {
        if (canOrderAndPay)
            button(Text["history-command"])
        if (canReplenishAccount)
            button(Text["replenish-account-command"])
        if (canReplenishAccount)
            button(Text["money-total-command"])
        button(Text["help-command"])
    }
}