package org.order

import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update

class FoodOrderBot(private val username: String, private val token: String): TelegramLongPollingBot() {
    override fun getBotUsername() = username
    override fun getBotToken() = token

    override fun onUpdateReceived(update: Update?) = TODO()
}