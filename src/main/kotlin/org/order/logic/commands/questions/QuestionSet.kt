package org.order.logic.commands.questions

import org.order.bot.send.SenderContext
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.order.logic.commands.triggers.Trigger
import org.telegram.telegrambots.meta.api.objects.Update

class QuestionSet(
        private vararg val questions: Question,
        private val beginning : SenderContext.(User) -> Unit = {},
        private val conclusion: SenderContext.(User) -> Unit = {},
        val trigger: Trigger) : Command {

    override fun SenderContext.process(user: User, update: Update): Boolean {
        for (index in questions.indices) {
            val question = questions[index]

            if (user.state == question.state) {
                val askNext = question.run { receive(user, update) } // Receive response from previous question
                if (!askNext)
                    return true

                val nextQuestion = questions
                        .getOrNull(index + 1) // Get next question

                return if (nextQuestion != null) {
                    nextQuestion.run { ask(user) } // Ask next question of user
                    user.state = nextQuestion.state

                    true  // Next question was asked, break flow
                } else {
                    conclusion(user) // This question set was ended

                    false // Move in flow to next command
                }
            }
        }

        if (trigger.test(user, update)) {
            beginning(user) // This question set was started

            val firstQuestion = questions[0]

            user.state = firstQuestion.state
            firstQuestion.run { ask(user) }

            return true
        }

        return false // This question set isn't for this user
    }

}