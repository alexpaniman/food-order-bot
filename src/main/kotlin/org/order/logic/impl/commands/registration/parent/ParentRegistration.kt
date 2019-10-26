package org.order.logic.impl.commands.registration.parent

import org.order.bot.send.Sender
import org.order.data.entities.Parent
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.data.tables.Parents
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Update

val REGISTER_NEW = object : Command {
    override fun Sender.process(user: User, update: Update): Boolean {
        if (user.state != State.COMMAND || Parent.find { Parents.user eq user.id }.empty())
            return false
        // TODO
        return true
    }
}