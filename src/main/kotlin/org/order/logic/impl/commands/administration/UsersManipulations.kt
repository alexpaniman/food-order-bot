package org.order.logic.impl.commands.administration

import org.joda.time.DateTime
import org.order.bot.send.*
import org.order.data.entities.*
import org.order.data.entities.State.*
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.commands.triggers.*
import org.order.logic.corpus.Text
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.math.max

private fun longestCommonSubstring(s1: String, s2: String): Int {
    val dynamic = Array(s2.length) {
        IntArray(s1.length) { 0 }
    }

    for (i in 1..s1.length)
        for (j in 1..s2.length) {
            dynamic[i][j] = max(dynamic[i - 1][j], dynamic[i][j - 1])
            if (s1[j - 1] == s2[i - 1])
                dynamic[i][j] += 1
        }

    return dynamic.last().last()
}

private object ReadClientToReplenishAccount: Question(READ_CLIENT_TO_REPLENISH_ACCOUNT) {
    override fun SenderContext.ask(user: User) =
            user.send(Text["choose-client-to-replenish-account"]) {
                reply {
                    button(Text["cancel-button"])
                }
            }

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val response = update.message?.text
        if (response == null) {
            user.send(Text["wrong-client-to-replenish-account"])
            return false
        }

        if (response == Text["cancel-button"]) {
            user.send(Text["successful-account-replenishment-cancellation"]) {
                removeReply()
            }
            user.state = COMMAND
            return false
        }

        val (_, clients) = Client.all()
                .map { it.user }
                .groupBy { longestCommonSubstring(it.name!!, response) }
                .maxBy { (similarity, _) -> similarity } ?: return false

        user.send(Text["choose-client-from-list"]) {
            reply {
                show(clients, 1) {
                    "${it.name}, ${it.linkedOrNull(Student)?.grade?.name ?: Text["empty-grade"]}"
                }
                button(Text["cancel-button"])
            }
        }
        return true
    }
}

object ChooseClientToReplenishAccount: Question(CHOOSE_CLIENT_TO_REPLENISH_ACCOUNT) {
    override fun SenderContext.ask(user: User) {}

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        if (update.message?.text == Text["cancel-button"]) {
            user.send(Text["successful-account-replenishment-cancellation"])
        }

        val args = update.message?.text?.split(", ")
        if (args == null || args.size != 2) {
            user.send(Text["wrong-user-identifier-to-replenish-account"])
            return false
        }

        val (name, grade) = args
        val client = Client.all().firstOrNull {
            val isNameSame = it.user.name == name
            val clientGrade = it.user.linkedOrNull(Student)?.grade?.name ?: Text["empty-grade"]
            val isGradeSame = grade == clientGrade
            isNameSame && isGradeSame
        }!!

        Payment.new {
            this.madeBy = user
            this.client = client
        }
        return true
    }

}

object ChoosePaymentAmountToReplenishAccount: Question(READ_PAYMENT_AMOUNT_TO_REPLENISH_ACCOUNT) {
    override fun SenderContext.ask(user: User) =
            user.send(Text["choose-payment-amount-to-replenish-account"])

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val amount = update.message?.text?.toFloatOrNull()
        if (amount == null) {
            user.send(Text["wrong-payment-amount-to-replenish-account"])
            return false
        }

        val payment = Payment.all()
                .firstOrNull { it.madeBy == user && it.amount == null }!!

        payment.amount = amount

        val client = payment.client
        val clientGrade = client.user.linkedOrNull(Student)?.grade?.name ?: Text["empty-grade"]

        user.send(Text.get("confirm-account-replenishment") {
            it["name"] = client.user.name!!
            it["grade"] = clientGrade
            it["amount"] = payment.amount.toString()
        }) {
            inline {
                button(Text["i-confirm-account-replenishment"], "perform-account-replenishment:confirm:${payment.id.value}")
                button(Text["cancel-button"], "perform-account-replenishment:dismiss:${payment.id.value}")
            }
        }
        user.state = COMMAND
        return true
    }
}

val PERFORM_ACCOUNT_REPLENISHMENT = CallbackProcessor("perform-account-replenishment") { _, src, (action, paymentIdStr) ->
    val payment = Payment.findById(paymentIdStr.toInt())!!
    when(action) {
        "confirm" -> {
            payment.registered = DateTime.now()
            payment.client.balance += payment.amount!!
            src.edit(Text["successful-account-replenishment"])
        }
        "dismiss" -> {
            payment.delete()
            src.delete()
        }
    }
}

private val ACCOUNT_REPLENISHMENT_TRIGGER =
        StateTrigger(COMMAND) and
                CommandTrigger(Text["replenish-account"]) and
                (RoleTrigger(Parent) or RoleTrigger(Admin))

val ACCOUNT_REPLENISHMENT = QuestionSet(
        ReadClientToReplenishAccount,
        ChooseClientToReplenishAccount,
        ChoosePaymentAmountToReplenishAccount,
        trigger = ACCOUNT_REPLENISHMENT_TRIGGER
)