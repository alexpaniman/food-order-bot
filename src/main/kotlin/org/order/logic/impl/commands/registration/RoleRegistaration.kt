package org.order.logic.impl.commands.registration

import org.telegram.telegrambots.meta.api.objects.Update

import org.order.bot.send.SenderContext
import org.order.bot.send.button
import org.order.bot.send.reply

import org.order.data.entities.*
import org.order.data.entities.State.*

import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.commands.triggers.StateTrigger

import org.order.logic.corpus.Text

object RoleQuestion : Question(READ_ROLE) {
    override fun SenderContext.ask(user: User) =
            user.send(Text["register-role"]) {
                reply {
                    button(Text[           "student"])
                    button(Text[           "teacher"])
                    button(Text[            "parent"])
                    button(Text[          "producer"])
                    button(Text["parent-and-teacher"])
                }
            }

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        when(update.message?.text) {
            Text[            "parent"] -> Parent  .new { this.user = user }
            Text[          "producer"] -> Producer.new { this.user = user }
            Text[           "student"] -> {
                Student.new { this.user = user }
                Client .new { this.user = user }
            }
            Text[           "teacher"] -> {
                Teacher.new { this.user = user }
                Client .new { this.user = user }
            }
            Text["parent-and-teacher"] -> {
                Teacher.new { this.user = user }
                Parent .new { this.user = user }
                Client .new { this.user = user }
            }
            null -> {
                user.send("wrong-role")
                return false
            }
        }

        return true
    }
}

val ROLE_REGISTRATION = QuestionSet(
        RoleQuestion,
        conclusion = { it.state =  CHOOSE_ROLE },
        trigger    = StateTrigger(NEW)
)