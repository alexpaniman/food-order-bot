package org.order

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.order.bot.send.SenderContext
import org.order.data.entities.*
import org.order.data.tables.*
import org.order.logic.impl.FoodOrderBot
import org.order.logic.impl.commands.BOT_TOKEN
import org.order.logic.impl.commands.BOT_USERNAME
import org.order.logic.impl.commands.DATABASE_DRIVER
import org.order.logic.impl.commands.JDBC_DATABASE_URL
import org.order.logic.impl.utils.Schedule
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
                Users,
                OrdersCancellations
        )

        val user = User.new {
            this.chat = 505120843
            this.name = "Паниман Александр"
            this.valid = true
            this.state = State.COMMAND
            this.phone = "+380669362726"
        }

        val grade = Grade.new {
            this.name = "10-Ф"
        }

        Student.new {
            this.user = user
            this.grade = grade
        }

        val client = Client.new {
            this.user = user
            this.balance = 0f
        }

        val menu = Menu.new {
            this.name = "menu"
            this.schedule = Schedule.parse("15-02-2019:2")
            this.cost = 100f
        }

        Order.new {
            this.menu = menu
            this.orderDate = LocalDate.now()
            this.registered = DateTime.now()
            this.madeBy = user
            this.client = client
        }

        createData() // TODO remove
    }

    // Use the default bot options to create sender that the bot will use to execute telegram api methods
    val options = DefaultBotOptions()
    val sender = SenderContext(BOT_TOKEN, options)


    val bot = FoodOrderBot(sender, BOT_USERNAME, BOT_TOKEN)

    // Launch bot
    TelegramBotsApi().registerBot(bot)
}
