package org.order.logic.impl.commands.registration.parent

import org.order.bot.send.Sender
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.logic.commands.readers.TextReader
import org.order.logic.corpus.Text

private const val SINGLE_NAME = "[А-ЯЁ][а-яё]+"
private const val NAME_VALIDATOR = "$SINGLE_NAME(-$SINGLE_NAME)? $SINGLE_NAME\$"

fun Sender.readChildName(user: User) = user.run {
    send(Text["register-child-name"])
    state = State.READ_CHILD_GRADE
}

val READ_NAME  = TextReader(State.READ_CHILD_NAME) reader@ { user, text ->
    if (text matches NAME_VALIDATOR.toRegex()) user.run {
        name  = text
        state = State.COMMAND
    } else user.send(Text["wrong-name"])

    return@reader true
}