package org.order.logic.impl

import org.order.bot.CommandsBot
import org.order.bot.send.SenderContext
import org.order.logic.impl.commands.TIME_TO_SEND_POLL
import org.order.logic.impl.commands.display.*
import org.order.logic.impl.commands.orders.CANCEL_ORDER
import org.order.logic.impl.commands.orders.MAKE_ORDER
import org.order.logic.impl.commands.orders.ORDER_CANCELLATION_WINDOW
import org.order.logic.impl.commands.orders.ORDER_WINDOW
import org.order.logic.impl.commands.payments.*
import org.order.logic.impl.commands.polls.RATE_PROCESSOR
import org.order.logic.impl.commands.polls.launchPollSender
import org.order.logic.impl.commands.registration.*
import org.order.logic.impl.commands.tools.BAN_FILTER
import org.order.logic.impl.commands.tools.MESSAGE_REMOVER
import org.order.logic.impl.commands.tools.VALIDATION_FILTER

class FoodOrderBot(senderContext: SenderContext, username: String, token: String) : CommandsBot(senderContext, username, token) {
    init {
        //senderContext.launchPollSender()

        // ---- Validation Filter ----
        this += VALIDATION_FILTER
        // ---------------------------

        // ------- Ban Filter -------
        this += BAN_FILTER
        // --------------------------

        // ------ Registration ------
        this += USER_REGISTRATION
        this += ROLE_REGISTRATION

        this += PARENT_REGISTRATION
        this += STUDENT_REGISTRATION
        // --------------------------

        // ------- Validation -------
        this += REGISTRATION_PROCESSOR
        this += VALIDATION_PROCESSOR
        this += CHECK_REGISTRATION
        // --------------------------

        // ----- Message Remover -----
        this += MESSAGE_REMOVER
        // ---------------------------

        // --------- Orders ---------
        this += ORDER_WINDOW
        this += MAKE_ORDER

        this += ORDER_CANCELLATION_WINDOW
        this += CANCEL_ORDER
        // --------------------------

        // ----- Client Payment -----
        // this += CLIENT_PAYMENT
        // --------------------------

        // ----- Parent Payment -----
        // this += PARENT_PAYMENT
        // this += UPDATE_PARENT_PAYMENT_WINDOW
        // this += CANCEL_PARENT_PAYMENT
        // --------------------------

        // - Another Payments Stuff -
        // this += PAYMENT_CONFIRMATION
        // this += PROCESS_SUCCESSFUL_PAYMENT
        // --------------------------

        // --------- Display ---------
        this += HELP
        this += ORDERS_LIST_WINDOW
        this += PAYMENTS_LIST_WINDOW
        this += MY_ORDERS_LIST_WINDOW
        this += HISTORY_WINDOW
        // ---------------------------

        // ---------- Polls ----------
        // this += RATE_PROCESSOR
        // ---------------------------
    }
}