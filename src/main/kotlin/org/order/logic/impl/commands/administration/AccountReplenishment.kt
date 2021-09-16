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
import org.order.logic.impl.commands.modules.longestCommonSubstring
import org.order.logic.impl.utils.appendMainKeyboard
import org.order.logic.impl.utils.grade
import org.telegram.telegrambots.meta.api.objects.Update

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
                appendMainKeyboard(user)
            }
            user.state = COMMAND
            return false
        }

        val (_, clients) = Client.all()
                .map { it.user }
                .filter { it.valid }
                .groupBy { longestCommonSubstring(it.name!!, response) }
                .maxBy { (similarity, _) -> similarity } ?: return false

        data class NamedUser(val name: String, val user: User)

        val namedUsers = clients.map {
            NamedUser(it.name + ", " + it.grade, it)
        } .groupBy { it.name }.map { (_, users) ->
            if (users.size > 1)
                users.sortedBy { it.user.id }.mapIndexed { index, (name, user) ->
                    NamedUser("$name #$index", user)
                }
            else users
        }.flatten()

        user.send(Text["choose-client-from-list"]) {
            reply {
                show(namedUsers, 1) { (name, _) -> name }
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
            user.send(Text["successful-account-replenishment-cancellation"]) {
                appendMainKeyboard(user)
            }
            user.state = COMMAND
            return false
        }

        val args = update.message?.text?.split(", ")
        if (args == null || args.size != 2) {
            user.send(Text["wrong-user-identifier-to-replenish-account"])
            return false
        }

        val name = args[0]
        val gradeWithUniqueId = args[1].split(" #")

        val uniqueId: Int?
        val grade: String = gradeWithUniqueId[0]
        if (gradeWithUniqueId.size == 2) {
            uniqueId = gradeWithUniqueId[1].toIntOrNull()

            if (uniqueId == null) {
                user.send(Text["wrong-user-identifier-to-replenish-account"])
                return false
            }
        } else uniqueId = 0

        val client = Client.all().filter { it.user.valid }.filter {
            val isNameSame = it.user.name == name
            val clientGrade = it.user.grade
            val isGradeSame = grade == clientGrade
            isNameSame && isGradeSame
        }.sortedBy { it.user.id }[uniqueId]

        Payment.new {
            this.madeBy = user
            this.client = client
        }
        return true
    }

}

object ChoosePaymentAmountToReplenishAccount: Question(READ_PAYMENT_AMOUNT_TO_REPLENISH_ACCOUNT) {
    override fun SenderContext.ask(user: User) =
            user.send(Text["choose-payment-amount-to-replenish-account"]) {
                reply {
                    button(Text["cancel-button"])
                }
            }

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val text = update.message?.text

        val payment = Payment.all()
                .firstOrNull { it.madeBy == user && it.amount == null }!!

        if (text == Text["cancel-button"]) {
            user.send(Text["successful-account-replenishment-cancellation"]) {
                appendMainKeyboard(user)
            }

            payment.delete()
            user.state = COMMAND
            return false
        }

        val amount = text?.toFloatOrNull()
        if (amount == null) {
            user.send(Text["wrong-payment-amount-to-replenish-account"])
            return false
        }

        payment.amount = amount

        val client = payment.client
        val clientGrade = client.user.grade

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

val PERFORM_ACCOUNT_REPLENISHMENT = CallbackProcessor("perform-account-replenishment") { user, src, (action, paymentIdStr) ->
    val payment = Payment.findById(paymentIdStr.toInt())!!
    when(action) {
        "confirm" -> {
            payment.registered = DateTime.now()
            payment.client.balance += payment.amount!!
            src.edit(Text.get("successful-account-replenishment") {
                it["user-name"] = payment.madeBy.name!!
                it["client-name"] = payment.client.user.name!!
                it["amount"] = payment.amount.toString()
                it["registered"] = payment.registered!!.toString("yyyy-MM-dd HH:mm:ss")
            })

            user.send(Text["resend-buttons-message"]) {
                appendMainKeyboard(user)
            }
        }
        "dismiss" -> {
            payment.delete()
            src.delete()

            user.send(Text["successfully-dismissed-account-replenishment"]) {
                appendMainKeyboard(user)
            }
        }
    }
}

private val ACCOUNT_REPLENISHMENT_TRIGGER =
        StateTrigger(COMMAND) and
                CommandTrigger(Text["replenish-account-command"]) and
                (RoleTrigger(Admin) or RoleTrigger(Producer))

val ACCOUNT_REPLENISHMENT = QuestionSet(
        ReadClientToReplenishAccount,
        ChooseClientToReplenishAccount,
        ChoosePaymentAmountToReplenishAccount,
        trigger = ACCOUNT_REPLENISHMENT_TRIGGER
)