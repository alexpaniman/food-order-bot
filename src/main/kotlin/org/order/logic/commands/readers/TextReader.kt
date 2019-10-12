package org.order.logic.commands.readers

import org.order.bot.send.Sender
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Update

class TextReader(private val state: State, private val body: Sender.(User, String) -> Unit): Command {
    override fun matches(user: User, update: Update) = update.message?.text != null && user.state == state
    override fun process(sender: Sender, user: User, update: Update) = sender.body(user, update.message!!.text!!)
}