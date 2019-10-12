package org.order.logic.impl

import org.order.bot.send.button
import org.order.bot.send.reply
import org.order.data.entities.State
import org.order.logic.commands.CommandScope
import org.order.logic.commands.readers.TextReader
import org.order.logic.text.TextCorpus

val READ_NAME = TextReader(State.NAME) { user, text ->
    val isModification = user.name != null

    user.send(TextCorpus["register-name"]) {
        if (isModification) reply {
            button(TextCorpus["cancellation-button"])
        }
    }

    user.state = State.NAME
}

object Registration : CommandScope({ !it.isRegistered }) {

}