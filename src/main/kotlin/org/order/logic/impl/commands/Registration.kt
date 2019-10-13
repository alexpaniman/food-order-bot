package org.order.logic.impl.commands

import org.order.bot.send.Sender
import org.order.bot.send.button
import org.order.bot.send.reply
import org.order.bot.send.show
import org.order.data.entities.Grade
import org.order.data.entities.State
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.order.logic.commands.CommandScope
import org.order.logic.commands.handlers.CommandHandler
import org.order.logic.commands.readers.TextReader
import org.order.logic.text.TextCorpus
import org.telegram.telegrambots.meta.api.objects.Update

private const val SINGLE_NAME = "[А-ЯҐІЄЁЇ][а-яґієёї]+"
private const val NAME_VALIDATOR = "$SINGLE_NAME $SINGLE_NAME(-$SINGLE_NAME)?\$"

private const val PHONE_VALIDATOR = "^(\\+)?([- _(]?[0-9]){10,14}$"

private fun Sender.readName (user: User) {
    val isModification = user.name != null

    user.send(TextCorpus["register-name"]) {
        if (isModification)
            reply {
                button(TextCorpus["cancellation-button"])
            }
    }

    user.state = State.NAME
}
private fun Sender.readPhone(user: User) {
    val isModification = user.name != null

    user.send(TextCorpus["register-phone"]) {
        if (isModification)
            reply {
                button(TextCorpus["cancellation-button"])
            }
    }

    user.state = State.PHONE
}
private fun Sender.readGrade(user: User) {
    val isModification = user.name != null

    user.send(TextCorpus["register-grade"]) {
        reply {
            show(Grade.all().map { it.name }, 5)

            if (isModification)
                button(TextCorpus["cancellation-button"])
        }
    }

    user.state = State.GRADE
}

private val READ_NAME  = TextReader (State.NAME) { user, text ->
    if (text matches NAME_VALIDATOR.toRegex()) {
        user.send(TextCorpus.get("right-name") {
            it["name"] = text
        })

        user.name = text
        user.state = State.COMMAND

    } else user.send(TextCorpus["wrong-name"])
}
private val READ_PHONE = TextReader(State.PHONE) { user, text ->
    if (text matches PHONE_VALIDATOR.toRegex()) {
        user.send(TextCorpus.get("right-phone") {
            it["phone"] = text
        })

        user.phone = text
        user.state = State.COMMAND

    } else user.send(TextCorpus["wrong-phone"])
}

private val READ_STATE = TextReader(State.STATE) { user, text ->

}

private val READ_GRADE = TextReader(State.GRADE) { user, text ->
    val grade = Grade.all().singleOrNull { it.name == text }

    if (grade != null) {
        user.send(TextCorpus.get("right-grade") {
            it["grade"] = text
        })

        user.grade = grade
        user.state = State.COMMAND

    } else user.send(TextCorpus["wrong-grade"])
}

private val CHANGE_NAME  = CommandHandler(TextCorpus["command-change-name" ]) { readName(it) }
private val CHANGE_PHONE = CommandHandler(TextCorpus["command-change-phone"]) { readName(it) }
private val CHANGE_GRADE = CommandHandler(TextCorpus["command-change-grade"]) { readName(it) }

private val REGISTER_NEW = object: Command {
    override fun matches(user: User, update: Update) =
            user.state == State.COMMAND && !user.isRegistered

    override fun process(sender: Sender, user: User, update: Update): Boolean {
        when {
            user.name  == null -> sender.readName (user)
            user.phone == null -> sender.readPhone(user)
            user.grade == null -> sender.readGrade(user)
            else -> return false
        }
        return true
    }
}

object Registration : CommandScope({ !it.isRegistered }) {
    init {
        this += READ_NAME
        this += READ_PHONE
        this += READ_GRADE

        this += CHANGE_NAME
        this += CHANGE_GRADE
        this += CHANGE_PHONE

        this += REGISTER_NEW // Must be last
    }
}