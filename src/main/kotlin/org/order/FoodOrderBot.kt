package org.order

import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.LongPollingBot
import org.telegram.telegrambots.util.WebhookUtils

class FoodOrderBot(
        private val sender: DefaultAbsSender,

        private val username: String,
        private val token: String
) : LongPollingBot {
    override fun getOptions() = sender.options!!
    override fun clearWebhook() = WebhookUtils.clearWebhook(sender)

    override fun getBotUsername() = username
    override fun getBotToken() = token

    private fun onMessage(text: String) {

    }

    private fun onCallbackQuery(query: String) {

    }

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage()) {
            val message = update.message!!

            val isInUserChat = message.chat.isUserChat == true
            val isNotForward = message.forwardFrom == null
            if (isInUserChat && isNotForward && message.hasText())
                onMessage(message.text!!)
            return
        }
        if (update.hasInlineQuery()) {
            val callbackQuery = update.callbackQuery!!

            val isInUserChat = callbackQuery.message.chat.isUserChat == true
            if (isInUserChat)
                onCallbackQuery(callbackQuery.data!!)
            return
        }
    }
}