package org.order.logic.impl.commands.payments

import org.order.logic.commands.processors.PreCheckoutQueryProcessor

val PAYMENT_CONFIRMATION = PreCheckoutQueryProcessor("account-replenishment") { user, id, currency, amount, _ ->
    assert(user.valid) { "Not validated user is trying to pay" }

    answerPreCheckoutQuery(id, true) // TODO create readable errors
}