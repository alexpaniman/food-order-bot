package org.order.logic.commands.window

import org.order.bot.send.SenderContext
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.order.logic.commands.triggers.Trigger
import org.telegram.telegrambots.meta.api.objects.Update

class Window(private val marker: String,
             private val trigger: Trigger,
             private val args: List<String>,
             private val window: WindowContext.(User, List<String>) -> Unit): Command {

    override fun SenderContext.process(user: User, update: Update): Boolean {
        val callback = update.callbackQuery?.data
        if (callback != null && callback.startsWith("$marker:")) {
            val args = callback
                    .substringAfter(':')
                    .split(":")

            val source = update.callbackQuery.message

            WindowContext(this, source, user)
                    .window(user, args)
            return true
        }

        if (trigger.test(user, update)) {
            WindowContext(this, null, user)
                    .window(user, args)
            return true
        }

        return false
    }
}