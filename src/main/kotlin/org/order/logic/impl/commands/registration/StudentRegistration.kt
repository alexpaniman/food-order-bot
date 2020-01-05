package org.order.logic.impl.commands.registration


import org.order.bot.send.SenderContext
import org.order.bot.send.reply
import org.order.bot.send.show
import org.order.data.entities.Grade
import org.order.data.entities.State.*
import org.order.data.entities.Student
import org.order.data.entities.User
import org.order.data.tables.Grades
import org.order.data.tables.Students
import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.commands.triggers.RoleTrigger
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.commands.triggers.Trigger
import org.order.logic.corpus.Text
import org.telegram.telegrambots.meta.api.objects.Update

object GradeQuestion : Question(READ_GRADE) {
    override fun SenderContext.ask(user: User) =
            user.send(Text["register-grade"]) {
                reply {
                    val grades = Grade.all().map { it.name } // Get all grade names

                    show(grades, 5) // And show them (5 per row)
                }
            }

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val inputGrade = update.message?.text
        val grade      = if (inputGrade != null)
            Grade.find { Grades.name eq inputGrade }
                    .singleOrNull() // Null if there's no grade with same name
        else null // Or input doesn't contains text

        val currentStudent = Student
                .find { Students.user eq user.id }
                .single()

        if (grade != null)
            currentStudent.grade = grade
        else
            user.send(Text["wrong-grade"])

        return grade != null
    }
}

val STUDENT_REGISTRATION = QuestionSet(
        GradeQuestion,
        conclusion = { it.state = VALIDATION },
        trigger    = StateTrigger(CHOOSE_ROLE) + RoleTrigger(Student)
)