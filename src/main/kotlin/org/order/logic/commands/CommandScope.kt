package org.order.logic.commands

import org.order.bot.send.Sender
import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.Update

open class CommandScope(val validate: (User) -> Boolean): Command {
    private val commands: MutableList<Command> = mutableListOf()

    final override fun matches(user: User, update: Update) = validate(user)
    final override fun process(sender: Sender, user: User, update: Update) = commands.forEach {
        if (it.matches(user, update))
            it.process(sender, user, update)
    }

    operator fun plusAssign(command: Command) {
        commands += command
    }
}