package org.order.logic.impl.commands.payments

import org.order.data.entities.Payment
import org.order.data.entities.State
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.appendMainKeyboard

val REMOVE_CANCELED_PAYMENT = CallbackProcessor("remove-canceled-payment") { user, src, (paymentIdStr) ->
    val paymentId = paymentIdStr.toInt()
    val payment = Payment.findById(paymentId) ?: error("There's no payment with id: $paymentId")

    if (payment.registered == null) {
        payment.delete()
        user.state = State.COMMAND

        user.send(Text["canceled-payment-was-successful-removed"]){
            appendMainKeyboard(user)
        }
    }

    src.delete()
}