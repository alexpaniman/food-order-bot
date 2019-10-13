package org.order.bot

import org.order.bot.send.*
import org.order.data.entities.Right
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.data.tables.Users
import org.order.logic.commands.Command
import org.order.logic.loaders.TelegramUserLoader
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

    private val commandHandlers: MutableList<Command> = mutableListOf()
    private val userLoaders: MutableList<TelegramUserLoader> = mutableListOf()

    private fun fetchOrCreateUser(telegramUser: TUser) =
            User.find { Users.chat eq telegramUser.id }.firstOrNull() ?: User.new {
                chat = telegramUser.id

                firstName = telegramUser.firstName
                lastName = telegramUser.lastName
                username = telegramUser.userName

                name = null
                phone = null
                grade = null

                right = Right.CUSTOMER
                state = State.COMMAND
            }

    override fun onUpdateReceived(update: Update) {
        var telegramUser: TUser? = null
        for (loader in userLoaders) {
            telegramUser = loader.loadUser(update)
            if (telegramUser != null)
                break
        }


        val user = fetchOrCreateUser(telegramUser ?: return)

        for (command in commandHandlers) {
            val isMatches = command.matches(user, update)
            if (isMatches) {
                val isProcessed = command.process(sender, user, update)
                if (isProcessed)
                    break
            }
        }
    }

    operator fun plusAssign(command: Command) {
        commandHandlers += command
    }

    operator fun plusAssign(userLoader: TelegramUserLoader) {
        userLoaders += userLoader
    }
}