package org.order.logic.commands.readers

import org.order.bot.send.Sender
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Contact
import org.telegram.telegrambots.meta.api.objects.Update

class ContactReader(private val body: Sender.(User, Contact) -> Unit): Command {
    override fun matches(user: User, update: Update) = update.message?.contact != null
    override fun process(sender: Sender, user: User, update: Update): Boolean {
        sender.body(user, update.message!!.contact!!)
        return true
    }
}