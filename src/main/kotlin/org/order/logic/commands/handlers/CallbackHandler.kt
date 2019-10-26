package org.order.logic.commands.handlers

import org.order.bot.send.Sender
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Update

class CallbackHandler(private val marker: String, private val body: Sender.(User, List<String>) -> Unit): Command {
    override fun Sender.process(user: User, update: Update): Boolean {
        val data = update.callbackQuery?.data ?: return false
        if (data.substringBefore(':') != marker)
            return false
        val args = data
                .substringAfter(':')
                .split(":")
        body(user, args)
        return true
    }
}