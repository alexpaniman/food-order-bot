package org.order.logic.impl

import org.order.bot.CommandsBot
import org.order.bot.send.SenderContext
import org.order.logic.impl.commands.REGION_ID
import org.order.logic.impl.commands.display.*
import org.order.logic.impl.commands.orders.CANCEL_ORDER
import org.order.logic.impl.commands.orders.MAKE_ORDER
import org.order.logic.impl.commands.orders.ORDER_CANCELLATION_WINDOW
import org.order.logic.impl.commands.orders.ORDER_WINDOW
import org.order.logic.impl.commands.payments.*
import org.order.logic.impl.commands.registration.*
import org.order.logic.impl.commands.tools.BAN_FILTER
import org.order.logic.impl.commands.tools.MESSAGE_REMOVER
import org.order.logic.impl.commands.tools.VALIDATION_FILTER
import org.joda.time.DateTimeZone
import org.order.logic.impl.commands.administration.*
import org.order.logic.impl.commands.notifications.launchClientsNotifier
import org.order.logic.impl.commands.polls.*

class FoodOrderBot(senderContext: SenderContext, username: String, token: String) : CommandsBot(senderContext, username, token) {
    init {
        DateTimeZone.setDefault(DateTimeZone.forID(REGION_ID))

        senderContext.launchPollSender()
        senderContext.launchClientsNotifier()

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

        // ---- Keyboard Resender ----
        this += RESEND_BUTTONS
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

        // - Payments Related Stuff -
        this += REMOVE_CANCELED_PAYMENT
        this += PAYMENT_CONFIRMATION
        this += PROCESS_SUCCESSFUL_PAYMENT
        this += MODALITY_OF_PAYMENT_WINDOW_FILTER
        // --------------------------

        // ----- Client Payment -----
        this += CLIENT_PAYMENT
        // --------------------------

        // ----- Parent Payment -----
        this += PARENT_PAYMENT
        this += UPDATE_PARENT_PAYMENT_WINDOW
        this += REMOVE_CANCELED_PAYMENT
        // --------------------------

        // --- Manual Account Replenishment ---
        this += ACCOUNT_REPLENISHMENT
        this += PERFORM_ACCOUNT_REPLENISHMENT
        // ------------------------------------

        // --------- Display ---------
        this += HELP
        this += ORDERS_LIST_WINDOW
        this += PAYMENTS_LIST_WINDOW
        this += MY_ORDERS_LIST_WINDOW
        this += HISTORY_WINDOW
        this += MONEY_TOTAL_WINDOW
        this += POLLS_PDF_TOTAL
        // ---------------------------

        // ---------- Polls ----------
        this += RATE_PROCESSOR
        this += SUGGEST_WRITING_A_COMMENT
        this += CANCEL_WRITING_A_COMMENT
        this += SEND_A_COMMENT
        // ---------------------------

        // --------- Mailing ---------
        this += MAILING_MENU
        this += SUGGEST_MAILING_A_MESSAGE_TO_MAIL
        this += PERFORM_MAILING
        // ---------------------------
    }
}