package org.order.bot.send

import org.order.data.entities.User
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class SenderContext(private val token: String, options: DefaultBotOptions): DefaultAbsSender(options) {
    override fun getBotToken() = token

    private fun Int.send(text: String, markdown: Boolean = true, init: SendMessage.() -> Unit = {}): Message {
        val send = SendMessage().apply {
            this.text = text
            this.chatId = this@send.toString()

            this.enableMarkdown(markdown)
        }.apply(init)

        return execute(send)
    }

    fun User.send(text: String, markdown: Boolean = true, init: SendMessage.() -> Unit = {}) {
        chat!!.send(text, markdown, init)
    }

    fun Message.edit(text: String, markdown: Boolean = true, init: InlineKeyboardMarkup.() -> Unit = {}) = try {
        val send = EditMessageText().apply {
            this.text = text
            this.chatId = this@edit.chatId.toString()
            this.messageId = this@edit.messageId

            this.enableMarkdown(markdown)
        }

        send.replyMarkup = InlineKeyboardMarkup().apply(init)

        execute(send)!!
    } catch (exc: TelegramApiException) {
        chatId.toInt().send(text) { inline(init) }
    }

    fun Message.delete(): Boolean {
        val delete = DeleteMessage().apply {
            chatId = this@delete.chatId.toString()
            messageId = this@delete.messageId
        }

        return execute(delete)!!
    }
}