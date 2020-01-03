package org.order.logic.commands.questions

import org.order.bot.send.SenderContext
import org.order.data.entities.State
import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.Update

abstract class Question(val state: State) {
    abstract fun SenderContext.ask(user: User)
    abstract fun SenderContext.receive(user: User, update: Update): Boolean
}