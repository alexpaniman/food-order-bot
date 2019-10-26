package org.order.logic.impl.commands.registration.student

import org.order.bot.send.Sender
import org.order.bot.send.reply
import org.order.bot.send.show
import org.order.data.entities.Grade
import org.order.data.entities.State
import org.order.data.entities.Student
import org.order.data.entities.User
import org.order.data.tables.Grades
import org.order.data.tables.Students
import org.order.logic.commands.readers.TextReader
import org.order.logic.corpus.Text

fun Sender.readGrade(user: User) = user.run {
    send(Text["register-grade"]) {
        reply {
            val grades = Grade.all()
                    .map { it.name }

            show(grades, 5)
        }
    }

    state = State.READ_GRADE
}

val READ_GRADE = TextReader(State.READ_GRADE) reader@ { user, text ->
    val student = Student
            .find { Students.user eq user.id }
            .single()

    val grade = Grade
            .find { Grades.name eq text }
            .singleOrNull()

    if (grade != null) {
        user   .state = State.COMMAND
        student.grade = grade
    } else user.send(Text["wrong-grade"])

    return@reader true
}