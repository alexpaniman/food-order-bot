package org.order.logic.commands

import org.order.bot.send.Sender
import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.Update

class CommandScope(private vararg val commands: Command, val validate: (User) -> Boolean): Command {
    override fun Sender.process(user: User, update: Update): Boolean {
        if (!validate(user))
            return false
        var processed = false
        for (command in commands)
           if(command.run { process(user, update) })
               processed = true
        return processed
    }
}