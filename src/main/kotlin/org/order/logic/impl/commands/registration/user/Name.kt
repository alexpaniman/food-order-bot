package org.order.logic.impl.commands.registration.user

import org.order.bot.send.Sender
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.logic.commands.readers.TextReader
import org.order.logic.corpus.Text

private const val SINGLE_NAME = "[А-ЯЁ][а-яё]+"
private const val NAME_VALIDATOR = "$SINGLE_NAME(-$SINGLE_NAME)? $SINGLE_NAME\$"

fun Sender.readName (user: User) = user.run {
    send(Text["register-name"])
    state = State.READ_NAME
}

val READ_NAME = TextReader (State.READ_NAME) reader@ { user, text ->
    if (text matches NAME_VALIDATOR.toRegex()) user.run {
        name  = text
        state = State.NEW
    } else user.send(Text["wrong-name"])

    return@reader false
}