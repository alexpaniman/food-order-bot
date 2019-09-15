package org.order

import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.TelegramBotsApi

fun main() {
    // Initialize bots context
    ApiContextInitializer.init()

    // Fetch token and username from environment
    val token = System.getenv("BOT_TOKEN")
    val username = System.getenv("BOT_USERNAME")

    // Use the default bot options to create sender that the bot will use to execute telegram api methods
    val options = DefaultBotOptions()
    val sender = DefaultSender(token, options)

    val bot = FoodOrderBot(sender, username, token)

    // Launch bot
    TelegramBotsApi().registerBot(bot)
}