package org.order.logic.impl.commands.registration.parent

import org.order.bot.send.Sender
import org.order.bot.send.reply
import org.order.bot.send.show
import org.order.data.entities.State
import org.order.data.entities.Student
import org.order.data.entities.User
import org.order.logic.commands.readers.TextReader
import org.order.logic.corpus.Text

private const val NAME_VALIDATOR = "$[А-ЯЁ][а-яё]+(-[А-ЯЁ][а-яё]+)? [А-ЯЁ][а-яё]+\$"

fun Sender.readChildName(user: User) = user.run {
    send(Text["register-child-name"])

    state = State.READ_CHILD_GRADE
}

val READ_CHILD_NAME = TextReader(State.READ_CHILD_NAME) reader@{ user, text ->
    val isInputValid = text matches NAME_VALIDATOR.toRegex()

    if (isInputValid) {
        val studentsWithSameName = Student.all()
                .filter { it.user.name == text }

        if (studentsWithSameName.isEmpty()) {
            Student.new {
                this.user = User.new {
                    this.state = State.IMAGINE
                    this.valid = false

                    this.name  = text
                }

                this.grade = null // Grade will be set later
            }

            user.name  = text
            user.state = State.COMMAND
        } else {
            user.send(Text["link-child"]) {
                reply { show(studentsWithSameName, 1) }
            }

            user.state = State.READ_CHILD_LINK
        }
    } else user.send(Text["wrong-name"])

    return@reader false
}

val READ_CHILD_LINK = TextReader(State.READ_CHILD_LINK) reader@ { user, text ->
    fun wrongChildLink() = user.send(Text["wrong-child-link"])
    val (name, grade) = text.split(", ").apply {  }

    val students = Student
            .all()
            .singleOrNull {
                it.grade!!.name == grade
                        && it.user.name == name
            } // TODO rewrite with join


    return@reader false
}