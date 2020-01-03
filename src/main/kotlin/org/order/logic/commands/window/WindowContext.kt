package org.order.logic.commands.window

import org.order.bot.send.SenderContext
import org.order.bot.send.inline
import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup

class WindowContext(private val senderContext: SenderContext,
                    private val message: Message?,
                    private val user: User) {

    fun show(text: String, markdown: Boolean = true, init: InlineKeyboardMarkup.() -> Unit = {}) =
            senderContext.run {
                message?.edit(text, markdown, init) ?: user.send(text, markdown) { inline(init) }
            }
}