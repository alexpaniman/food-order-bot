package org.order.logic.impl.commands.registration

import org.jetbrains.exposed.sql.insert
import org.telegram.telegrambots.meta.api.objects.Update

import org.order.bot.send.SenderContext
import org.order.bot.send.button
import org.order.bot.send.reply
import org.order.bot.send.show
import org.order.data.entities.*
import org.order.data.entities.State.*

import org.order.data.tables.Grades

import org.order.data.tables.Parents
import org.order.data.tables.Relations

import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.commands.triggers.RoleTrigger
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.commands.triggers.Trigger
import org.order.logic.commands.triggers.and

import org.order.logic.corpus.Text

private fun Parent.createChild(): Student {
    val user    = User   .new { this.state = IMAGINE }
    val student = Student.new { this.user  =    user }

    Relations.insert {
        it[parent] = this@createChild.id
        it[child ] = student.id
    }

    Client.new { this.user = user }
    return student
}

private const val NAME_VALIDATOR = "^[А-ЯЁ][а-яё]+(-[А-ЯЁ][а-яё]+)? [А-ЯЁ][а-яё]+\$"
object ChildNameQuestion: Question(READ_CHILD_NAME) {
    override fun SenderContext.ask(user: User) = user.send(Text["register-child-name"])

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val inputChildName = update.message?.text
        val isValid        = inputChildName?.matches(NAME_VALIDATOR.toRegex()) ?: false

        val linkedParent   = Parent
                .find { Parents.user eq user.id }
                .single()

        val currentStudent = linkedParent.children
                .singleOrNull { it.user.name == null }
                ?: linkedParent.createChild()

        if (isValid)
            currentStudent.user.name = inputChildName
        else
            user.send(Text["wrong-child-name"])

        return isValid
    }
}

object ChildGradeQuestion : Question(READ_CHILD_GRADE) {
    override fun SenderContext.ask(user: User) =
            user.send(Text["register-child-grade"]) {
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

        val linkedParent   = Parent
                .find { Parents.user eq user.id }
                .single()

        val currentStudent = linkedParent.children
                .singleOrNull { it.user.name == null }
                ?: linkedParent.createChild()

        if (grade != null)
            currentStudent.grade = grade
        else
            user.send(Text["wrong-child-grade"])

        return grade != null
    }
}

object AddAnotherChildQuestion: Question(CONFIRM_CHILD_ADDING) {
    override fun SenderContext.ask(user: User) = user.send(Text["suggest-adding-another-child"]) {
        reply {
            button(Text[      "add-child"])
            button(Text["don't-add-child"])
        }
    }

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        when (update.message?.text) {
            Text[      "add-child"] -> {
                ChildNameQuestion.run { ask(user) }

                user.state = READ_CHILD_NAME
            }
            Text["don't-add-child"] -> {
                user.state = VALIDATION

                return true // Move in flow
            }
            else -> user.send(Text["wrong-add-child-response"])
        }

        return false
    }
}

val PARENT_REGISTRATION = QuestionSet(
        ChildNameQuestion, ChildGradeQuestion, AddAnotherChildQuestion,
        trigger = StateTrigger(CHOOSE_ROLE) and RoleTrigger(Parent)
)