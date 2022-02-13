package org.order.logic.impl.commands.payments

import com.jakewharton.picnic.table
import org.order.bot.send.SenderContext
import org.order.bot.send.button
import org.order.bot.send.reply
import org.order.data.entities.Client
import org.order.data.entities.Parent
import org.order.data.entities.State.*
import org.order.data.entities.User
import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.commands.triggers.*
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.COMMISSION
import org.order.logic.impl.utils.*
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.math.absoluteValue

private object PaymentAmount : Question(READ_PAYMENT_AMOUNT) {
    override fun SenderContext.ask(user: User) {
        val clients = user.clients()
        val message = table {
            defaultSettings()

            row {
                cell(Text["name-column"])
                cell(Text["balance-column"])
            }

            for (client in clients)
                row {
                    cell(client.user.name)
                    cell(client.balance)
                }
        }.stringifyTable() + "\n" + Text["ask-payment-amount"]

        val totalDebt = clients
                .map { it.balance }.sum()

        user.send(message) {
            reply {
                if (totalDebt < 0)
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

        val balance = user.clients()
                .map { it.balance }.sum()

        val amount = if (answer == Text["pay-a-debt"])
            balance.absoluteValue
        else answer?.toFloatOrNull()

        if (amount == null) {
            user.send(Text["wrong-payment-amount"])
            return false
        }

        val actualAmount = amount.withCommission
        user.sendInvoice(
                Text["payment-title"], amount,
                Text.get("payment-description") {
                    it["amount"] = amount.toString()
                    it["commission"] = (COMMISSION * 100f).toString()
                    it["actual-amount"] = actualAmount.toString()
                },
                "account-replenishment:$actualAmount"
        ) {
            button(Text.get("pay-button") {
                it["actual-amount"] = actualAmount.toString()
            }, pay = true)

            button(Text["cancel-button"], "remove-message")
        }

        user.state = COMMAND
        return true
    }
}

private val PAYMENT_TRIGGER = CommandTrigger(Text["pay-command"]) and StateTrigger(COMMAND) and
        (RoleTrigger(Client) or RoleTrigger(Parent))
val PAYMENT = QuestionSet(PaymentAmount, trigger = PAYMENT_TRIGGER)
