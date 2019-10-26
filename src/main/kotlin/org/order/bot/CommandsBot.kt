package org.order.bot

import org.jetbrains.exposed.sql.transactions.transaction
import org.order.bot.send.*
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.data.tables.Users
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.LongPollingBot
import org.telegram.telegrambots.util.WebhookUtils

import org.telegram.telegrambots.meta.api.objects.User as TUser

open class CommandsBot(
        private val sender: Sender,

        private val username: String,
        private val token: String
) : LongPollingBot {
    override fun getOptions() = sender.options!!
    override fun clearWebhook() = WebhookUtils.clearWebhook(sender)

    override fun getBotUsername() = username
    override fun getBotToken() = token

    private val handlers: MutableList<Command> = mutableListOf()
    private fun fetchOrCreateUser(rawUser: TUser) =
            User.find { Users.chat eq rawUser.id }.firstOrNull() ?: User.new {
                chat  = rawUser.id

                name  = null
                phone = null

                valid = false
                state = State.NEW
            }

    override fun onUpdateReceived(update: Update) = transaction {
        val user = fetchOrCreateUser(
                update.message?.from
                        ?: update.callbackQuery   ?.from
                        ?: update.preCheckoutQuery?.from
                        ?: return@transaction
        )

        for (command in handlers)
            if (command.run { sender.process(user, update) })
                return@transaction
    }

    operator fun plusAssign(command: Command) {
        handlers += command
    }
}