package org.order.logic.impl.commands.payments

import org.order.bot.send.SenderContext
import org.order.data.entities.State.READ_PAYMENT_AMOUNT
import org.order.data.entities.User
import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.corpus.Text
import org.telegram.telegrambots.meta.api.objects.Update

private object PaymentAmount : Question(READ_PAYMENT_AMOUNT) {
    override fun SenderContext.ask(user: User) =
            user.send(Text["ask-payment-amount"])

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val answer = update.message?.text
        val isValid = answer?.toIntOrNull() != null
                || answer == Text["cancel-payment"]
                || answer == Text["pay-a-debt"]

        if (!isValid) {
            user.send()
            return false
        }

        return true
    }
}

val PAY = QuestionSet()