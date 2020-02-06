package org.order.logic.impl.commands.payments

import org.joda.time.DateTime
import org.order.data.entities.Payment
import org.order.logic.commands.processors.SuccessfulPaymentProcessor
import org.order.logic.corpus.Text

val PROCESS_SUCCESSFUL_PAYMENT = SuccessfulPaymentProcessor("account-replenishment") { user, telegramId, providerId, (paymentIdStr) ->
    val paymentId = paymentIdStr.toInt()
    val payment = Payment.findById(paymentId) ?: error("There's no payment with id: $paymentId")

    payment.apply {
        this.registered = DateTime.now()

        this.telegramId = telegramId
        this.providerId = providerId
    }

    payment.client.balance += payment.amount!!

    user.send(Text.get("successful-payment") {
        it["amount"] = payment.amount.toString()
    })
}