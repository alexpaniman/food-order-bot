package org.order.logic.impl

import org.order.bot.CommandsBot
import org.order.bot.send.SenderContext
import org.order.data.entities.State.VALIDATION
import org.order.data.entities.User
import org.order.logic.commands.Command
import org.order.logic.impl.commands.orders.CANCEL_ORDER
import org.order.logic.impl.commands.orders.MAKE_ORDER
import org.order.logic.impl.commands.orders.ORDER_CANCELLATION_WINDOW
import org.order.logic.impl.commands.orders.ORDER_WINDOW
import org.order.logic.impl.commands.registration.*
import org.order.logic.impl.commands.tools.MESSAGE_REMOVER
import org.telegram.telegrambots.meta.api.objects.Update

class FoodOrderBot(senderContext: SenderContext, username: String, token: String) : CommandsBot(senderContext, username, token) {
    init {
        // ------ Registration ------
        this += USER_REGISTRATION
        this += ROLE_REGISTRATION

        this += PARENT_REGISTRATION
        this += STUDENT_REGISTRATION
        // --------------------------

        // ------- Validation -------
        this += CHECK_REGISTRATION
        this += REGISTRATION_PROCESSOR
        this += VALIDATION_PROCESSOR
        // --------------------------

        // ---- Validation Filter ----
        this += object : Command {
            override fun SenderContext.process(user: User, update: Update) =
                    user.state != VALIDATION
        }
        // ---------------------------

        // ----- Message Remover -----
        this += MESSAGE_REMOVER
        // ---------------------------

        // --------- Orders ---------
        this += ORDER_WINDOW
        this += MAKE_ORDER

        this += ORDER_CANCELLATION_WINDOW
        this += CANCEL_ORDER
        // --------------------------
    }
}