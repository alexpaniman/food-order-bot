package org.order.logic.commands.readers

import org.order.bot.send.Sender
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Update

class TextReader(private val state: State, private val body: Sender.(User, String) -> Boolean): Command {
    override fun Sender.process(user: User, update: Update): Boolean {
        if (user.state != state || update.message?.text == null)
            return false
        return body(user, update.message!!.text!!)
    }
}