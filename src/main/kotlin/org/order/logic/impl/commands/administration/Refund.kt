package org.order.logic.impl.commands.administration

import org.joda.time.DateTime
import org.order.bot.send.*
import org.order.data.entities.*
import org.order.data.tables.RefundComments
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.commands.triggers.*
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.modules.searchUsers
import org.order.logic.impl.utils.appendMainKeyboard
import org.order.logic.impl.utils.getTempProperty
import org.order.logic.impl.utils.removeTempProperty
import org.order.logic.impl.utils.setTempProperty
import org.telegram.telegrambots.meta.api.objects.Update

val REFUND_TRIGGER = CommandTrigger(Text["refund-command"]) and
        StateTrigger(State.COMMAND) and (RoleTrigger(Admin) or RoleTrigger(Producer))

private const val REFUND_READ_AMOUNT_CALLBACK_NAME = "refund"

val REFUND = TriggerCommand(REFUND_TRIGGER) { user, _ ->
    searchUsers(user, "$REFUND_READ_AMOUNT_CALLBACK_NAME:{}")
}

val REFUND_READ_AMOUNT = CallbackProcessor(REFUND_READ_AMOUNT_CALLBACK_NAME) { user, _, (listOfClientIdsStr) ->
    val clientId = listOfClientIdsStr
        .split(",")
        .first().toInt()

    val client = Client.findById(clientId) ?: error("There's no client with id: $clientId")

    val payment = Payment.new {
        this.madeBy = user
        this.client = client

        // Amount placeholder for refunds, so I can detect them
        // from database if I need.
        this.amount = -1f
    }

    user.setTempProperty("refund:payment-id", "${payment.id}")

    // Activate REFUND_QUESTION_SET module
    user.state = State.READ_REFUND_AMOUNT

    user.send(Text.get("refund:read-refund-amount") {
        it["balance"] = "${client.balance}"
    }) {
        reply {
            button(Text["cancel-button"])
        }
    }
}

val REFUND_QUESTION_SET = QuestionSet(ReadRefundAmount, ReadRefundComment, trigger = NegativeTrigger())

private fun getRefundPayment(user: User): Payment {
    val paymentId = user.getTempProperty("refund:payment-id")
        .toIntOrNull() ?: error("Illegal refund payment id!")

    return Payment.findById(paymentId)
        ?: error("Cannot locate payment with id: $paymentId")
}

private fun SenderContext.abortRefund(user: User, payment: Payment = getRefundPayment(user)) {
    // Cleanup refund comments
    // Only really useful if confirmation was dismissed.
    RefundComment
        .find { RefundComments.payment eq payment.id }
        .forEach { it.delete() }

    // TODO Make safer
    payment.delete()

    user.state = State.COMMAND
    user.removeTempProperty("refund:payment-id")

    user.send(Text["refund:abort"]) {
        appendMainKeyboard(user)
    }
}

object ReadRefundAmount : Question(State.READ_REFUND_AMOUNT) {
    override fun SenderContext.ask(user: User) = error("Never activates by itself.")

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        if (update.message?.text == Text["cancel-button"]) {
            abortRefund(user)
            return false
        }

        val amount = update.message?.text?.toFloatOrNull()
        if (amount != null && amount > 0) {
            val paymentId = user.getTempProperty("refund:payment-id")
                .toIntOrNull() ?: error("Illegal refund payment id!")

            val payment = Payment.findById(paymentId)
                ?: error("Cannot locate payment with id: $paymentId")

            payment.amount = -amount // Write amount there

            // Temp property is still there, I will remove it in the next question.
        } else user.send(Text["refund:wrong-refund-amount"])

        return amount != null
    }
}

private const val REFUND_CONFIRMATION_CALLBACK_NAME = "refund-confirmation"

object ReadRefundComment : Question(State.READ_REFUND_COMMENT) {
    override fun SenderContext.ask(user: User) =
        user.send(Text["refund:read-refund-comment"]) {
            reply {
                button(Text["cancel-button"])
            }
        }

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val comment = update.message?.text
        if (comment == Text["cancel-button"]) {
            // All cleaning up is done internally
            abortRefund(user)
            return false
        }

        if (comment != null) {
            val payment = getRefundPayment(user)

            val refundComment = RefundComment.new {
                this.comment = comment
                this.payment = payment
            }

            // It's negative
            var refundAmount = payment.amount
                ?: error("Undefined refund amount!")

            // Now it's positive
            refundAmount *= -1f

            user.send(Text.get("refund:confirmation") {
                it["client"] = payment.client!!.user.name!!
                it["balance"] = "${payment.client!!.balance}"
                it["amount"] = "$refundAmount"
                it["comment"] = comment
            }) {
                inline {
                    row {
                        button(Text["refund:confirm"],
                            "$REFUND_CONFIRMATION_CALLBACK_NAME:confirm:${refundComment.id}")

                        button(Text["refund:dismiss"],
                            "$REFUND_CONFIRMATION_CALLBACK_NAME:dismiss:${refundComment.id}")
                    }
                }
            }

            // Clean up
            user.removeTempProperty("refund:payment-id")

            user.state = State.COMMAND
        } else user.send(Text["refund:wrong-refund-comment"])

        return false
    }
}

val REFUND_CONFIRMATION = CallbackProcessor(REFUND_CONFIRMATION_CALLBACK_NAME) processor@ {
        user, src, (status, commentIdStr) ->

    val commentId = commentIdStr.toIntOrNull()
        ?: error("Illegal comment id: $commentIdStr")

    val refundComment = RefundComment.findById(commentId)
        ?: error("No such comment (id: ${commentId})!")

    val payment = refundComment.payment

    val confirm = status == "confirm"

    if (!confirm) {
        abortRefund(user, payment)
        return@processor
    }

    // It's negative
    var refundAmount = payment.amount
        ?: error("Undefined refund amount!")

    // Now it's positive
    refundAmount *= -1f

    val client = payment.client

    // Update client's balance
    client!!.balance -= refundAmount

    // Register refund time
    payment.registered = DateTime.now()

    try {
        payment.client!!.user.send(Text.get("refund:notify") {
            it["name"] = user.name!!
            it["amount"] = "$refundAmount"
            it["comment"] = refundComment.comment
        })

        src.edit(Text.get("refund:successful-client-notification") {
            it["client"] = client!!.user.name!!
        })
    } catch(exc: Exception) {
        exc.printStackTrace()
        src.edit(Text["refund:cannot-notify"])
    }

    (Admin.all().map { it.user } + Producer.all().map { it.user })
        .forEach { admin ->
            admin.send(Text.get("refund:success-notification") {
                it["client-name"] = client!!.user.name!!
                it["made-by-name"] = user.name!!
                it["amount"] = "$refundAmount"
                it["new-balance"] = "${client.balance}"
                it["registered"] = payment.registered!!.toString("yyyy-MM-dd HH:mm:ss")
                it["comment"] = refundComment.comment
            }) {
                if (user == admin)
                    appendMainKeyboard(user)
            }
        }
}