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
import org.order.logic.commands.triggers.or

import org.order.logic.corpus.Text

object RoleQuestion : Question(READ_ROLE) {
    override fun SenderContext.ask(user: User) =
            user.send(Text["register-role"]) {
                reply {
                    button(Text["student"])
                    button(Text["teacher"])
                    button(Text["parent"])
                    button(Text["producer"])
                    button(Text["parent-and-teacher"])
                }
            }

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        when (update.message?.text) {
            Text["parent"] -> {
                Parent.new {
                    this.user = user
                }
                user.state = CHOOSE_ROLE
            }
            Text["producer"] -> {
                Producer.new {
                    this.user = user
                }
                user.state = REGISTRATION_FINISHED
            }
            Text["student"] -> {
                Student.new {
                    this.user = user
                }
                Client.new {
                    this.user = user
                }
                user.state = CHOOSE_ROLE
            }
            Text["teacher"] -> {
                Teacher.new {
                    this.user = user
                }
                Client.new {
                    this.user = user
                }
                user.state = REGISTRATION_FINISHED
            }
            Text["parent-and-teacher"] -> {
                Teacher.new {
                    this.user = user
                }
                Parent.new {
                    this.user = user
                }
                Client.new {
                    this.user = user
                }
                user.state = CHOOSE_ROLE
            }
            else -> {
                user.send(Text["wrong-role"])
                return false
            }
        }

        return true
    }
}

val ROLE_REGISTRATION = QuestionSet(
        RoleQuestion,
        trigger = StateTrigger(NEW) or StateTrigger(READ_ROLE)
)