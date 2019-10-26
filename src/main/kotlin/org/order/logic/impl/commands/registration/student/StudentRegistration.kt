package org.order.logic.impl.commands.registration.student

import org.order.bot.send.Sender
import org.order.data.entities.State
import org.order.data.entities.Student
import org.order.data.entities.User
import org.order.data.tables.Students
import org.order.logic.commands.Command
import org.order.logic.commands.CommandScope
import org.order.logic.corpus.Text
import org.telegram.telegrambots.meta.api.objects.Update

val REGISTER_NEW = object : Command {
    override fun Sender.process(user: User, update: Update): Boolean {
        if (user.state != State.COMMAND && update.message?.text != Text["student"])
            return false

        val student = Student
                .find { Students.user eq user.id }
                .singleOrNull() ?: return false

        return if (student.grade == null) { readGrade(user); true } else false
    }
}

val STUDENT_REGISTRATION = CommandScope(READ_GRADE, REGISTER_NEW) { !it.valid }