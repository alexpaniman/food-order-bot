package org.order.bot.send

import org.order.data.entities.User
import org.order.logic.impl.commands.CURRENCY
import org.order.logic.impl.commands.INVOICE_START_PARAMETER
import org.order.logic.impl.commands.PAYMENTS_TOKEN
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.File

class SenderContext(private val token: String, options: DefaultBotOptions): DefaultAbsSender(options) {
    override fun getBotToken() = token

    private fun Int.send(text: String, markdown: Boolean = true, init: SendMessage.() -> Unit = {}): Message {
        val sendText = SendMessage().apply {
            this.text = text
            this.chatId = this@send.toString()

            this.enableMarkdown(markdown)
        }.apply(init)

        return execute(sendText)
    }

    fun User.send(text: String, markdown: Boolean = true, init: SendMessage.() -> Unit = {}) {
        chat!!.send(text, markdown, init)
    }

    fun Message.edit(text: String, markdown: Boolean = true, init: InlineKeyboardMarkup.() -> Unit = {}) = try {
        val sendText = EditMessageText().apply {
            this.text = text
            this.chatId = this@edit.chatId.toString()
            this.messageId = this@edit.messageId

            this.enableMarkdown(markdown)
        }

        sendText.replyMarkup = InlineKeyboardMarkup().apply(init)

        execute(sendText)!!
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

    fun User.sendInvoice(title: String, amount: Float, description: String, payload: String, init: InlineKeyboardMarkup.() -> Unit = {}) {
        val realAmount = (amount * 100).toInt()
        val sendInvoice = SendInvoice().also {
            it.chatId = chat.toString()

            it.title = title
            it.prices = listOf(LabeledPrice(title, realAmount))
            it.description = description

            it.payload = payload

            it.currency = CURRENCY
            it.providerToken = PAYMENTS_TOKEN
            it.startParameter = INVOICE_START_PARAMETER

            it.replyMarkup = InlineKeyboardMarkup().apply(init)
        }

        execute(sendInvoice)
    }

    fun User.sendFile(name: String, caption: String, file: File, init: SendDocument.() -> Unit = {}) {
        val sendFile = SendDocument().also {
            it.chatId = chat.toString()

            it.caption = caption
            it.document = InputFile(file, name)
        }.apply(init)

        execute(sendFile)
    }

    fun answerPreCheckoutQuery(id: String, ok: Boolean, errorMessage: String? = null) {
        val answerPreCheckoutQuery = AnswerPreCheckoutQuery().apply {
            this.preCheckoutQueryId = id

            this.ok = ok
            this.errorMessage = errorMessage
        }

        execute(answerPreCheckoutQuery)
    }
}