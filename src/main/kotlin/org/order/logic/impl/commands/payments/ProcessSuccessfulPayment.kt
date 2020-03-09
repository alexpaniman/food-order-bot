package org.order.logic.impl.commands.payments

import org.joda.time.DateTime
import org.order.data.entities.Admin
import org.order.data.entities.Payment
import org.order.data.entities.Producer
import org.order.data.entities.State
import org.order.logic.commands.processors.SuccessfulPaymentProcessor
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.appendMainKeyboard
import org.order.logic.impl.utils.withCommission

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
    }) { appendMainKeyboard(user) }

    user.state = State.COMMAND

    (Admin.all().map { it.user } + Producer.all().map { it.user })
            .forEach { admin ->
                admin.send(Text.get("payment-notification") {
                    it["user-name"] = payment.madeBy.name!!
                    it["amount"] = payment.amount!!.toString()
                    it["actual-amount"] = payment.amount!!.withCommission.toString()
                    it["client-name"] = payment.client.user.name!!
                    it["registered"] = payment.registered.toString()
                    it["telegram-id"] = payment.telegramId!!
                    it["provider-id"] = payment.providerId!!
                })
            }
}