package org.order.logic.commands.callback

import org.telegram.telegrambots.meta.api.objects.Update
import org.order.bot.send.SenderContext
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Message

class CallbackProcessor(private val marker: String,
                        private val process: SenderContext.(User, Message, List<String>) -> Unit) : Command {

    override fun SenderContext.process(user: User, update: Update): Boolean {
        val callback = update.callbackQuery?.data

        if (callback != null && callback.startsWith("$marker:")) {
            val args = callback
                    .substringAfter(':')
                    .split(":")

            val source = update.callbackQuery.message
            process(user, source, args)
            return true
        }

        return false
    }
}