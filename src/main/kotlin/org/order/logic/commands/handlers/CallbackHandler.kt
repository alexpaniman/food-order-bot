package org.order.logic.commands.handlers

import org.order.bot.send.Sender
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Update

class CallbackHandler(private val marker: String, private val body: Sender.(User, List<String>) -> Unit): Command {
    override fun matches(user: User, update: Update) = update.callbackQuery?.data
            ?.substringBefore(':') == marker

    override fun process(sender: Sender, user: User, update: Update) = sender.body(
            user,
            update.callbackQuery!!.data!!
                    .substringAfter(':')
                    .split(":")
    )
}