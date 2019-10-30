package org.order.logic.impl.commands.registration.parent

import org.order.bot.send.Sender
import org.order.bot.send.reply
import org.order.bot.send.show
import org.order.data.entities.*
import org.order.data.tables.Parents
import org.order.data.tables.Students
import org.order.logic.commands.readers.TextReader
import org.order.logic.corpus.Text

fun Sender.readChildGrade(user: User) = user.run {
    send(Text["register-child-grade"]) {
        reply {
            val  grades = Grade
                    .all()
                    .map { it.name }

            show(grades, 5)
        }
    }

    state = State.READ_CHILD_GRADE
}

val READ_CHILD_GRADE = TextReader(State.READ_CHILD_GRADE) reader@ { user, text ->
    val parent = Parent
            .find   { Parents.user eq user.id }
            .single()

    val child = Student
            .find   { Students.grade.isNull() }
            .single { parent in it.parents    }

    val grade = Grade.all()
            .singleOrNull {  it.name == text  }

    if (grade != null) {
        user .state = State.COMMAND
        child.grade = grade
    } else user.send(Text["wrong-grade"])

    return@reader false
}