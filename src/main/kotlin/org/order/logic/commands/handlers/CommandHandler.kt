package org.order.logic.commands.handlers

import org.order.bot.send.Sender
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Update

class CommandHandler(private vararg val names: String, private val body: Sender.(User) -> Unit): Command {
    override fun Sender.process(user: User, update: Update): Boolean {
        if (update.message?.text !in names)
            return false
        body(user)
        return true
    }
}