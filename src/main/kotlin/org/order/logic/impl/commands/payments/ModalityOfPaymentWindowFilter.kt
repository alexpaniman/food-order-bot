package org.order.logic.impl.commands.payments

import org.order.data.entities.State
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.corpus.Text

val MODALITY_OF_PAYMENT_WINDOW_FILTER = TriggerCommand(StateTrigger(State.MODALITY_OF_PAYMENT_WINDOW)) { user, _ ->
    user.send(Text["make-payment-or-cancel-it"])
}