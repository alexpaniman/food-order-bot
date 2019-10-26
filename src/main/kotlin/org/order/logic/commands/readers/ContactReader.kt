package org.order.logic.commands.readers

import org.order.bot.send.Sender
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Contact
import org.telegram.telegrambots.meta.api.objects.Update

class ContactReader(private val state: State, private val body: Sender.(User, Contact) -> Boolean): Command {
    override fun Sender.process(user: User, update: Update): Boolean {
        if (user.state != state || update.message?.contact == null)
            return false
        return body(user, update.message!!.contact!!)
    }
}