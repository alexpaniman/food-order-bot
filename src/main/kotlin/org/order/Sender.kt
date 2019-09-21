package org.order

import org.order.data.entities.User
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class Sender(private val token: String, options: DefaultBotOptions): DefaultAbsSender(options) {
    override fun getBotToken() = token

    private fun Int.send(text: String, markdown: Boolean = true, init: SendMessage.() -> Unit = {}): Message {
        val send = SendMessage().apply {
            this.text = text
            this.chatId = this@send.toString()

            this.enableMarkdown(markdown)
        }.apply(init)

        return execute(send)
    }

    fun User.send(text: String, markdown: Boolean = true, init: SendMessage.() -> Unit = {}): Message = chat.send(text, markdown, init)

    fun Message.edit(text: String, markdown: Boolean = true, init: InlineKeyboardMarkup.() -> Unit = {}) {
        val send = EditMessageText().apply {
            this.text = text
            this.chatId = this@edit.chatId.toString()
            this.messageId = this@edit.messageId

            this.enableMarkdown(markdown)
        }

        send.replyMarkup = InlineKeyboardMarkup().apply(init)

        execute(send)
    }

    fun Message.edit(init: InlineKeyboardMarkup.() -> Unit = {}) {
        val send = EditMessageReplyMarkup().apply {
            this.chatId = this@edit.chatId.toString()
            this.messageId = this@edit.messageId
            this.replyMarkup = InlineKeyboardMarkup().apply(init)
        }

        execute(send)
    }

    fun Message.safeEdit(text: String, markdown: Boolean = true, init: InlineKeyboardMarkup.() -> Unit = {}) = try {
        edit(text, markdown, init)
    } catch (exc: TelegramApiException) {
        chatId.toInt().send(text) { inline(init) }
    }
}