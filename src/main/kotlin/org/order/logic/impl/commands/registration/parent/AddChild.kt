package org.order.logic.impl.commands.registration.parent

import org.order.bot.send.Sender
import org.order.bot.send.button
import org.order.bot.send.reply
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.logic.commands.readers.TextReader
import org.order.logic.corpus.Text

fun Sender.suggestAddingChild(user: User) {
    user.send(Text["suggest-adding-another-child"]) {
        reply {
            button(Text[      "add-child"])
            button(Text["don't-add-child"])
        }
    }

    user.state = State.CONFIRM_CHILD_ADDING
}

val READ_ADD_USER_RESPONSE = TextReader(State.READ_STATE) reader@ { user, text ->
    when(text) {
        Text[      "add-child"] -> readChildName (user)
        Text["don't-add-child"] -> {
            user.state = State.VALIDATION
            return@reader false
        }
        else -> user.send(Text["wrong-child-confirmation"])
    }

    return@reader true
}