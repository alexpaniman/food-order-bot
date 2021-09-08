package org.order.logic.impl.commands.payments

import org.order.bot.send.SenderContext
import org.order.bot.send.button
import org.order.data.entities.Parent
import org.order.data.entities.Payment
import org.order.data.entities.State.*
import org.order.data.entities.User
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.triggers.RoleTrigger
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.commands.triggers.and
import org.order.logic.commands.window.WindowContext
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.COMMISSION
import org.order.logic.impl.utils.clients
import org.order.logic.impl.utils.orZero
import org.order.logic.impl.utils.withCommission
import org.telegram.telegrambots.meta.api.objects.Update

private const val WINDOW_MARKER = "read-parent-payment-amount-window"

private fun WindowContext.showParentPaymentWindow(user: User, childNum: Int) {
    val clients = user.clients()
    val clientNum = childNum.coerceIn(clients.indices)
    val client = clients[clientNum]

    val name = client.user.name!!
    val balance = client.balance

    val unfinishedPayment = user.payments.find {
        it.registered == null // Find unfinished payment
    }?.apply {
        this.client = client // Update old client
    } ?: Payment.new {
        this.madeBy = user
        this.client = client // Or create new payment
    }

    show(Text.get("ask-payment-amount") { it["balance"] = "$balance" }) {
        if (user.hasLinked(Parent))
            button(name, "$WINDOW_MARKER:${(clientNum + 1).orZero(clients.indices)}")
        button(Text["cancel-button"], "remove-canceled-payment:${unfinishedPayment.id.value}")
    }
}

val UPDATE_PARENT_PAYMENT_WINDOW = CallbackProcessor(WINDOW_MARKER) { user, src, (numStr) ->
    WindowContext(this, src, user)
            .showParentPaymentWindow(user, numStr.toInt())
}

private object ReadParentPaymentAmount: Question(READ_PARENT_PAYMENT_AMOUNT) {
    override fun SenderContext.ask(user: User) =
            WindowContext(this, null, user)
                    .showParentPaymentWindow(user, 0)

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val amount = update.message?.text?.toFloatOrNull()
        if (amount == null) {
            user.send(Text["wrong-payment-amount"])
            return false
        }
        
        val unfinishedPayment = user.payments.find { it.amount == null }
                ?: error("There's no unfinished payment")

        unfinishedPayment.amount = amount

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
        return false
    }
}

private val PARENT_PAYMENT_TRIGGER = CommandTrigger(Text["pay-command"]) and RoleTrigger(Parent) and StateTrigger(COMMAND)
val PARENT_PAYMENT = QuestionSet(ReadParentPaymentAmount, trigger = PARENT_PAYMENT_TRIGGER)
