package org.order

import createOrUpdatePostgreSQLEnum
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.order.bot.send.SenderContext
import org.order.data.entities.State
import org.order.data.tables.*
import org.order.logic.impl.FoodOrderBot
import org.order.logic.impl.commands.BOT_TOKEN
import org.order.logic.impl.commands.BOT_USERNAME
import org.order.logic.impl.commands.DATABASE_DRIVER
import org.order.logic.impl.commands.JDBC_DATABASE_URL
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession



fun main() {
    // Initialize bots context
    val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)

    // Fetch database url and driver and connect to database
    Database.connect(url = JDBC_DATABASE_URL, driver = DATABASE_DRIVER)

    transaction {
        addLogger(StdOutSqlLogger)

        createOrUpdatePostgreSQLEnum(State.entries.toTypedArray())

        SchemaUtils.create(
                Teachers, Admins, Clients, Dishes, Grades,
                Menus, Orders, Parents, Payments, Producers,
                Relations, Coordinators, Users, PollAnswers,
                OrdersCancellations, PollComments, Properties,
                TempProperties, RefundComments
        )
    }

    // Use the default bot options to create sender that the bot will use to execute telegram api methods
    val options = DefaultBotOptions()
    val sender = SenderContext(BOT_TOKEN, options)

    val bot = FoodOrderBot(sender, BOT_USERNAME, BOT_TOKEN)

    // Launch bot
    telegramBotsApi.registerBot(bot)
}