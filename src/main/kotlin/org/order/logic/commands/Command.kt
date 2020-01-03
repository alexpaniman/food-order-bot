package org.order.logic.commands

import org.order.bot.send.SenderContext
import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.Update

interface Command {
    fun SenderContext.process(user: User, update: Update): Boolean
}