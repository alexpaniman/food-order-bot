package org.order.logic.impl.commands.payments

import org.joda.time.DateTime
import org.order.bot.send.SenderContext
import org.order.bot.send.button
import org.order.bot.send.reply
import org.order.data.entities.*
import org.order.data.entities.State.*
import org.order.logic.commands.processors.SuccessfulPaymentProcessor
import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.commands.triggers.NegativeTrigger
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.appendMainKeyboard
import org.order.logic.impl.utils.clients
import org.order.logic.impl.utils.withCommission
import org.telegram.telegrambots.meta.api.objects.Update

val PROCESS_SUCCESSFUL_PAYMENT = SuccessfulPaymentProcessor("account-replenishment") { user, telegramId, providerId, totalAmount, _ ->
    val payment = Payment.new {
        this.madeBy = user

        this.registered = DateTime.now()

        this.telegramId = telegramId
        this.providerId = providerId

        this.amount = totalAmount
    }

    if (user.clients().size > 1)
        with(AskWhichClientToReplenish) {
            ask(user)
            user.state = ASK_WHICH_CLIENT_TO_PAY_FOR
        }
    else {
        payment.client = user.clients().first()
        processFinishedPayment(user, payment)
    }
}

private fun SenderContext.processFinishedPayment(user: User, payment: Payment) {
    payment.client!!.balance += payment.amount!!

    user.send(Text.get("successful-payment") {
        it["amount"] = payment.amount.toString()
    }) { appendMainKeyboard(user) }

    user.state = COMMAND

    (Admin.all().map { it.user } + Producer.all().map { it.user })
            .forEach { admin ->
                admin.send(Text.get("payment-notification") {
                    it["user-name"] = payment.madeBy.name!!
                    it["amount"] = payment.amount!!.toString()
                    it["actual-amount"] = payment.amount!!.withCommission.toString()
                    it["client-name"] = payment.client!!.user.name!!
                    it["registered"] = payment.registered!!.toString("yyyy-MM-dd HH:mm:ss")
                    it["telegram-id"] = payment.telegramId!!
                    it["provider-id"] = payment.providerId!!
                })
            }
}

object AskWhichClientToReplenish: Question(ASK_WHICH_CLIENT_TO_PAY_FOR) {
    private fun SenderContext.sendWithListOfClients(user: User, message: String) =
            user.send(message) {
                val clients = user.clients()
                        .mapNotNull { it.user.name }

                reply {
                    for (clientName in clients)
                        button(clientName)
                }
            }

    override fun SenderContext.ask(user: User) =
            sendWithListOfClients(user, Text.get("ask-payment-client") {
                it["amount"] = user.payments
                        .find { payment -> payment.client == null }!!
                        .amount.toString()
            })

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val targetClientName = update.message?.text
        val targetClients = user.clients()
                .filter { it.user.name == targetClientName }

        val payment = user.payments
                .find { it.client == null }!!

        if (targetClients.isEmpty()) {
            sendWithListOfClients(user, Text.get("wrong-payment-client") {
                it["amount"] = "${payment.amount!!}"
            })
            return false
        }

        val targetClient = targetClients.first()
        payment.client = targetClient

        processFinishedPayment(user, payment)
        return true
    }
}

val PAYMENT_CLIENT_SELECTOR = QuestionSet(AskWhichClientToReplenish, trigger = NegativeTrigger())