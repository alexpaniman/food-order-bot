package org.order.logic.impl.commands.registration.state

import org.order.bot.send.Sender
import org.order.bot.send.reply
import org.order.bot.send.show
import org.order.data.entities.*
import org.order.logic.commands.readers.TextReader
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.registration.state.Role.*
import org.order.logic.impl.commands.registration.student.readGrade

private enum class Role(private val key: String) {
    STUDENT ( "student"),
    TEACHER ( "teacher"),
    PARENT  (  "parent"),
    PRODUCER("producer"),
    PARENT_AND_TEACHER("parent-and-teacher");

    val button: String get() = Text[key]
}

fun Sender.readState(user: User) = user.run {
    send(Text["register-state"]) {
        reply { show(values().map { it.button }, 1) }
    }
    state = State.READ_STATE
}


val READ_STATE = TextReader(State.READ_STATE) reader@ { user, text ->
    when (values().singleOrNull { it.button == text }) {
        null     -> {
            user.send(Text["wrong-grade"])
            return@reader true // Break flow and wait for next response
        }
        STUDENT  -> {
            Student.new {
                this.user = user
            }
        }
        TEACHER  -> {
            Teacher.new {
                this.user = user
            }
        }
        PARENT   -> {
            Parent.new {
                this.user = user
            }
        }
        PRODUCER -> {
            Producer.new {
                this.user = user
            }
        }
        PARENT_AND_TEACHER -> {
            Teacher.new {
                this.user = user
            }
            Parent.new {
                this.user = user
            }
        }
    }

    user.state = State.CHOOSES_ROLES // Start flow processing user roles
    return@reader false
}