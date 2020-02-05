package org.order.logic.commands.processors

import org.order.bot.send.SenderContext
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Update

class SuccessfulPaymentProcessor(private val marker: String,
                                 private val process: SenderContext.(User, String, String, List<String>) -> Unit): Command {

    override fun SenderContext.process(user: User, update: Update): Boolean {
        val successfulPayment = update.message?.successfulPayment
        val payload = successfulPayment?.invoicePayload

        if (payload != null && payload.startsWith("$marker:")) {
            val telegramId = successfulPayment.telegramPaymentChargeId
            val providerId = successfulPayment.providerPaymentChargeId

            val args = payload
                    .substringAfter(':')
                    .split(':')

            // TODO let bot make sure it's right payment
            process(user, telegramId, providerId, args)
            return true
        }

        return false
    }
}