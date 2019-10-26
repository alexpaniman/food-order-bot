package org.order.logic.impl.commands.registration.state

import org.order.bot.send.Sender
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.order.logic.commands.CommandScope
import org.telegram.telegrambots.meta.api.objects.Update

val REGISTER_NEW = object : Command {
    override fun Sender.process(user: User, update: Update) =
            if (user.state == State.NEW) { readState(user); true }
            else false
}

val STATE_REGISTRATION = CommandScope(READ_STATE, REGISTER_NEW) { !it.valid }