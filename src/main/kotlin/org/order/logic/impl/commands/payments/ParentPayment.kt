package org.order.logic.impl.commands.payments

import org.order.bot.send.SenderContext
import org.order.bot.send.button
import org.order.data.entities.Client
import org.order.data.entities.Parent
import org.order.data.entities.Payment
import org.order.data.entities.State.COMMAND
import org.order.data.entities.State.READ_PARENT_PAYMENT_AMOUNT
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
import org.telegram.telegrambots.meta.api.objects.Update

private const val windowMarker = "read-parent-payment-amount-window"

private fun WindowContext.showParentPaymentWindow(user: User, childNum: Int) {
    val children = user.linked(Parent).children.toList()
    val child = children[childNum]

    val client = child.user.linked(Client)

    val name = client.user.name!!
    val balance = client.balance

    val unfinishedPayment = client.payments.find {
        it.amount == null // Find unfinished payment
    }?.apply {
        this.client = client // Update old client
    } ?: Payment.new {
        this.madeBy = user
        this.client = client // Or create new payment
    }

    show(Text.get("ask-payment-amount") { it["balance"] = "$balance" }) {
        when {
            children.size == 1 -> button(name)
            childNum == children.size - 1 -> button(name, "$windowMarker:0")
            childNum <  children.size - 1 -> button(name, "$windowMarker:${childNum + 1}")
        }
        button(Text["cancel"], "cancel-parent-payment:${unfinishedPayment.id.value}")
    }
}

val CANCEL_PARENT_PAYMENT = CallbackProcessor("cancel-parent-payment") { user, src, (paymentIdStr) ->
    val paymentId = paymentIdStr.toInt()
    val payment = Payment.findById(paymentId) ?: error("There's no payment with id: $paymentId")

    payment.delete()
    user.state = COMMAND

    src.delete()
}

val UPDATE_PARENT_PAYMENT_WINDOW = CallbackProcessor(windowMarker) { user, src, (numStr) ->
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

        user.sendInvoice(
                Text["payment-title"], amount,
                Text.get("payment-description") {
                    it["amount"] = amount.toString()
                },
                "account-replenishment:${unfinishedPayment.id.value}"
        )
        return true
    }
}

private val PARENT_PAYMENT_TRIGGER = CommandTrigger(Text["pay-command"]) and RoleTrigger(Parent) and StateTrigger(COMMAND)
val PARENT_PAYMENT = QuestionSet(ReadParentPaymentAmount, trigger = PARENT_PAYMENT_TRIGGER)