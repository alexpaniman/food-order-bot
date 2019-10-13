package org.order.logic.commands

import org.order.bot.send.Sender
import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.Update

interface Command {
    fun matches(user: User, update: Update): Boolean
    fun process(sender: Sender, user: User, update: Update): Boolean
}