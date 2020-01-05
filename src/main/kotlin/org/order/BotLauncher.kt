package org.order

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.order.bot.send.SenderContext
import org.order.data.entities.Admin
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.data.tables.*
import org.order.logic.impl.FoodOrderBot
import org.order.logic.impl.commands.BOT_TOKEN
import org.order.logic.impl.commands.BOT_USERNAME
import org.order.logic.impl.commands.DATABASE_DRIVER
import org.order.logic.impl.commands.JDBC_DATABASE_URL
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.TelegramBotsApi

fun main() {
    // Initialize bots context
    ApiContextInitializer.init()

    // Fetch database url and driver and connect to database
    Database.connect(url = JDBC_DATABASE_URL, driver = DATABASE_DRIVER)

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(
                Teachers,
                Admins,
                Clients,
                Dishes,
                Grades,
                Menus,
                Orders,
                Parents,
                Payments,
                Producers,
                Relations,
                Coordinators,
                Users
        )
    }

    transaction { // TODO remove it
        Grades.batchInsert(listOf("10-Ф", "11-Ф", "5", "11-ХБ", "7-М", "9-Ф")) {
            this[Grades.name] = it
        }
        val admin = User.new {
            name = "Паниман Александр"
            chat = 637833277
            phone = "+380669362726"
            state = State.COMMAND
        }
        Admin.new {
            user = admin
        }
    }

    // Use the default bot options to create sender that the bot will use to execute telegram api methods
    val options = DefaultBotOptions()
    val sender = SenderContext(BOT_TOKEN, options)

    val bot = FoodOrderBot(sender, BOT_USERNAME, BOT_TOKEN)

    // Launch bot
    TelegramBotsApi().registerBot(bot)
}
