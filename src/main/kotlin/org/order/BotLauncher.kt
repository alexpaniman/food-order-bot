package org.order

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.order.data.tables.*
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.TelegramBotsApi

fun main() {
    // Initialize bots context
    ApiContextInitializer.init()

    // Fetch token and username from environment
    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    transaction {
        SchemaUtils.create(Users, Grades, Dishes, Menus, Orders)
    }

    val token = /*System.getenv("BOT_TOKEN")*/ "***REMOVED***"
    val username = /*System.getenv("BOT_USERNAME")*/ "***REMOVED***"

    // Use the default bot options to create sender that the bot will use to execute telegram api methods
    val options = DefaultBotOptions()
    val sender = Sender(token, options)

    val bot = FoodOrderBot(sender, username, token)

    // Launch bot
    TelegramBotsApi().registerBot(bot)
}