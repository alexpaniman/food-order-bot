package org.order.logic.commands

import org.order.bot.send.SenderContext
import org.order.data.entities.User
import org.order.logic.commands.triggers.Trigger
import org.telegram.telegrambots.meta.api.objects.Update

class TriggerCommand(private val trigger: Trigger,
                     private val action: SenderContext.(User, Update) -> Unit) : Command {

    override fun SenderContext.process(user: User, update: Update): Boolean {
        if (trigger.test(user, update)) {
            action(user, update)
            return true
        }

        return false
    }
}