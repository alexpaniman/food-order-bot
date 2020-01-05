package org.order.logic.commands.processors

import org.order.bot.send.SenderContext
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Update

class PreCheckoutQueryProcessor(private val marker: String,
                                private val process: SenderContext.(User, String, String, Float, List<String>) -> Unit): Command {
    override fun SenderContext.process(user: User, update: Update): Boolean {
        val preCheckoutQuery = update.preCheckoutQuery
        val payload = preCheckoutQuery?.invoicePayload

        if (payload != null && payload.startsWith("$marker:")) {
            val args = payload
                    .substringAfter(':')
                    .split(':')

            val currency = preCheckoutQuery.currency
            val amount = preCheckoutQuery.totalAmount / 100f

            process(user, preCheckoutQuery.id, currency, amount, args)
            return true
        }

        return false
    }

}