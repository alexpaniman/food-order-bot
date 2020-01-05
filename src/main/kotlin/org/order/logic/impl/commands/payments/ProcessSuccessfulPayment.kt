package org.order.logic.impl.commands.payments

import org.joda.time.DateTime
import org.order.data.entities.Payment
import org.order.logic.commands.processors.SuccessfulPaymentProcessor

val PROCESS_SUCCESSFUL_PAYMENT = SuccessfulPaymentProcessor("account-replenishment") { _, telegramId, providerId, (paymentIdStr) ->
    val paymentId = paymentIdStr.toInt()
    val payment = Payment.findById(paymentId) ?: error("There's no payment with id: $paymentId")

    payment.apply {
        this.registered = DateTime.now()

        this.telegramId = telegramId
        this.providerId = providerId
    }

    // TODO include tests and user notification after successful payment
}