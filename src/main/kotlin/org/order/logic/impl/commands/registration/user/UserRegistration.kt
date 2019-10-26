package org.order.logic.impl.commands.registration.user

import org.order.bot.send.Sender
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.order.logic.commands.CommandScope
import org.order.logic.corpus.Text
import org.telegram.telegrambots.meta.api.objects.Update

val REGISTER_NEW = object: Command {
    override fun Sender.process(user: User, update: Update): Boolean {
        if (user.state != State.NEW)
            return false
        if (user.name == null && user.phone == null)
            user.send(Text["greeting"])

        when {
            user.name  == null -> readName (user)
            user.phone == null -> readPhone(user)
            else -> return false
        }
        return true
    }
}

val USER_REGISTRATION = CommandScope(READ_NAME, READ_PHONE, READ_CONTACT, REGISTER_NEW) { !it.valid }