package org.order.logic.commands.handlers

import org.order.bot.send.Sender
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.payments.SuccessfulPayment

class SuccessfulPaymentHandler(val body: Sender.(User, SuccessfulPayment) -> Unit): Command {
    override fun matches(user: User, update: Update) = update.message?.successfulPayment != null
    override fun process(sender: Sender, user: User, update: Update) = sender.body(user, update.message!!.successfulPayment!!)
}