package org.order

import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions

class DefaultSender(private val token: String, options: DefaultBotOptions): DefaultAbsSender(options) {
    override fun getBotToken() = token
}