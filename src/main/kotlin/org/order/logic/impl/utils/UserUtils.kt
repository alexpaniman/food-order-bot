package org.order.logic.impl.utils

import org.order.bot.send.button
import org.order.bot.send.reply
import org.order.bot.send.row
import org.order.data.entities.*
import org.order.logic.corpus.Text
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup

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

val User.grade get() = linkedOrNull(Student)?.grade?.name ?: Text["empty-grade"]

val GRADE_COMPARATOR = compareBy<String> {
    val split = it.split("-")
    val gradeValue = split[0].toIntOrNull() ?: 12 // <=== Teachers are treated as people from "twelfth" grade
    val letterValue = split.getOrNull(1)?.toIntOrNull() ?: 0

    1e6 * gradeValue + letterValue
}

private fun ReplyKeyboardMarkup.mainKeyboard(user: User) {
    val isClient = user.hasLinked(Client)
    val isParent = user.hasLinked(Parent)

    val isAdmin = user.hasLinked(Admin)
    val isProducer = user.hasLinked(Producer)

    val isAdminOrProducer = isAdmin || isProducer
    val isClientOrParent = isClient || isParent

    if (isClientOrParent)
        row {
            button(Text["order-command"])
            button(Text["order-cancellation-command"])
        }

    row {
        if (isClientOrParent)
            button(Text["my-orders-command"])
        button(Text["orders-list-command"])
    }

    if (isClientOrParent)
        row {
            button(Text["pay-command"])
            button(Text["payments-list-command"])
        }

    row {
        if (isClientOrParent)
            button(Text["history-command"])
        button(Text["help-command"])
    }

    if (isAdminOrProducer) row {
        button(Text["replenish-account-command"])
        button(Text["money-total-command"])
        if (isAdmin)
            button(Text["mailing-command"])
    }

    if (isAdminOrProducer) row {
        button(Text["money-pdf-total-command"])
        button(Text["polls-pdf-total-command"])
    }

}

fun SendMessage.appendMainKeyboard(user: User) = reply {
    mainKeyboard(user)
}

fun SendDocument.appendMainKeyboard(user: User) = reply {
    mainKeyboard(user)
}

val User.isRegistered
    get() = name != null && phone != null && state == State.COMMAND