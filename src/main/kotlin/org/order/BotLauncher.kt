package org.order

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.order.bot.CommandsBot
import org.order.bot.send.Sender
import org.order.data.tables.*
import org.order.logic.impl.bot.FoodOrderBot
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.TelegramBotsApi

fun main() {
    // Initialize bots context
    ApiContextInitializer.init()

    // Fetch database url and driver and connect to database
    Database.connect(
            url    = System.getenv("JDBC_DATABASE_URL"),
            driver = System.getenv("DATABASE_DRIVER")
    )
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Users, Grades, Dishes, Menus, Orders)
    }

    // Fetch token and username from the environment
    val token    = System.getenv("BOT_TOKEN")
    val username = System.getenv("BOT_USERNAME")

    // Use the default bot options to create sender that the bot will use to execute telegram api methods
    val options = DefaultBotOptions()
    val sender = Sender(token, options)

    val bot = FoodOrderBot(sender, username, token)

    // Launch bot
    TelegramBotsApi().registerBot(bot)
}
