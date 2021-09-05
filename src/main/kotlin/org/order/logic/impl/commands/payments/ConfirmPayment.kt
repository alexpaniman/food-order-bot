package org.order.logic.impl.commands.payments

import org.order.data.entities.Payment
import org.order.logic.commands.processors.PreCheckoutQueryProcessor
import org.order.logic.impl.commands.CURRENCY

val PAYMENT_CONFIRMATION = PreCheckoutQueryProcessor("account-replenishment") { user, id, currency, amount, (paymentIdStr) ->
    val paymentId = paymentIdStr.toInt()
    val payment = Payment.findById(paymentId) ?: error("There's no payment with id: $paymentId")

    check(currency == CURRENCY) {
        "Wrong currency: $currency instead of $CURRENCY"
    }

    // check(payment.amount!!.withCommission == amount) {
    //    "Wrong payment amount: $amount instead of ${payment.amount!!.withCommission}"
    // }

    check(payment.madeBy == user) {
        "Wrong creator of payment (User ID's): ${user.id.value} instead of ${payment.madeBy.id.value}"
    }

    answerPreCheckoutQuery(id, true) // TODO create readable errors
}