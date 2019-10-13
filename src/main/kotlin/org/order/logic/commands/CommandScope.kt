package org.order.logic.commands

import org.order.bot.send.Sender
import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.Update

open class CommandScope(val validate: (User) -> Boolean) : Command {
    private val commands: MutableList<Command> = mutableListOf()

    final override fun matches(user: User, update: Update) = validate(user)
    final override fun process(sender: Sender, user: User, update: Update): Boolean {
        var processed = false
        for (command in commands) {
            val isMatches = command.matches(user, update)
            if (isMatches) {
                val isProcessed = command.process(sender, user, update)
                if (isProcessed)
                    processed = true
            }
        }
        return processed
    }

    operator fun plusAssign(command: Command) {
        commands += command
    }
}