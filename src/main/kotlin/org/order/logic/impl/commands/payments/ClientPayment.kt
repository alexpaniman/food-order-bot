package org.order.logic.impl.commands.payments

import org.order.bot.send.SenderContext
import org.order.bot.send.button
import org.order.bot.send.reply
import org.order.data.entities.Client
import org.order.data.entities.Payment
import org.order.data.entities.State.*
import org.order.data.entities.User
import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.commands.triggers.RoleTrigger
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.triggers.and
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.COMMISSION
import org.order.logic.impl.utils.appendMainKeyboard
import org.order.logic.impl.utils.withCommission
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.math.absoluteValue

private object ClientPaymentAmount : Question(READ_CLIENT_PAYMENT_AMOUNT) {
    override fun SenderContext.ask(user: User) {
        val balance = user.linked(Client).balance

        user.send(Text.get("ask-payment-amount") { it["balance"] = "$balance" }) {
            reply {
                if (balance < 0)
                    button(Text["pay-a-debt"])
                button(Text["cancel-button"])
            }
        }
    }

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val answer = update.message?.text

        if (answer == Text["cancel-button"]) {
            user.send(Text["successful-payment-cancellation"]) {
                appendMainKeyboard(user)
            }
            user.state = COMMAND
            return true
        }

        val client = user.linked(Client)
        val balance = client.balance

        val amount = if (answer == Text["pay-a-debt"])
            balance.absoluteValue
        else answer?.toFloatOrNull()

        if (amount == null) {
            user.send(Text["wrong-payment-amount"])
            return false
        }

        val unfinishedPayment = Payment.new {
            this.client = user.linked(Client)
            this.madeBy = user

            this.amount = amount
        }

        val actualAmount = amount.withCommission
        user.sendInvoice(
                Text["payment-title"], amount,
                Text.get("payment-description") {
                    it["amount"] = amount.toString()
                    it["commission"] = (COMMISSION * 100f).toString()
                    it["actual-amount"] = actualAmount.toString()
                },
                "account-replenishment:${unfinishedPayment.id.value}"
        ) {
            button(Text.get("pay-button") {
                it["actual-amount"] = actualAmount.toString()
            }, pay = true)

            button(Text["cancel-button"], "remove-canceled-payment:${unfinishedPayment.id.value}")
        }

        user.state = MODALITY_OF_PAYMENT_WINDOW
        return true
    }
}

private val CLIENT_PAYMENT_TRIGGER = CommandTrigger(Text["pay-command"]) and RoleTrigger(Client)
val CLIENT_PAYMENT = QuestionSet(ClientPaymentAmount, trigger = CLIENT_PAYMENT_TRIGGER)