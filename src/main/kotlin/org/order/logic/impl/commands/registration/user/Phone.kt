package org.order.logic.impl.commands.registration.user

import org.order.bot.send.Sender
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.logic.commands.readers.ContactReader
import org.order.logic.commands.readers.TextReader
import org.order.logic.corpus.Text

private const val PHONE_VALIDATOR = "^(\\+)?([- _(]?[0-9]){10,14}$"

fun Sender.readPhone(user: User) {
    user.send(Text["register-phone"])
    user.state = State.READ_PHONE
}

private fun Sender.processReceivedPhone(user: User, text: String) {
    if (text matches PHONE_VALIDATOR.toRegex()) user.run {
        phone = text
        state = State.NEW
    } else user.send(Text["wrong-phone"])
}

val READ_PHONE = TextReader(State.READ_PHONE) reader@ { user, text ->
    processReceivedPhone(user, text)
    return@reader false
}

val READ_CONTACT = ContactReader(State.READ_PHONE) reader@ { user, contact ->
    val phone = contact.phoneNumber?:return@reader false
    processReceivedPhone(user, phone)
    return@reader false
}