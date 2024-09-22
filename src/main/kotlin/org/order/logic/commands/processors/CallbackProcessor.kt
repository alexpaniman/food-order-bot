package org.order.logic.commands.processors

import org.telegram.telegrambots.meta.api.objects.Update
import org.order.bot.send.SenderContext
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Message

class CallbackProcessor(private val marker: String,
                        private val process: SenderContext.(User, Message, List<String>) -> Unit) : Command {

    override fun SenderContext.process(user: User, update: Update): Boolean {
        val callback = update.callbackQuery?.data

        if (callback != null && callback.startsWith("$marker:") || callback == marker) {
            val args = callback
                    .substringAfter(':')
                    .split(":")

            val source = update.callbackQuery.message as Message // ??

            process(user, source, args)
            return true
        }

        return false
    }
}