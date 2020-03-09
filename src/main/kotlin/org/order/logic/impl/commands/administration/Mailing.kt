package org.order.logic.impl.commands.administration

import org.order.bot.send.button
import org.order.bot.send.reply
import org.order.data.entities.Admin
import org.order.data.entities.Client
import org.order.data.entities.State.*
import org.order.data.entities.User
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.triggers.*
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.appendMainKeyboard
import java.lang.Thread.sleep

private val MAILING_TRIGGER = StateTrigger(COMMAND) and
        RoleTrigger(Admin) and
        CommandTrigger(Text["mailing-command"])

val MAILING_MENU = TriggerCommand(MAILING_TRIGGER) { user, _ ->
    user.send(Text["select-mailing-type"]) {
        reply {
            button(Text["send-message-with-keyboard-to-all-users"])
            button(Text["send-message-to-all-users"])
            button(Text["send-message-to-all-clients"])
            button(Text["suggest-registration-to-all-new-users"])
            button(Text["cancel-button"])
        }
    }

    user.state = READ_MAILING_TYPE
}

private val SUGGEST_MAILING_A_MESSAGE_TRIGGER = StateTrigger(READ_MAILING_TYPE)
val SUGGEST_MAILING_A_MESSAGE_TO_MAIL = TriggerCommand(SUGGEST_MAILING_A_MESSAGE_TRIGGER) mailing@{ user, update ->
    when (update.message?.text) {
        Text["send-message-with-keyboard-to-all-users"] ->
            user.state = SEND_MESSAGE_WITH_KEYBOARD_TO_ALL_USERS

        Text["send-message-to-all-users"] ->
            user.state = SEND_MESSAGE_TO_ALL_USERS

        Text["send-message-to-all-clients"] ->
            user.state = SEND_MESSAGE_TO_ALL_CLIENTS

        Text["suggest-registration-to-all-new-users"] ->
            user.state = SUGGEST_REGISTRATION_TO_ALL_NEW_USERS

        Text["cancel-button"] -> {
            user.state = COMMAND
            user.send(Text["successful-mailing-cancellation"]) {
                appendMainKeyboard(user)
            }
            return@mailing
        }

        else -> {
            user.send(Text["wrong-mailing-type"])
            return@mailing
        }
    }

    user.send(Text["write-a-message-to-mail"]) {
        reply {
            button(Text["cancel-button"])
        }
    }
}

private val PERFORM_MAILING_TRIGGER =
        StateTrigger(SEND_MESSAGE_WITH_KEYBOARD_TO_ALL_USERS) or
                StateTrigger(SEND_MESSAGE_TO_ALL_USERS) or
                StateTrigger(SEND_MESSAGE_TO_ALL_CLIENTS) or
                StateTrigger(SUGGEST_REGISTRATION_TO_ALL_NEW_USERS)
val PERFORM_MAILING = TriggerCommand(PERFORM_MAILING_TRIGGER) mailing@ { user, update ->
    val text = update.message?.text
    if (text == Text["cancel-button"]) {
        user.send(Text["successful-mailing-cancellation"]) {
            appendMainKeyboard(user)
        }
        user.state = COMMAND
        return@mailing
    }

    if (text == null) {
        user.send(Text["wrong-message-to-mail"])
        return@mailing
    }

    when (user.state) {
        SEND_MESSAGE_WITH_KEYBOARD_TO_ALL_USERS ->
            User.all()
                    .filter { it.chat != null }
                    .forEach {
                        it.send(text) {
                            appendMainKeyboard(it)
                        }
                        sleep(35)
                    }

        SEND_MESSAGE_TO_ALL_USERS ->
            User.all()
                    .filter { it.chat != null }
                    .forEach {
                        it.send(text)
                        sleep(35)
                    }

        SEND_MESSAGE_TO_ALL_CLIENTS ->
            Client.all()
                    .map { it.user }
                    .filter { it.chat != null }
                    .forEach {
                        it.send(text)
                        sleep(35)
                    }

        SUGGEST_REGISTRATION_TO_ALL_NEW_USERS ->
            User.all()
                    .filter { it.state == COMMAND && it.name == null && it.chat != null }
                    .forEach {
                        try {
                            it.send(text)

                            it.state = READ_NAME
                            it.send(Text["register-name"])
                            sleep(70)
                        } catch (exc: Exception) {
                            user.send("Error occurred: $exc")
                            sleep(35)
                        }
                    }

        else -> error("Trigger doesn't work!")
    }

    user.send(Text["mailing-was-done"]) {
        appendMainKeyboard(user)
    }

    user.state = COMMAND
}